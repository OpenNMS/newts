package org.opennms.newts.gsod;


import static org.opennms.newts.api.MetricType.GAUGE;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;


public class FileImport implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FileImport.class);
    
    private final DateFormat m_dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private final SampleRepository m_repository;
    private final BufferedReader m_reader;
    private final Counter m_numRows;
    private final Counter m_numSamples;
    private final Timer m_writeTimer;

    public FileImport(SampleRepository repository, MetricRegistry metrics, Path path)
            throws FileNotFoundException, IOException {

        m_repository = repository;
        m_numRows = metrics.counter("num-rows");
        m_numSamples = metrics.counter("num-samples");
        m_writeTimer = metrics.timer("writes");

        InputStream gzipStream = new GZIPInputStream(new FileInputStream(path.toString()));
        m_reader = new BufferedReader(new InputStreamReader(gzipStream, "US-ASCII"));

    }

    @SuppressWarnings("unused")
    @Override
    public void run() {

        String line;

        try {

            // Throw away the first (header).
            m_reader.readLine();

            while ((line = m_reader.readLine()) != null) {

                LOG.trace("Parsing {}", line);

                List<Sample> samples = Lists.newArrayList();

                Scanner scanner = new Scanner(line);

                String station = scanner.next();
                String wban = scanner.next();
                String dateYMD = scanner.next();

                Date date;

                try {
                    date = m_dateFormat.parse(dateYMD);
                }
                catch (ParseException e) {
                    LOG.error("Unable to parse date from '{}'", dateYMD);
                    continue;
                }

                Timestamp ts = new Timestamp(date.getTime(), TimeUnit.MILLISECONDS);

                double meanTemp = scanner.nextDouble();
                samples.add(new Sample(ts, station, "meanTemperature", GAUGE, valueFor(meanTemp, 9999.9)));

                int meanTemperatureCount = scanner.nextInt();

                double dewpoint = scanner.nextDouble();
                samples.add(new Sample(ts, station, "dewPoint", GAUGE, valueFor(dewpoint, 9999.9)));

                int dewpointCount = scanner.nextInt();

                Gauge seaLevelPressure = valueFor(scanner.nextDouble(), 9999.9);
                samples.add(new Sample(ts, station, "seaLevelPressure", GAUGE, seaLevelPressure));

                int seaLevelPressureCount = scanner.nextInt();

                Gauge stationPressure = valueFor(scanner.nextDouble(), 9999.9);
                samples.add(new Sample(ts, station, "stationPressure", GAUGE, stationPressure));

                int stationPressureCount = scanner.nextInt();

                Gauge visibility = valueFor(scanner.nextDouble(), 999.9);
                samples.add(new Sample(ts, station, "visibility", GAUGE, visibility));

                int visibilityCount = scanner.nextInt();

                Gauge meanWindSpeed = new Gauge(scanner.nextDouble());
                samples.add(new Sample(ts, station, "meanWindSpeed", GAUGE, meanWindSpeed));

                int meanWindSpeedCount = scanner.nextInt();

                Gauge maxWindSpeed = valueFor(scanner.nextDouble(), 999.9);
                samples.add(new Sample(ts, station, "maxWindSpeed", GAUGE, maxWindSpeed));

                Gauge maxWindGust = valueFor(scanner.nextDouble(), 999.9);
                samples.add(new Sample(ts, station, "maxWindGust", GAUGE, maxWindGust));

                Gauge maxTemperature = valueFor(Double.parseDouble(scanner.next().replace("*", "")), 9999.9);
                samples.add(new Sample(ts, station, "maxTemperature", GAUGE, maxTemperature));

                Gauge minTemperature = valueFor(Double.parseDouble(scanner.next().replace("*", "")), 9999.9);
                samples.add(new Sample(ts, station, "minTemperature", GAUGE, minTemperature));

                LOG.trace("Station number {}, WBAN {}, date {}, Max Temp {}...", station, wban, dateYMD, maxTemperature);

                scanner.close();

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

        }
        catch (IOException e) {
            LOG.error("Error reading GSOD data file: {]", e);
        }
    }

    private static Gauge valueFor(double value, double nan) {
        return new Gauge((value == nan ? Double.NaN : value));
    }

}
