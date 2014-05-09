package org.opennms.newts.gsod;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import rx.Observable;
import rx.Observer;
import rx.exceptions.Exceptions;
import rx.functions.Func1;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

public class FileObservableTest {
    
    @Rule public TestName name = new TestName();

    @Test
    public void testWalking() throws IOException {
        
        Path dir = tempDir("target/testing/walker-test");
        
        List<Path> files = createTestTree(dir, "a/a", "a/b", "b/a", "b/b");
        
        TestObserver<Path> testObserver = new TestObserver<>();
        
        Observable<Path> walk = FileObservable.fileTreeWalker(dir);
        
        walk.subscribe(testObserver);
        
        testObserver.assertReceivedOnNext(files);
        testObserver.assertTerminalEvent();
        
        
        
        
    }
    
    @Test
    public void testLines() throws IOException {
        
        Path testFile = tempFile("target/testing/lines-test");
        
        List<String> lines = createTestFile(testFile,
           "line1",
           "line2",
           "line3",
           "",
           "line5"
        );
        
        
        TestObserver<String> testObserver = new TestObserver<>();
        
        Observable<String> contents = FileObservable.lines(testFile);
        
        contents.subscribe(testObserver);
        
        testObserver.assertReceivedOnNext(lines);
        testObserver.assertTerminalEvent();
    }
    
    @Test
    public void testConcat() throws IOException, InterruptedException {
        Path dir = tempDir("target/testing/concat-test");
        
        List<String> files = Arrays.asList(
          "a/a",
          "a/b",
          "a/c",
          "b/a",
          "b/b",
          "c/c"
        );
        
        List<String> contents = Arrays.asList(
           "line1",
           "line2",
           "line3",
           "line4"
        );
        
        
        List<String> expected = createTestTree(dir, files, contents);

        TestObserver<String> testObserver = new TestObserver<>();
            
        final CountDownLatch latch = new CountDownLatch(1);
        
        FileObservable.fileTreeWalker(dir)
            .observeOn(Schedulers.executor(Executors.newScheduledThreadPool(10)))
            .concatMap(FileObservable.lines())
            .map(new Func1<String, String>() {

                @Override
                public String call(String t1) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    return new Date() + ":"+ t1;
                }
            })
            .observeOn(Schedulers.executor(Executors.newScheduledThreadPool(10)))
            .buffer(8)
            .subscribe(new Observer<List<String>>() {

                @Override
                public void onCompleted() {
                    latch.countDown();
                }

                @Override
                public void onError(Throwable e) {
                    latch.countDown();
                }

                @Override
                public void onNext(List<String> t) {
                    System.err.printf("%s: %s\n", Thread.currentThread(), t);
                }
            });
        
        
            latch.await();
//        throwObserverErrors(testObserver);
//        testObserver.assertReceivedOnNext(expected);
//        testObserver.assertTerminalEvent();
        
    }
    
    private <T> void throwObserverErrors(TestObserver<T> observer) {
        for(Throwable t : observer.getOnErrorEvents()) {
            Exceptions.propagate(t);
        }
    }
    
    private List<String> createTestTree(Path dir, List<String> files, List<String> contents) throws IOException {
        List<String> strings = new ArrayList<String>();
        for(String file: files) {
            List<String> data = new ArrayList<String>();
            for(String line : contents) {
                String l = file+":"+line;
                data.add(l);
                strings.add(l);
            }
            Path f = dir.resolve(file);
            f.getParent().toFile().mkdirs();
            writeGzipped(f, data, Charset.defaultCharset());
        }
        return strings;
      
    }
    
    private void writeGzipped(Path f, List<String> lines, Charset cs) throws FileNotFoundException, IOException {
        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f.toFile())), cs))) {
           for (String line : lines) {
               out.append(line);
               out.newLine();
           }
        }
    }
    
    private List<String> createTestFile(Path file, String... lines) throws IOException {
        List<String> list = Arrays.asList(lines);
        Files.write(file, list, Charset.defaultCharset());
        return list;
    }

    private List<Path> createTestTree(Path root, String... files) throws IOException {
        
        root.toFile().mkdirs();

        List<Path> paths = new ArrayList<Path>();
        for(String file : files) {
            Path f = root.resolve(file);
            f.getParent().toFile().mkdirs();
            if (!Files.exists(f)) {
                Files.createFile(f);
            }
            paths.add(f);
        }
        return paths;
    }
    
    
    private Path tempDir(String root) throws IOException {
        File rootDir = new File(root);
        rootDir.mkdirs();
        return Files.createTempDirectory(rootDir.toPath(), name.getMethodName());
    }
    
    private Path tempFile(String dir) throws IOException {
        File d = new File(dir);
        d.mkdirs();
        return Files.createTempFile(d.toPath(), name.getMethodName(), ".txt");
    }
    
    
}
