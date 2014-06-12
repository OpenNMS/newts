package org.opennms.newts.reporter.metrics;


import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

/**
 * A reporter which creates a comma-separated values file of the measurements for each metric.
 */
public class NewtsReporter extends ScheduledReporter {
    /**
     * Returns a new {@link Builder} for {@link NewtsReporter}.
     *
     * @param registry the registry to report
     * @return a {@link Builder} instance for a {@link NewtsReporter}
     */
    public static Builder forRegistry(MetricRegistry registry) {
        return new Builder(registry);
    }

    /**
     * A builder for {@link NewtsReporter} instances. Defaults to using the default locale, converting
     * rates to events/second, converting durations to milliseconds, and not filtering metrics.
     */
    public static class Builder {
        private final MetricRegistry registry;
        private String name;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;
        private Clock clock;
        private MetricFilter filter;

        private Builder(MetricRegistry registry) {
            this.registry = registry;
            this.rateUnit = TimeUnit.SECONDS;
            this.durationUnit = TimeUnit.MILLISECONDS;
            this.clock = Clock.defaultClock();
            this.filter = MetricFilter.ALL;
        }

        /**
         * Convert rates to the given time unit.
         *
         * @param rateUnit a unit of time
         * @return {@code this}
         */
        public Builder convertRatesTo(TimeUnit rateUnit) {
            this.rateUnit = rateUnit;
            return this;
        }

        /**
         * Convert durations to the given time unit.
         *
         * @param durationUnit a unit of time
         * @return {@code this}
         */
        public Builder convertDurationsTo(TimeUnit durationUnit) {
            this.durationUnit = durationUnit;
            return this;
        }

        /**
         * Use the given {@link Clock} instance for the time.
         *
         * @param clock a {@link Clock} instance
         * @return {@code this}
         */
        public Builder withClock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Only report metrics which match the given filter.
         *
         * @param filter a {@link MetricFilter}
         * @return {@code this}
         */
        public Builder filter(MetricFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }
        /**
         * Builds a {@link NewtsReporter} with the given properties, writing {@code .csv} files to the
         * given directory.
         *
         * @param directory the directory in which the {@code .csv} files will be created
         * @return a {@link NewtsReporter}
         */
        public NewtsReporter build(SampleRepository repository) {
            return new NewtsReporter(registry,
                                   repository,
                                   name,
                                   rateUnit,
                                   durationUnit,
                                   clock,
                                   filter);
        }

    }

    private final SampleRepository repository;
    private final String name;
    private final Clock clock;

    private NewtsReporter(MetricRegistry registry,
                        SampleRepository repository,
                        String name,
                        TimeUnit rateUnit,
                        TimeUnit durationUnit,
                        Clock clock,
                        MetricFilter filter) {
        super(registry, "newts-reporter", filter, rateUnit, durationUnit);
        this.repository = repository;
        this.name = name;
        this.clock = clock;
    }

