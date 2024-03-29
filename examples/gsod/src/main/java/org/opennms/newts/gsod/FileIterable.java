/*
 * Copyright 2014-2024, The OpenNMS Group
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

@SuppressWarnings("java:S4738")
public class FileIterable {
    
    private FileIterable() {}

    public static FluentIterable<Path> fileTreeWalker(final Path root) {
        return FluentIterable.from(Iterables.concat(groupFilesByDir(root)));
    }
    
    public static class KeyedIterable<K, T> extends FluentIterable<T> {
        
        private K m_key;
        private Iterable<T> m_items;
        

        public KeyedIterable(K key, Iterable<T> items) {
            m_key = key;
            m_items = items;
        }

        @Override
        public Iterator<T> iterator() {
            return m_items.iterator();
        }
        
        public K getKey() {
            return m_key;
        }

    }
    
    @SuppressWarnings("java:S1149")
    public static class GroupedPathIterator extends AbstractIterator<KeyedIterable<Path, Path>> {
        
        Stack<Iterator<Path>> m_dirStack;
        File m_root;
        
        GroupedPathIterator(File root) {
            m_root = root;
        }

        @Override
        protected KeyedIterable<Path, Path> computeNext() {
            if (m_dirStack == null) {
                m_dirStack = new Stack<>();
                m_dirStack.push(subdirs(m_root).iterator());
                return files(m_root);
            } 
            
            while(!m_dirStack.isEmpty()) {
                Iterator<Path> subdirs = m_dirStack.peek();
                if (!subdirs.hasNext()) {
                    m_dirStack.pop();
                } else {
                    File dir = subdirs.next().toFile();
                    m_dirStack.push(subdirs(dir).iterator());
                    return files(dir);
                }
            }
            
            endOfData();
            return null;
        }
        
        private static KeyedIterable<Path, Path> files(final File dir) {
            return children(dir, File::isFile);
        }
        
        private static KeyedIterable<Path, Path> subdirs(final File dir) {
            return children(dir, File::isDirectory);
        }

        private static KeyedIterable<Path, Path> children(File dir, FileFilter filter) {
            return new KeyedIterable<>(dir.toPath(), toPaths(dir.listFiles(filter)));
        }

        public static Iterable<Path> toPaths(File[] files) {
            return files == null ? Collections.<Path>emptyList() : toPaths(Arrays.asList(files));
        }
        
        public static Iterable<Path> toPaths(Iterable<File> files) {
            return Iterables.transform(files, File::toPath);
        }
        
        public static Iterable<File> toFiles(Iterable<Path> files) {
            return Iterables.transform(files, Path::toFile);
        }
    }
    
    public static FluentIterable<KeyedIterable<Path, Path>> groupFilesByDir(final Path root) {
        return new FluentIterable<KeyedIterable<Path, Path>>() {

            @Override
            public Iterator<KeyedIterable<Path, Path>> iterator() {
                return new GroupedPathIterator(root.toFile());
            }
            
        };

    }
    
    private static class LineIterator implements Iterator<String> {
        
        private BufferedReader m_in;
        private String m_nextLine;
        
        public LineIterator(Reader r) {
            m_in = new BufferedReader(r);
            fetchLine();
        }
        
        private void fetchLine() {
            if (m_in == null) return;
            try {
                m_nextLine = m_in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (m_nextLine == null) {
                    close();
                }
            }
        }
        
        private void close() {
            try {
                if (m_in != null) {
                    m_in.close();
                }
            } catch(IOException e) {
                throw new RuntimeException(e);
            } finally {
                m_in = null;
            }
        }


        @Override
        public boolean hasNext() {
            return m_nextLine != null;
        }

        @Override
        public String next() {
            String line = m_nextLine;
            fetchLine();
            if (line == null) throw new NoSuchElementException();
            return line;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator<String>.remove is not yet implemented.");
        }
        
    }
    
    public static FluentIterable<String> lines(final Path path) {
        return new FluentIterable<String>() {

            @Override
            public Iterator<String> iterator() {
                try {
                    return new LineIterator(new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_8));
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
    }
    
    public static FluentIterable<String> unzipLines(final Path path, final Charset cs) {
        return new FluentIterable<String>() {

            @Override
            public Iterator<String> iterator() {
                try {
                    return new LineIterator(zippedFileReader(path, cs));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        
    }
    
    private static Reader zippedFileReader(final Path path, final Charset cs)  throws IOException {
        InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toFile()));
        return new InputStreamReader(gzipStream, cs);
    }
    
    public static <F, T> Function<Iterable<F>, Iterable<T>> bind(final Function<? super F, Iterable<T>> f) {
        return input -> Iterables.concat(Iterables.transform(input, f));
    }
    
    public static <F, T> Function<Iterable<F>, Iterable<T>> lift(final Function<F, T> f) {
        return input -> Iterables.transform(input, f);
    }
    

}
