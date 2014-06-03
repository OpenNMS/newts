package org.opennms.newts.persistence.leveldb;


import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.opennms.newts.api.SampleRepository;

import com.codahale.metrics.MetricRegistry;


public class AbstractLeveldbTestCase {
    
    @Rule public TestName name = new TestName();

    public static final String CASSANDRA_CONFIG  = "cassandra.yaml";
    public static final String KEY_SEPARATOR     = "||";
    public static final File LEVEL_DB_BASENAME     = new File("target"+ File.separatorChar + "leveldb-test");

    protected SampleRepository m_repository;

    @Before
    public void setUp() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        File testDir = new File(LEVEL_DB_BASENAME, name.getMethodName());
        testDir.mkdirs();
        testDir.deleteOnExit();
        m_repository = new LeveldbSampleRepository(testDir, registry);
    }

    @After
    public void tearDown() throws Exception {
        
    }

    public SampleRepository getRepository() {
        return m_repository;
    }

}
