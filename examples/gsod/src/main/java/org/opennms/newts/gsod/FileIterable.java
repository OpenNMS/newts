package org.opennms.newts.gsod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.zip.GZIPInputStream;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

public class FileIterable {

    private static class PathHolder {
        Path m_path;
        Exception m_exception;
        
        PathHolder(Path p) {
            m_path = p;
        }
        
        PathHolder(Exception e) {
            m_exception = e;
        }
        
        void checkException() {
            if (m_exception != null) {
                throw Throwables.propagate(m_exception);
            }
        }
        
        Path getPath() {
            return m_path;
        }
        
        public String toString() {
            if (m_exception != null) {
                return m_exception.toString();
            } else {
                return ""+m_path;
            }
        }
    }
    
    private static final PathHolder END_OF_WALK = new PathHolder((Path)null);
    
    public static class WalkingPathIterator extends SimpleFileVisitor<Path> implements Iterator<Path> {
        private BlockingQueue<PathHolder> m_queue = new ArrayBlockingQueue<>(1);
        private PathHolder m_next;
        
        WalkingPathIterator(final Path root) {
            new Thread() {
                public void run() {
                    try {
                        Files.walkFileTree(root, WalkingPathIterator.this);
                    } catch (IOException e) {
                        m_queue.offer( new PathHolder(e) );
                    } finally {
                        m_queue.offer( END_OF_WALK );
                    }
                }
            }.start();
            
            m_next = take();
        }
        
        

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            m_queue.offer( new PathHolder( file ) );
            return FileVisitResult.CONTINUE;
        }

        @Override
        public boolean hasNext() {
           return m_next != END_OF_WALK;
        }



        PathHolder take() {
            try {
                return m_queue.take();
            } catch (InterruptedException e) {
                throw Throwables.propagate(e);
            }
        }

        @Override
        public Path next() {
            PathHolder p = m_next;
            if (p == END_OF_WALK) throw new NoSuchElementException();
            p.checkException();
            m_next = take();
            return p.getPath();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
        
    }
    
    public static FluentIterable<Path> fileTreeWalker(final Path root) {
        return new FluentIterable<Path>() {

            @Override
            public Iterator<Path> iterator() {
                return new WalkingPathIterator(root);
            }

        };
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
    
    private static KeyedIterable<Path, Path> children(File dir, FileFilter filter) {
        return new KeyedIterable<Path, Path>(dir.toPath(), toPaths(dir.listFiles(filter)));
    }

    private static KeyedIterable<Path, Path> files(File dir) {
        return children(dir, fileMatcher());
    }
    
    private static KeyedIterable<Path, Path> subdirs(File dir) {
        return children(dir, directoryMatcher());
    }


    private static FileFilter fileMatcher() {
        return new FileFilter() {
            
            @Override
            public boolean accept(File f) {
                return f.isFile();
            }
        };
    }
    
    private static FileFilter directoryMatcher() {
        return new FileFilter() {
            
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }
        };
    }


    public static Iterable<Path> toPaths(File[] files) {
        return files == null ? Collections.<Path>emptyList() : toPaths(Arrays.asList(files));
    }
    
    public static Iterable<Path> toPaths(Iterable<File> files) {
        return Iterables.transform(files, new Function<File, Path>(){

            @Override
            public Path apply(File input) {
                return input.toPath();
            }
            
        });
    }
    
    public static Iterable<File> toFiles(Iterable<Path> files) {
        return Iterables.transform(files, new Function<Path, File>(){

            @Override
            public File apply(Path input) {
                return input.toFile();
            }
            
        });
    }
    
    public static class GroupedPathIterator extends AbstractIterator<KeyedIterable<Path, Path>> {
        
        Stack<Iterator<Path>> m_dirStack;
        File m_root;
        
        GroupedPathIterator(File root) {
            m_root = root;
        }

        @Override
        protected KeyedIterable<Path, Path> computeNext() {
            if (m_dirStack == null) {
                m_dirStack = new Stack<Iterator<Path>>();
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
                throw Throwables.propagate(e);
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
                throw Throwables.propagate(e);
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
                    return new LineIterator(new FileReader(path.toFile()));
                } catch (FileNotFoundException e) {
                    throw Throwables.propagate(e);
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
                    throw Throwables.propagate(e);
                }
            }
        };
        
    }
    
    private static Reader zippedFileReader(final Path path, final Charset cs)  throws IOException {
        InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toFile()));
        return new InputStreamReader(gzipStream, cs);
    }
    
    public static <F, T> Function<? super Iterable<F>, Iterable<T>> bind(final Function<? super F, Iterable<T>> f) {
        return new Function<Iterable<F>, Iterable<T>>() {
            @Override
            public Iterable<T> apply(Iterable<F> input) {
                return Iterables.concat(Iterables.transform(input, f));
            }
        };
    }
    
    public static <F, T> Function<? super Iterable<F>, Iterable<T>> lift(final Function<F, T> f) {
        return new Function<Iterable<F>, Iterable<T>>() {

            @Override
            public Iterable<T> apply(Iterable<F> input) {
                return Iterables.transform(input, f);
            }
            
        };
    }
    

}
