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


import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;


public class FileImport implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FileImport.class);
    
    private final SampleRepository m_repository;
    private final BufferedReader m_reader;
    private final Counter m_numRows;
    private final Counter m_numSamples;
    private final Timer m_writeTimer;

    private LineParser m_lineParser;

    public FileImport(SampleRepository repository, MetricRegistry metrics, Path path)
            throws FileNotFoundException, IOException {

        m_repository = repository;
        m_numRows = metrics.counter("num-rows");
        m_numSamples = metrics.counter("num-samples");
        m_writeTimer = metrics.timer("writes");

        InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toString()));
        m_reader = new BufferedReader(new InputStreamReader(gzipStream, "US-ASCII"));
        
        m_lineParser = new LineParser();

    }

    @SuppressWarnings("unused")
    @Override
    public void run() {

        String line;

        try {

            // Throw away the first (header).
            m_reader.readLine();

            while ((line = m_reader.readLine()) != null) {

                try {
                    List<Sample> samples = m_lineParser.parseLine(line);

                    Context timerCtx = m_writeTimer.time();

                    try {
                        m_repository.insert(samples);
                    }
                    finally {
                        timerCtx.stop();
                    }

                    m_numRows.inc();
                    m_numSamples.inc(10);

                }
                catch (ParseException e) {
                    LOG.error("Unable to parse date from line '{}'", line);
                }
            }

        }
        catch (IOException e) {
            LOG.error("Error reading GSOD data file: {]", e);
        }
    }

}
