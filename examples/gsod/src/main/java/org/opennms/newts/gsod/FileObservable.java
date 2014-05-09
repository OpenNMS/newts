package org.opennms.newts.gsod;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.GZIPInputStream;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func1;

public class FileObservable {
    
    public static Observable<Path> fileTreeWalker(final Path root) {
        
        return Observable.create(new OnSubscribe<Path>() {
            @Override
            public void call(final Subscriber<? super Path> o) {
                SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (o.isUnsubscribed()) return FileVisitResult.TERMINATE;
                        o.onNext(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                        if (o.isUnsubscribed()) return FileVisitResult.TERMINATE;
                        o.onError(exc);
                        return FileVisitResult.TERMINATE;
                    }
                    
                };
                try {
                    Files.walkFileTree(root, visitor);
                    if (o.isUnsubscribed()) return;
                    o.onCompleted();
                } catch (IOException e) {
                    if (o.isUnsubscribed()) return;
                    o.onError(e);
                }
            }

        });
        
    }
    
    
    public static Observable<String> lines(final Path path) {
        return Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> s) {
                try (BufferedReader in = fileReader(path))
                {
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        if (s.isUnsubscribed()) return;
                        s.onNext(line);
                    }
                    s.onCompleted();
                    
                } catch (Exception e) {
                    if (!s.isUnsubscribed()) s.onError(e);
                }
            }
            
        });
    }
    
    public static Observable<String> unzipLines(final Path path) {
        return Observable.create(new OnSubscribe<String>() {

            @Override
            public void call(Subscriber<? super String> s) {
                try (BufferedReader in = zippedFileReader(path))
                {
                    String line = null;
                    while ((line = in.readLine()) != null) {
                        if (s.isUnsubscribed()) return;
                        s.onNext(line);
                    }
                    s.onCompleted();
                    
                } catch (Exception e) {
                    if (!s.isUnsubscribed()) s.onError(e);
                }
            }
            
        });
        
    }
    
    public static Func1<Path, Observable<String>> lines() {
        return new Func1<Path, Observable<String>>() {

            @Override
            public Observable<String> call(Path path) {
                return FileObservable.unzipLines(path);
            }
        };
    }
    



    private static BufferedReader fileReader(final Path path)  throws IOException {
        return Files.newBufferedReader(path, Charset.forName("US-ASCII"));
    }

    private static BufferedReader zippedFileReader(final Path path)  throws IOException {
        InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toFile()));
        return new BufferedReader(new InputStreamReader(gzipStream, "US-ASCII"));
    }

}
