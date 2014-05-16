/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opennms.newts.gsod;


import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.AnnotatedElement;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.MethodSetter;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.gsod.FileIterable.KeyedIterable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.io.Files;


public class MergeSort {

    private static final Logger LOG = LoggerFactory.getLogger(MergeSort.class);
    
    private static final String HDR = "STN--- WBAN   YEARMODA    TEMP       DEWP      SLP        STP       VISIB      WDSP     MXSPD   GUST    MAX     MIN   PRCP   SNDP   FRSHTT";

    public static void main(String... args) throws IOException {
        new MergeSort().execute(args);
    }

    private File m_source;
    private File m_targetDir;
    private int m_mergeCount = 1000;
    
    private void checkArgument(boolean check, String failureMessage) {
        if (!check) throw new IllegalArgumentException(failureMessage);
    }

    @Option(name="-c", aliases="--merge-count", usage="the number of files to include in a single merge")
    public void setMergeCount(int mergeCount) {
        checkArgument(mergeCount > 1, "the merge count must be a number greater than 1.");
        m_mergeCount  = mergeCount;
    }
    

    @Argument(index=0, metaVar="sourceDir", required=true, usage="the source directory that contains gsod")
    public void setSource(File source) {
        checkArgument(source.exists(), "the source directory does not exist");
        checkArgument(source.isDirectory(), "the source directory must be a directory");
        m_source = source;
    }

    @Argument(index=1, metaVar="targetDir", required=true, usage="the target directory for the sourted output")
    public void setTarget(File target) {
        m_targetDir = target;
        target.mkdirs();
    }
    
    
    public void execute(String... args) throws IOException {
        

        CmdLineParser parser = createCmdLineParser();
        try {
                parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }
        
        final MetricRegistry metrics = new MetricRegistry();
        ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .outputTo(System.err)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .build();

        reporter.start(10, SECONDS);


        Meter linesMeter = metrics.meter("lines");
        Meter filesMeter = metrics.meter("files");
        Meter dirsMeter = metrics.meter("dirs");
        Meter batchMeter = metrics.meter("batches");
        Path root = m_source.toPath();
        
        if (m_targetDir == null) {
            m_targetDir = Files.createTempDir();
            System.err.println("Working Directory: " + m_targetDir);
        }
        
        LOG.debug("Scanning {} for GSOD data files...", root);

        FluentIterable<KeyedIterable<Path, Path>> dirs = FileIterable.groupFilesByDir(root);

        for(KeyedIterable<Path, Path> filesInDir : dirs) {
            Path subdir = root.relativize(filesInDir.getKey());
            String dirName = subdir.getFileName().toString();
            
            System.err.println("Sorted dir: " + subdir);
            FluentIterable<Iterable<String>> contentIterables = filesInDir
                    .transform(this.<Path>meter(filesMeter))
                    .transform(lines("YEARMODA"))
                    ;
            FluentIterable<List<Iterable<String>>> batches = FluentIterable.from(Iterables.partition(contentIterables, m_mergeCount));
            FluentIterable<Iterable<GSODLine>> sortedBatches = batches.transform(lift2GsodLines()).transform(mergeSorter());

            Path sortedDir = m_targetDir.toPath().resolve(subdir);
            sortedDir.toFile().mkdirs();
            
            int count = 1;
            for(Iterable<GSODLine> batch : sortedBatches) {
                Path sortedFile = sortedDir.resolve(dirName+"-batch-"+(count++)+".gz");
                System.err.println("Creating " + sortedFile);
                try (PrintStream out = open(sortedFile)) {
                    out.println(HDR);
                    for(GSODLine line : batch) {
                        out.println(line);
                        linesMeter.mark();
                    }
                }
                batchMeter.mark();
            }
            
            dirsMeter.mark();
            
        }
        
        
    }

    private PrintStream open(Path sortedFile) throws IOException,
            FileNotFoundException {
        return new PrintStream(
                 new BufferedOutputStream(
                          new GZIPOutputStream(
                                 new FileOutputStream(sortedFile.toFile())
                          ),
                 64*1024) // use a 64K buffer for speed comparison
        );
    }

