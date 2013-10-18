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

import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Lists;


public class FileImport implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(FileImport.class);
    
    private final DateFormat m_dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
    private final MeasurementRepository m_repository;
    private final BufferedReader m_reader;
    private final Counter m_numRows;
    private final Counter m_numMeasurements;

    public FileImport(MeasurementRepository repository, MetricRegistry metrics, Path path)
            throws FileNotFoundException, IOException {

        m_repository = repository;
        m_numRows = metrics.counter("num-rows");
        m_numMeasurements = metrics.counter("num-measurements");

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

                List<Measurement> measurements = Lists.newArrayList();

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
                measurements.add(new Measurement(ts, station, "meanTemperature", GAUGE, valueFor(meanTemp, 9999.9)));

                int meanTemperatureCount = scanner.nextInt();

                double dewpoint = scanner.nextDouble();
                measurements.add(new Measurement(ts, station, "dewPoint", GAUGE, valueFor(dewpoint, 9999.9)));

                int dewpointCount = scanner.nextInt();

                double seaLevelPressure = valueFor(scanner.nextDouble(), 9999.9);
                measurements.add(new Measurement(ts, station, "seaLevelPressure", GAUGE, seaLevelPressure));

                int seaLevelPressureCount = scanner.nextInt();

                double stationPressure = valueFor(scanner.nextDouble(), 9999.9);
                measurements.add(new Measurement(ts, station, "stationPressure", GAUGE, stationPressure));

                int stationPressureCount = scanner.nextInt();

                double visibility = valueFor(scanner.nextDouble(), 999.9);
                measurements.add(new Measurement(ts, station, "visibility", GAUGE, visibility));

                int visibilityCount = scanner.nextInt();

                double meanWindSpeed = scanner.nextDouble();
                measurements.add(new Measurement(ts, station, "meanWindSpeed", GAUGE, meanWindSpeed));

                int meanWindSpeedCount = scanner.nextInt();

                double maxWindSpeed = valueFor(scanner.nextDouble(), 999.9);
                measurements.add(new Measurement(ts, station, "maxWindSpeed", GAUGE, maxWindSpeed));

                double maxWindGust = valueFor(scanner.nextDouble(), 999.9);
                measurements.add(new Measurement(ts, station, "maxWindGust", GAUGE, maxWindGust));

                double maxTemperature = valueFor(Double.parseDouble(scanner.next().replace("*", "")), 9999.9);
                measurements.add(new Measurement(ts, station, "maxTemperature", GAUGE, maxTemperature));

                double minTemperature = valueFor(Double.parseDouble(scanner.next().replace("*", "")), 9999.9);
                measurements.add(new Measurement(ts, station, "minTemperature", GAUGE, minTemperature));

                LOG.trace("Station number {}, WBAN {}, date {}, Max Temp {}...", station, wban, dateYMD, maxTemperature);

                scanner.close();

                m_repository.insert(measurements);

                m_numRows.inc();
                m_numMeasurements.inc(10);

            }

        }
        catch (IOException e) {
            LOG.error("Error reading GSOD data file: {]", e);
        }
    }

    private static double valueFor(double value, double nan) {
        return (value == nan) ? Double.NaN : value;
    }

}