    @Override
    public void report(SortedMap<String, Gauge> gauges,
                       SortedMap<String, Counter> counters,
                       SortedMap<String, Histogram> histograms,
                       SortedMap<String, Meter> meters,
                       SortedMap<String, Timer> timers) {
        Timestamp timestamp = Timestamp.fromEpochMillis(clock.getTime());
        
        List<Sample> samples = Lists.newArrayList();

        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            reportGauge(samples, timestamp, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            reportCounter(samples, timestamp, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            reportHistogram(samples, timestamp, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            reportMeter(samples, timestamp, entry.getKey(), entry.getValue());
        }

        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            reportTimer(samples, timestamp, entry.getKey(), entry.getValue());
        }
        
        this.repository.insert(samples);
        
        
    }

    private void reportTimer(List<Sample> samples, Timestamp timestamp, String name, Timer timer) {
        final Snapshot snapshot = timer.getSnapshot();
        Map<String, String> rateAttr = Maps.newHashMap();
        rateAttr.put("rate_unit", getRateUnit());
        Map<String, String> durationAttr = Maps.newHashMap();
        durationAttr.put("duration_unit", getDurationUnit());
        reportC(samples, timestamp, name, "count",     timer.getCount());
        reportG(samples, timestamp, name, "max",       convertDuration(snapshot.getMax()), durationAttr);
        reportG(samples, timestamp, name, "mean",      convertDuration(snapshot.getMean()), durationAttr);
        reportG(samples, timestamp, name, "min",       convertDuration(snapshot.getMin()), durationAttr);
        reportG(samples, timestamp, name, "stddev",    convertDuration(snapshot.getStdDev()), durationAttr);
        reportG(samples, timestamp, name, "p50",       convertDuration(snapshot.getMedian()), durationAttr);
        reportG(samples, timestamp, name, "p75",       convertDuration(snapshot.get75thPercentile()), durationAttr);
        reportG(samples, timestamp, name, "p95",       convertDuration(snapshot.get95thPercentile()), durationAttr);
        reportG(samples, timestamp, name, "p98",       convertDuration(snapshot.get98thPercentile()), durationAttr);
        reportG(samples, timestamp, name, "p99",       convertDuration(snapshot.get99thPercentile()), durationAttr);
        reportG(samples, timestamp, name, "p999",      convertDuration(snapshot.get999thPercentile()), durationAttr);
        reportG(samples, timestamp, name, "mean_rate", convertRate(timer.getMeanRate()), rateAttr);
        reportG(samples, timestamp, name, "m1_rate",   convertRate(timer.getOneMinuteRate()), rateAttr);
        reportG(samples, timestamp, name, "m5_rate",   convertRate(timer.getFiveMinuteRate()), rateAttr);
        reportG(samples, timestamp, name, "m15_rate",  convertRate(timer.getFifteenMinuteRate()), rateAttr);
    }

    private void reportMeter(List<Sample> samples, Timestamp timestamp, String name, Meter meter) {
        Map<String, String> attrs = Maps.newHashMap();
        attrs.put("rate_unit", getRateUnit());
        reportC(samples, timestamp, name, "count",     meter.getCount());
        reportG(samples, timestamp, name, "mean_rate", convertRate(meter.getMeanRate()), attrs);
        reportG(samples, timestamp, name, "m1_rate",   convertRate(meter.getOneMinuteRate()), attrs);
        reportG(samples, timestamp, name, "m5_rate",   convertRate(meter.getFiveMinuteRate()), attrs);
        reportG(samples, timestamp, name, "m15_rate",  convertRate(meter.getFifteenMinuteRate()), attrs);
    }

    private void reportHistogram(List<Sample> samples, Timestamp timestamp, String name, Histogram histogram) {
        final Snapshot snapshot = histogram.getSnapshot();

        reportC(samples, timestamp, name, "count",  histogram.getCount());
        reportG(samples, timestamp, name, "max",    snapshot.getMax());
        reportG(samples, timestamp, name, "mean",   snapshot.getMean());
        reportG(samples, timestamp, name, "min",    snapshot.getMin());
        reportG(samples, timestamp, name, "stddev", snapshot.getStdDev());
        reportG(samples, timestamp, name, "p50",    snapshot.getMedian());
        reportG(samples, timestamp, name, "p75",    snapshot.get75thPercentile());
        reportG(samples, timestamp, name, "p95",    snapshot.get95thPercentile());
        reportG(samples, timestamp, name, "p98",    snapshot.get98thPercentile());
        reportG(samples, timestamp, name, "p99",    snapshot.get99thPercentile());
        reportG(samples, timestamp, name, "p999",   snapshot.get999thPercentile());
    }

    private void reportCounter(List<Sample> samples, Timestamp timestamp, String name, Counter counter) {
        reportC(samples, timestamp, name, "count", counter.getCount());
    }

    private void reportGauge(List<Sample> samples, Timestamp timestamp, String name, Gauge<?> gauge) {
        Optional<Double> val = value(gauge.getValue());
        if (val.isPresent()) {
            reportG(samples, timestamp, name, "value", val.get());
        }
    }
    
    private void reportC(List<Sample> samples, Timestamp timestamp, String resource, String metricName, long count) {
        samples.add(new Sample(timestamp, this.name+"-"+resource, metricName, MetricType.COUNTER, counter(count)));
    }
    
    private void reportG(List<Sample> samples, Timestamp timestamp, String resource, String metricName, double val) {
        Sample s = new Sample(timestamp, this.name+"-"+resource, metricName, MetricType.GAUGE, gauge(val));
        samples.add(s);
    }
    
    private void reportG(List<Sample> samples, Timestamp timestamp, String resource, String metricName, double val, Map<String, String> attrs) {
        Sample s = new Sample(timestamp, this.name+"-"+resource, metricName, MetricType.GAUGE, gauge(val), attrs);
        samples.add(s);
    }
    
    private ValueType<?> counter(long count) {
        return ValueType.compose(count, MetricType.COUNTER);
    }
    
    private ValueType<?> gauge(double val) {
        return ValueType.compose(val, MetricType.GAUGE);
    }
    
    private Optional<Double> value(Object val) {
        return val instanceof Number ?
            Optional.of(((Number)val).doubleValue()) :
            Optional.<Double>absent();
    }

}
