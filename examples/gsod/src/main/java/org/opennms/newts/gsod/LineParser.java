package org.opennms.newts.gsod;

import static org.opennms.newts.api.MetricType.GAUGE;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.opennms.newts.api.Gauge;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LineParser {
    
    private static final Logger LOG = LoggerFactory.getLogger(FileImport.class);
    
    private static final ThreadLocal<DateFormat> m_dateFormat = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
        }
        
    };
    
    public static DateFormat getDateFormat() {
        return m_dateFormat.get();
    }

    List<Sample> parseLine(String line) throws ParseException {
        LOG.trace("Parsing {}", line);
    
        List<Sample> samples = Lists.newArrayList();
    
        Resource station = new Resource(stringAt(line, 0));
        String wban      = stringAt(line, 7);
        String dateYMD   = stringAt(line, 14);
    
        Date date = getDateFormat().parse(dateYMD);
    
        Timestamp ts = new Timestamp(date.getTime(), TimeUnit.MILLISECONDS);
    
        double meanTemp = doubleAt(line, 24);
        samples.add(new Sample(ts, station, "meanTemperature", GAUGE, LineParser.valueFor(meanTemp, 9999.9)));
    
        double dewpoint = doubleAt(line, 35);
        samples.add(new Sample(ts, station, "dewPoint", GAUGE, LineParser.valueFor(dewpoint, 9999.9)));
    
        double slp = doubleAt(line, 46);
        Gauge seaLevelPressure = LineParser.valueFor(slp, 9999.9);
        samples.add(new Sample(ts, station, "seaLevelPressure", GAUGE, seaLevelPressure));

        double stp = doubleAt(line, 57);
        Gauge stationPressure = LineParser.valueFor(stp, 9999.9);
        samples.add(new Sample(ts, station, "stationPressure", GAUGE, stationPressure));

        double vis = doubleAt(line, 68);
        Gauge visibility = LineParser.valueFor(vis, 999.9);
        samples.add(new Sample(ts, station, "visibility", GAUGE, visibility));

        double speed = doubleAt(line, 78);
        Gauge meanWindSpeed = new Gauge(speed);
        samples.add(new Sample(ts, station, "meanWindSpeed", GAUGE, meanWindSpeed));

        double maxSpeed = doubleAt(line, 88);
        Gauge maxWindSpeed = LineParser.valueFor(maxSpeed, 999.9);
        samples.add(new Sample(ts, station, "maxWindSpeed", GAUGE, maxWindSpeed));
    
        double maxGust = doubleAt(line, 95);
        Gauge maxWindGust = LineParser.valueFor(maxGust, 999.9);
        samples.add(new Sample(ts, station, "maxWindGust", GAUGE, maxWindGust));
    
        double maxTemp = doubleAt(line, 102);
        Gauge maxTemperature = LineParser.valueFor(maxTemp, 9999.9);
        samples.add(new Sample(ts, station, "maxTemperature", GAUGE, maxTemperature));
    
        double minTemp = doubleAt(line, 110);
        Gauge minTemperature = LineParser.valueFor(minTemp, 9999.9);
        samples.add(new Sample(ts, station, "minTemperature", GAUGE, minTemperature));
        
        LOG.trace("Station number {}, WBAN {}, date {}, Max Temp {}...", station, wban, dateYMD, maxTemperature);
        
        return samples;
    }

    public boolean isDigit(char ch) {
        return ('0' <= ch && ch <= '9');
    }
    
    public String stringAt(String line, int index) {
        StringBuilder buf = new StringBuilder();
        char ch;
        while(index < line.length() && (ch = line.charAt(index)) != ' ') {
            buf.append(ch);
            index++;
        }
        
        return buf.toString();
    }
    
    public long longAt(String line, int index) {
        index = skipLeadingSpaces(line, index);
        boolean negative = false;
        if (line.charAt(index) == '-') {
            negative = true;
            index++;
        }
        long n = 0;
        char ch;
        while(index < line.length() && isDigit(ch = line.charAt(index))) {
            n = n*10 + (ch - '0');
            index++;
        }
        return negative ? -n : n;
    }
    
    public int skipLeadingSpaces(String line, int index) {
        while(index < line.length() && line.charAt(index) == ' ') {
            index++;
        }
        return index;
    }
    
    public int intAt(String line, int index) {
        index = skipLeadingSpaces(line, index);
        boolean negative = false;
        if (line.charAt(index) == '-') {
            negative = true;
            index++;
        }
        int n = 0;
        char ch;
        while(index < line.length() && isDigit(ch = line.charAt(index))) {
            n = n*10 + (ch - '0');
            index++;
        }
        return negative ? -n : n;
    }
    
    public double doubleAt(String line, int index) {
        index = skipLeadingSpaces(line, index);
        boolean negative = false;
        if (line.charAt(index) == '-') {
            negative = true;
            index++;
        }
        double d = 0.0;
        char ch;
        while(index < line.length() && isDigit(ch = line.charAt(index))) {
            d = d*10.0 + (ch - '0');
            index++;
        }
        
        ch = line.charAt(index);
        if (ch != '.') return negative ? -d : d;
        
        //skip decimal point
        index++;
        
        double scale = 1/10.0;
        
        while(index < line.length() && isDigit(ch = line.charAt(index))) {
            d = d + scale*(ch - '0');
            scale = scale/10.0;
            index++;
        }
        
        return negative ? -d : d;
        
    }

    public static Gauge valueFor(double value, double nan) {
        return new Gauge((value == nan ? Double.NaN : value));
    }

}
