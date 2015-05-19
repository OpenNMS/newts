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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.gsod.FileIterable.KeyedIterable;

import com.google.common.base.Charsets;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class FileIterableTest {

    @Test
    public void testWalk() {
        Path root = new File("ftp.ncdc.noaa.gov/pub/data/gsod/1988").toPath();
        
        Iterable<Path> paths = FileIterable.fileTreeWalker(root);
        
        for(Path p : paths) {
            System.err.println(p);
        }
        
        
        List<Path> results = Lists.newArrayList();
        Iterables.addAll(results, paths);
        
        assertEquals(6,results.size());
    }
    
    @Test
    public void testLines() {
        File file = new File("ftp.ncdc.noaa.gov/pub/data/gsod/1988/722430-12960-1988.op.gz");
        
        Iterable<String> lines = FileIterable.unzipLines(file.toPath(), Charsets.US_ASCII);
        
        int count = 0;
        for(String line : lines ) {
            System.err.println((++count)+": "+line);
        }
    }
    
    @Test
    public void testCombine() {
        Path root = new File("ftp.ncdc.noaa.gov/pub/data/gsod/1988").toPath();
    
        FluentIterable<KeyedIterable<Path,Path>> iterables = FileIterable.groupFilesByDir(root);
        
        for(Iterable<Path> it : iterables) {
            System.err.println("Next Iterable");
            for(Path p : it) {
                System.err.println(p);
            }
        }
        
    }
    
    
}
