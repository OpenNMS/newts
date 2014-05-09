package org.opennms.newts.gsod;

import static org.opennms.newts.gsod.FileIterable.*;

import java.io.File;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Scanner;

import org.junit.Test;
import org.opennms.newts.gsod.FileIterable.KeyedIterable;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

public class FileIterableTest {

    @Test
    public void testWalk() {
        Path root = new File("ftp.ncdc.noaa.gov/pub/data/gsod/data").toPath();
        
        Iterable<Path> paths = FileIterable.fileTreeWalker(root);
        
        for(Path p : paths) {
            System.err.println(p);
        }
    }
    
    @Test
    public void testLines() {
        File file = new File("ftp.ncdc.noaa.gov/pub/data/gsod/data/1988/722430-12960-1988.op.gz");
        
        Iterable<String> lines = FileIterable.unzipLines(file.toPath(), Charsets.US_ASCII);
        
        int count = 0;
        for(String line : lines ) {
            System.err.println((++count)+": "+line);
        }
    }
    
    @Test
    public void testCombine() {
        Path root = new File("ftp.ncdc.noaa.gov/pub/data/gsod/data").toPath();
    
        FluentIterable<KeyedIterable<Path,Path>> iterables = FileIterable.groupFilesByDir(root);
        
        for(Iterable<Path> it : iterables) {
            System.err.println("Next Iterable");
            for(Path p : it) {
                System.err.println(p);
            }
        }
        
    }
    
    @Test
    public void testMergeSort() {
     
        //Path root = new File("ftp.ncdc.noaa.gov/pub/data/gsod/data").toPath();
        Path root = new File("/Users/brozow/gsod/data").toPath();
        
        FluentIterable<Iterable<String>> iterables = FileIterable.groupFilesByDir(root).transform(lines("YEARMODA")).transform(mergeSorter());
        
        for(Iterable<String> yearData : iterables) {
            System.err.println("New Year");
            for(String s : yearData) {
                System.err.println(s);
            }
        }
        
    }
    
   

    private Function<? super Iterable<Iterable<String>>, Iterable<String>> mergeSorter() {
        return new Function<Iterable<Iterable<String>>, Iterable<String>>() {

            @Override
            public Iterable<String> apply(Iterable<Iterable<String>> input) {
                return Iterables.mergeSorted(input, byDate());
            }
            
        };
    }

    private Comparator<? super String> byDate() {
        return new Comparator<String>() {

            @Override
            public int compare(String o1, String o2) {
                Scanner s1 = new Scanner(o1);
                Scanner s2 = new Scanner(o2);
                
                String stn1 = s1.next();
                String wban1 = s1.next();
                long d1 = s1.nextLong();
                
                String stn2 = s2.next();
                String wban2 = s2.next();
                long d2 = s2.nextLong();
                
                if (d1 < d2) return -1;
                if (d1 > d2) return 1;
                
                if (stn1.compareTo(stn2) < 0) return -1;
                if (stn1.compareTo(stn2) > 0) return 1;
                
                return wban1.compareTo(wban2);
                
                
            }
        };
    }

    private Function<? super Iterable<Path>, Iterable<Iterable<String>>> lines(final String excludePattern) {
        return FileIterable.lift(new Function<Path, Iterable<String>>() {
        
            @Override
            public Iterable<String> apply(Path input) {
                return FileIterable.unzipLines(input, Charsets.US_ASCII).filter(exludes(excludePattern));
            }
        
        });
    }
    
    private Predicate<? super String> exludes(final String excludePattern) {
        return new Predicate<String>() {

            @Override
            public boolean apply(String input) {
                return !input.contains(excludePattern);
            }
            
        };
    }

    
}