    private CmdLineParser createCmdLineParser() {
        CmdLineParser parser = new CmdLineParser(this) {

            @SuppressWarnings("rawtypes")
            @Override
            public void addArgument(final Setter setter, Argument a) {
                Setter newSetter = setter; 
                if (setter instanceof MethodSetter) {
                    newSetter = new Setter() {

                        @SuppressWarnings("unchecked")
                        @Override
                        public void addValue(Object value) throws CmdLineException {
                            setter.addValue(value);
                        }

                        @Override
                        public Class getType() {
                            return setter.getType();
                        }

                        @Override
                        public boolean isMultiValued() {
                            return false;
                        }

                        @Override
                        public FieldSetter asFieldSetter() {
                            return setter.asFieldSetter();
                        }

                        @Override
                        public AnnotatedElement asAnnotatedElement() {
                            return setter.asAnnotatedElement();
                        }
                        
                    };
                }
                super.addArgument(newSetter, a);
            }
        };
        return parser;
    }
        
        

    private Function<? super Iterable<Iterable<GSODLine>>, Iterable<GSODLine>> mergeSorter() {
        return new Function<Iterable<Iterable<GSODLine>>, Iterable<GSODLine>>() {

            @Override
            public Iterable<GSODLine> apply(Iterable<Iterable<GSODLine>> input) {
                return Iterables.mergeSorted(input, new Comparator<GSODLine>() {

                    @Override
                    public int compare(GSODLine o1, GSODLine o2) {
                        return o1.compareTo(o2);
                    }
                });
            }
            
        };
    }

    private Function<? super Path, Iterable<String>> lines(final String excludePattern) {
        return new Function<Path, Iterable<String>>() {
        
            @Override
            public Iterable<String> apply(Path input) {
                return FileIterable.unzipLines(input, Charsets.US_ASCII).filter(excludes(excludePattern));
            }
        
        };
    }
    
    private Predicate<? super String> excludes(final String excludePattern) {
        return new Predicate<String>() {

            @Override
            public boolean apply(String input) {
                return !input.contains(excludePattern);
            }
            
        };
    }
    
    private <T> Function<T, T> meter(final Meter meter) {
        return new Function<T, T>() {

            @Override
            public T apply(T input) {
                meter.mark();
                return input;
            }
            
        };
    }
    
    private Function<String, GSODLine> gsodLines() {
        return new Function<String, GSODLine>(){

            @Override
            public GSODLine apply(String input) {
                return new GSODLine(input);
            }
            
        };
    }
    
    private Function<Iterable<String>, Iterable<GSODLine>> liftGsodLines() {
        return new Function<Iterable<String>, Iterable<GSODLine>>() {

            @Override
            public Iterable<GSODLine> apply(Iterable<String> input) {
                return Iterables.transform(input, gsodLines());
            }
            
        };
    }
    
    private Function<List<Iterable<String>>, Iterable<Iterable<GSODLine>>> lift2GsodLines() {
        return new Function<List<Iterable<String>>, Iterable<Iterable<GSODLine>>>() {

            @Override
            public Iterable<Iterable<GSODLine>> apply(List<Iterable<String>> input) {
                return Iterables.transform(input, liftGsodLines());
            }
            
        };
    }
    
    public static class GSODLine implements Comparable<GSODLine>{
        private long m_date;
        private long m_stn;
        private long m_wban;
        private String m_line;
        
        public GSODLine(String line) {
            m_line = line;
            m_stn = longAt(0);
            m_wban = longAt(7);
            m_date = longAt(14);
        }
        
        public long longAt(int index) {
            long n = 0;
            char ch;
            while((ch = m_line.charAt(index)) != ' ') {
                n = n*10 + (ch - '0');
                index++;
            }
            return n;
        }
        
        @Override
        public int compareTo(GSODLine o) {
            if (m_date < o.m_date) return -1;
            if (m_date > o.m_date) return 1;
            if (m_stn < o.m_stn) return -1;
            if (m_stn > o.m_stn) return 1;
            if (m_wban < o.m_wban) return -1;
            if (m_wban > o.m_wban) return 1;
            return 0;
        }
        
        public String getLine() {
            return m_line;
        }
        
        public String toString() {
            return m_line;
        }

        public long getStation() {
            return m_stn;
        }
        
        public long getWBAN() {
            return m_wban;
        }
        
        public long getDate() {
            return m_date;
        }
    }

}
