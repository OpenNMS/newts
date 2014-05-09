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
import static org.opennms.newts.gsod.FileObservable.fileTreeWalker;
import static org.opennms.newts.gsod.FileObservable.lines;
import static rx.exceptions.Exceptions.propagate;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rx.Observable;
import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.apache.http.ObservableHttp;
import rx.apache.http.ObservableHttpResponse;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.schedulers.Schedulers;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class ImportRunner2 {
    
    private int m_samplesPerBatch = 1000;
    private File m_source;
    private String m_restUrl = null;
    private SampleRepository m_repository;
    
    private void checkArgument(boolean check, String failureMessage) {
        if (!check) throw new IllegalArgumentException(failureMessage);
    }


    @Option(name="-n", aliases="--samples-per-batch", metaVar="sample-count", usage="the maxinum number of samples to include in each post to the repository (default: 1000)")
    public void setSamplesPerBatch(int samplesPerBatch) {
        checkArgument(samplesPerBatch > 0, "samples per batch must be greater than zero!");
        m_samplesPerBatch = samplesPerBatch;
    }
    
    @Option(name="-u", aliases="--url", metaVar="url", usage="publish data via a Newts REST server at the given url (default: use direct access via Newts API)")
    public void setURL(String url) {
        checkArgument(url != null && !url.isEmpty(), "the url must not be empty");
        m_restUrl = url;
    }
    
    @Argument(metaVar="sourceDir", required=true, usage="the source directory that contains gsod data to import. These must be gzip'd files")
    public void setSource(File source) {
        checkArgument(source.exists(), "the source directory "+source+" does not exist");
        checkArgument(source.isDirectory(), "the source directory must be a directory");
        m_source = source;
    }

    private static final Logger LOG = LoggerFactory.getLogger(ImportRunner2.class);

    public static void main(String... args) throws Exception {
        new ImportRunner2().execute(args);

    }
    
    public void execute(String... args) throws Exception {

        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        // Setup the slf4j metrics reporter
        MetricRegistry metrics = new MetricRegistry();
        
        final long start = System.currentTimeMillis();
        metrics.register("elapsed-seconds", new Gauge<Double>() {

            @Override
            public Double getValue() {
                return (System.currentTimeMillis() - start)/1000.0;
            }
            
        });
        
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .outputTo(System.err)
                .convertRatesTo(SECONDS)
                .convertDurationsTo(MILLISECONDS)
                .build();

        reporter.start(10, SECONDS);



        LOG.debug("Scanning {} for GSOD data files...", m_source);
        
        // walk the files in the directory given
        Observable<Sample> samples = fileTreeWalker(m_source.toPath())
             .subscribeOn(Schedulers.io())
                
             // set up a meter for each file processed
            .map(meter(metrics.meter("files"), Path.class))
            
            // report file
            .map(reportFile())

            // read all the files and convert them into lines
            .mergeMap(lines())
            // excluding the header lines
            .filter(exclude("YEARMODA"))
            
            // turn each line into a list of samples
            .mergeMap(samples())
            
            // meter the samples
            .map(meter(metrics.meter("samples"), Sample.class))            
            ;
        
        
//        Observable<Sample> randomSamples = randomSamples(25300770, 40, "123456", "sendRate", metrics)
//                .subscribeOn(Schedulers.io())
//                .map(meter(metrics.meter("samples"), Sample.class))            
//                ;

        
        Observable<List<Sample>> batches = samples
            // create batches each second or of size m_samplesPerBatch whichever comes first
            .buffer(m_samplesPerBatch)
            ;
        
        Observable<Boolean> doImport;
        if (m_restUrl != null) {
            // a rest server was specified so post the data to the rest server
            doImport = restPoster(batches, metrics);
        } else {
            // write data directly to a SampleRepository
            doImport = directPoster(batches, metrics);
        }
        
        System.err.println("doImport = " + doImport);

        // GO!!!
        final AtomicReference<Subscription> subscription = new AtomicReference<>();
        final AtomicBoolean failed = new AtomicBoolean(false);
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        Subscription s = doImport.subscribe(new Observer<Boolean>() {

            @Override
            public void onCompleted() {
                System.err.println("Finished Importing Everything!");
                reporter.report();
                latch.countDown();
                System.exit(0);
            }

            @Override
            public void onError(Throwable e) {
                failed.set(true);
                System.err.println("Error importing!");
                e.printStackTrace();
                try {
                    //latch.await();
                    Subscription s = subscription.get();
                    if (s != null) s.unsubscribe();

                } catch (Exception ex) {
                    System.err.println("Failed to close httpClient!");
                    ex.printStackTrace();
                } finally {
                    //dumpThreads();
                }
            }

            @Override
            public void onNext(Boolean t) {
                System.err.println("Received a boolen: " + t);
            }
        });
        
        subscription.set(s);
        if (failed.get()) {
            s.unsubscribe();
        }
        //latch.countDown();
        System.err.println("Return from Subscribe!");
        
        latch.await();
        
        //dumpThreads();
        

    }
    
    private SampleRepository repository() {
        if (m_repository == null) {
            Injector injector = Guice.createInjector(new Config());
            m_repository = injector.getInstance(SampleRepository.class);
        }
        return m_repository;
    }


    private Observable<Boolean> directPoster(Observable<List<Sample>> samples, MetricRegistry metrics) {

        final SampleRepository repository = repository();
        final Timer timer = metrics.timer("writes");
        final Meter completions = metrics.meter("samples-completed");
        

        Func1<List<Sample>, Boolean> insert = new Func1<List<Sample>, Boolean>() {

            @Override
            public Boolean call(List<Sample> s) {
                int sz = s.size();
                try (Context timerCtx = timer.time()) {
                    repository.insert(s);
                    return true;
                } finally {
                    completions.mark(sz);
                }
            }
        };
        
        
        //Observable<Boolean> parallel = parallelMap(samples, 10, metrics, insert);
        
        Observable<Boolean> sequential = samples.map(insert);
        
        return sequential
               .all(Functions.<Boolean>identity());
        
        
    }
    
    private Observable<Boolean> restPoster(Observable<List<Sample>> samples,
            MetricRegistry metrics) {
        Observable<Boolean> doImport;
        final CloseableHttpAsyncClient httpClient = HttpAsyncClients.createDefault();
        httpClient.start();


        doImport = samples

                // turn each batch into json
                .map(toJSON())

                // meter them as the go into the post code
                .map(meter(metrics.meter("posts"), String.class))

                // post the json to the REST server
                .mergeMap(postJSON(m_restUrl, httpClient))

                // meter the responses
                .map(meter(metrics.meter("responses"), ObservableHttpResponse.class))
                
                // count sample completions
                .map(meter(metrics.meter("samples-completed"), m_samplesPerBatch, ObservableHttpResponse.class))

                // make sure every request has a successful return code
                .all(successful())
                
                .doOnCompleted(new Action0() {

                    @Override
                    public void call() {
                        try {
                            httpClient.close();
                        } catch (IOException e) {
                            System.err.println("Failed to close httpClient!");
                            e.printStackTrace();
                        }
                    }
                    
                });
        return doImport;
    }

    private static Func1<? super Path, ? extends Path> reportFile() {
        return new Func1<Path, Path>() {

            @Override
            public Path call(Path file) {
                System.err.println("Begin Processing: " + file);
                return file;
            }
            
        };
    }

    public static Func1<String, Observable<Sample>> samples() {
        final LineParser parser = new LineParser();
        return new Func1<String, Observable<Sample>>() {

            @Override
            public Observable<Sample> call(String line) {
                try {
                    return Observable.from(parser.parseLine(line));
                } catch (ParseException e) {
                    throw propagate(e);
                }
            }

        };
    }

    private static boolean isNaN(Sample sample) {
        return (sample.getType() == MetricType.GAUGE) && Double.isNaN(sample.getValue().doubleValue());
    }

    public static Func1<List<Sample>, String> toJSON() {
        return new Func1<List<Sample>, String>() {
            @Override
            public String call(List<Sample> samples) {
                JSONBuilder bldr = new JSONBuilder();

                for(Sample sample : samples) {
                    if (isNaN(sample)) continue;
                    //System.err.println("Importing: " + sample);
                    bldr.newObject();
                    bldr.attr("timestamp", sample.getTimestamp().asMillis());
                    bldr.attr("resource", sample.getResource());
                    bldr.attr("name", sample.getName());
                    bldr.attr("type", sample.getType().name());
                    if (sample.getType() == MetricType.GAUGE) {
                        bldr.attr("value", sample.getValue().doubleValue());
                    } else {
                        bldr.attr("value", sample.getValue().longValue());
                    }
                }

                return bldr.toString();
            }
        };
    }
    
    private static Func1<ObservableHttpResponse, Boolean> successful() {
        return new Func1<ObservableHttpResponse, Boolean>() {

            @Override
            public Boolean call(ObservableHttpResponse response) {
                if (response.getResponse().getStatusLine().getStatusCode() >= 400) {
                    throw new RuntimeException("Failed to post samples: " + response.getResponse().getStatusLine());
                }
                return true;
            }
            
        };
    }

    public static Func1<String, Observable<ObservableHttpResponse>> postJSON(final String baseURL, final CloseableHttpAsyncClient httpClient) {

        final URI baseURI = URI.create(baseURL);

        return new Func1<String, Observable<ObservableHttpResponse>>() {
            @Override
            public Observable<ObservableHttpResponse> call(String json) {
                try {
                    return ObservableHttp.createRequest(HttpAsyncMethods.createPost(baseURI, json, ContentType.APPLICATION_JSON), httpClient).toObservable();
                } catch (UnsupportedEncodingException e) {
                    throw Exceptions.propagate(e);
                }
            }
        };
    }
    
    private static final class OperatorParMap<T, R> implements Operator<R, T> {

        private final Func1<? super T, ? extends R> m_transformer;
        private final int m_threadCount; 
        private final MetricRegistry m_metrics;

        public OperatorParMap(int threadCount, MetricRegistry metrics, Func1<? super T, ? extends R> transformer) {
            m_transformer = transformer;
            m_threadCount = threadCount;
            m_metrics = metrics;
        }

        @Override
        public Subscriber<? super T> call(final Subscriber<? super R> o) {
            final ExecutorService executor = Executors.newFixedThreadPool(m_threadCount);
            final ExecutorCompletionService<R> completions = new ExecutorCompletionService<>(executor);
            final AtomicBoolean completed = new AtomicBoolean(false);
            final AtomicInteger inQueue = new AtomicInteger(0);
            final AtomicReference<Throwable> error = new AtomicReference<Throwable>(null);

            m_metrics.register("in-queue", new Gauge<Integer>() {

                @Override
                public Integer getValue() {
                    return inQueue.get();
                }
                
            });
            
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    try {

                        while(true) {
                            if (error.get() != null) {
                                if (!o.isUnsubscribed()) {
                                    o.onError(error.get());
                                }
                                // we've got an error so bail
                                return;
                            }
                            if (completed.get() && inQueue.get() == 0) {
                                if (!o.isUnsubscribed()) {
                                    o.onCompleted();
                                }
                                // we've been completed and the queue is empty to we're done
                                System.err.println("SHUTING DOWN PARMAP EXECUTOR!");
                                executor.shutdown();
                                return;
                            }

                            Future<R> future = completions.poll(1, TimeUnit.SECONDS);
                            if (future != null) {
                                // received the next item... decrement the queue count
                                inQueue.decrementAndGet();
                                
                                // now notifiery the subscriber
                                if (o.isUnsubscribed()) return;
                                o.onNext(future.get());
                            }
                        }
                        

                    } catch(Exception e) {
                        // an exception occurred so notify subscriber and bail
                        if (o.isUnsubscribed()) return;
                        o.onError(e);
                    } finally {
                        System.err.println("EXITTING PARMAP THREAD!");
                    }
                }
                
            };
            
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.start();
            
            return new Subscriber<T>(o) {

                @Override
                public void onCompleted() {
                    completed.set(true);
                }

                @Override
                public void onError(Throwable e) {
                    System.err.println("ERROR! " + e.getMessage());
                    e.printStackTrace();
                    error.set(e);
                }

                @Override
                public void onNext(final T t) {
                    if (o.isUnsubscribed()) return;
                    try {
                        inQueue.incrementAndGet();
                        completions.submit(new Callable<R>() {

                            @Override
                            public R call() throws Exception {
                                return m_transformer.call(t);
                            }
                        });
                    } catch (Throwable e) {
                        onError(OnErrorThrowable.addValueAsLastCause(e, t));
                    }
                }

            };
            
            
            
        }

    }    
    public static <R, T> Observable<R> parallelMap(Observable<T> o, int threadCount, MetricRegistry metrics, Func1<? super T, ? extends R> func) {
        return o.lift(new OperatorParMap<T, R>(threadCount, metrics, func));
    }
        
    public static Func1<String, Boolean> exclude(final String pattern) {
        return new Func1<String, Boolean>() {

            @Override
            public Boolean call(String s) {
                return !s.contains(pattern);
            }
            
        };
    }
    
    public static <T> Func1<T, T> meter(final Meter meter, Class<T> clazz) {
        return meter(meter, 1, clazz);
    }
    
    public static <T> Func1<T, T> meter(final Meter meter, final int count, Class<T> clazz) {
        return new Func1<T, T>() {

            @Override
            public T call(T t) {
                meter.mark(count);
                return t;
            }
            
        };
    }

    

}
