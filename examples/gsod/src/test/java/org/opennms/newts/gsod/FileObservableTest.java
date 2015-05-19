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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import rx.Observable;
import rx.observers.TestObserver;

public class FileObservableTest {
    
    @Rule
    public TestName name = new TestName();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testWalking() throws IOException, InterruptedException {

        Path dir = tempFolder.getRoot().toPath();

        List<Path> files = createTestTree(dir, "a/a", "a/b", "b/a", "b/b");

        TestObserver<Path> testObserver = new TestObserver<>();
        
        Observable<Path> walk = FileObservable.fileTreeWalker(dir);
        
        walk.subscribe(testObserver);
        
        List<Path> observedFiles = testObserver.getOnNextEvents();
        
        assertTrue(files.containsAll(observedFiles) && observedFiles.containsAll(files));
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
    
    private List<String> createTestFile(Path file, String... lines) throws IOException {
        List<String> list = Arrays.asList(lines);
        Files.write(file, list, Charset.defaultCharset());
        return list;
    }

    private List<Path> createTestTree(Path root, String... files) throws IOException {

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

    private Path tempFile(String dir) throws IOException {
        File d = new File(dir);
        d.mkdirs();
        return Files.createTempFile(d.toPath(), name.getMethodName(), ".txt");
    }

}
