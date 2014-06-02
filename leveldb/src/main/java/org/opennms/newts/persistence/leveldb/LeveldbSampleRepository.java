package org.opennms.newts.persistence.leveldb;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static org.fusesource.leveldbjni.JniDBFactory.asString;
import static org.fusesource.leveldbjni.JniDBFactory.bytes;
import static org.fusesource.leveldbjni.JniDBFactory.factory;
import static com.codahale.metrics.MetricRegistry.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Named;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.MetricType;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.SampleRepository;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.ResultDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.inject.Inject;


public class LeveldbSampleRepository implements SampleRepository {
    private static final Logger LOG = LoggerFactory.getLogger(LeveldbSampleRepository.class);
    private static final String DEFAULT_SEPARATOR = "##";
    private static final String SAMPLES_PREFIX = "S";

    private final File m_databaseDir;
    private final Joiner m_joiner;
    private final Splitter m_splitter;
    private final MetricRegistry m_registry;
    private final Meter m_sampleRate;
    private final Meter m_stmtRate;
    private final Timer m_insertTime;
    private final Timer m_queryTime;
    private final Histogram m_queryResults;
    private final DB m_db;

    @Inject
    public LeveldbSampleRepository(
            @Named("leveldb.dir")File databaseDir, 
            @Named("samples.leveldb.separator")String separator, 
            MetricRegistry registry) throws IOException {
        m_databaseDir = databaseDir;
        m_joiner = Joiner.on(separator);
        m_splitter = Splitter.on(separator);
        m_registry = registry;
        m_stmtRate = registry.meter(name("leveldb", "sample.inserts.stmts"));
        m_sampleRate = registry.meter(name("leveldb", "sample.inserts.samples"));
        m_insertTime = registry.timer(name("leveldb", "sample.insert.time"));
        m_queryTime = registry.timer(name("leveldb", "query.time"));
        m_queryResults = registry.histogram(name("leveldb", "query.result-count"));
                
        Options options = new Options();
        options.createIfMissing(true);
        m_db = factory.open(databaseDir, options);

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                LeveldbSampleRepository.this.shutdown();
            }

        });


    }
    
    public void shutdown() {
        try {
            m_db.close();
            LOG.info("Sucessfully closed LevelDB database at {}", m_databaseDir);
        } catch (IOException e) {
            LOG.error("Failed to close LevelDB database at {}", m_databaseDir);
        }
    }
    
    public LeveldbSampleRepository(File databaseDir, MetricRegistry registry) throws IOException {
        this(databaseDir, DEFAULT_SEPARATOR, registry);
    }

    @Override
    public Results<Measurement> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end, ResultDescriptor descriptor, Duration resolution) {
        
        validateSelect(start, end);

        try (Context timer = m_queryTime.time() ) {

            Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
            Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

            LOG.debug("Querying database for resource {}, from {} to {}", resource, lower.minus(resolution), upper);

            Results<Sample> samples = leveldbSelect(resource, lower.minus(resolution), upper, descriptor.getSourceNames());
            Results<Measurement> results = new ResultProcessor(resource, lower, upper, descriptor, resolution).process(samples.iterator());

            int size = samples.getRows().size();
            m_queryResults.update(size);
            LOG.debug("{} results returned from database", size);

            return results;

        }

    }

    @Override
    public Results<Sample> select(String resource, Optional<Timestamp> start, Optional<Timestamp> end) {
        
        validateSelect(start, end);

        try (Context timer = m_queryTime.time() ) {


            Timestamp upper = end.isPresent() ? end.get() : Timestamp.now();
            Timestamp lower = start.isPresent() ? start.get() : upper.minus(Duration.seconds(86400));

            LOG.debug("Querying database for resource {}, from {} to {}", resource, lower, upper);


            Results<Sample> samples = leveldbSelect(resource, lower, upper);

            int size = samples.getRows().size();
            m_queryResults.update(size);
            LOG.debug("{} results returned from database", size);

            return samples;

        }
    }
    
    private byte[] searchKey(String resource, Timestamp ts) {
        return bytes(searchKeyString(resource, ts));
    }

    private String searchKeyString(String resource, Timestamp ts) {
        return m_joiner.join(
            SAMPLES_PREFIX,
            resource,
            Strings.padStart(Long.toString(ts.asMillis(), 10), 13, '0')
        );
    }
    
    private byte[] sampleKey(Sample m) {
        return bytes(sampleKeyString(m));
    }

    private String sampleKeyString(Sample m) {
        return m_joiner.join(
           SAMPLES_PREFIX,                                   
           m.getResource(),
           Strings.padStart(Long.toString(m.getTimestamp().asMillis(), 10), 13, '0'),
           m.getName(),
           m.getType()
        );
    }
    
    private byte[] sampleValue(Sample m) {
        return ValueType.decompose(m.getValue()).array();
    }

    @Override
    public void insert(Collection<Sample> samples) {
        m_stmtRate.mark();
        for (Sample m : samples) {
            //System.err.printf("put(%s, %s)\n", sampleKeyString(m), m.getValue());
            try ( Context time = m_insertTime.time() ) {
                
                m_db.put(sampleKey(m), sampleValue(m));

            }
            m_sampleRate.mark();
        }
    }
    
    private Sample toSample(byte[] key, byte[] value) {
        checkNotNull(key, "key may not be null");
        checkNotNull(value, "value may not be null");
        
        // parse the key into its parts
        List<String> sampleInfo = m_splitter.splitToList(asString(key));
        
        // reconstruct the Sample data from the parts of the key
        String resource = sampleInfo.get(1);
        Timestamp ts = Timestamp.fromEpochMillis(Long.valueOf(sampleInfo.get(2)));
        String name = sampleInfo.get(3);
        MetricType type = MetricType.valueOf(MetricType.class, sampleInfo.get(4));
        
        // parse the value into a ValueType
        ValueType<?> val = ValueType.compose(ByteBuffer.wrap(value), type);

        // return a filled in sample
        return new Sample(ts, resource, name, type, val);
        
    }
    
    private Results<Sample> leveldbSelect(String resource, Timestamp start, Timestamp end) {
        return leveldbSelect(resource, start, end, Predicates.<Sample> alwaysTrue());
    }


    private Results<Sample> leveldbSelect(String resource, Timestamp start, Timestamp end, final Set<String> metrics) {
        return leveldbSelect(resource, start, end, new Predicate<Sample>() {

            @Override
            public boolean apply(Sample input) {
                return metrics.contains(input.getName());
            }
            
        });
    }
        
    private Results<Sample> leveldbSelect(String resource, Timestamp start, Timestamp end, Predicate<Sample> filter) {
        DBIterator iterator = m_db.iterator();
        try {
            Results<Sample> results = new Results<Sample>();
            //System.err.println("Searching with start key: " + searchKeyString(resource, start));
            iterator.seek(searchKey(resource, start));
            while(iterator.hasNext()) {
                Entry<byte[], byte[]> entry = iterator.next();
                Sample s = toSample(entry.getKey(), entry.getValue());
                //System.err.printf("Found a sample: %s\n", s);
                String r = s.getResource();
                Timestamp ts = s.getTimestamp();
                // if we get a value for a new resource or its outside of the timerange then we are finished
                if (!(resource.equals(r)) || !(ts.gte(start) && ts.lte(end))) {
                    break;
                }
                if (filter.apply(s)) {
                    results.addElement(s);
                }
            }
            return results;
        } finally {
            // Make sure you close the iterator to avoid resource leaks.
            try {
                iterator.close();
            } catch(Exception e) {
                propagate(e);
            }
        }
        

    }

    private void validateSelect(Optional<Timestamp> start, Optional<Timestamp> end) {
        if ((start.isPresent() && end.isPresent()) && start.get().gt(end.get())) {
            throw new IllegalArgumentException("start time must be less than end time");
        }
    }

}
