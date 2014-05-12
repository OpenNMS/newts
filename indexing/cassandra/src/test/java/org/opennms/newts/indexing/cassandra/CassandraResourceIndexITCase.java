package org.opennms.newts.indexing.cassandra;


import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.opennms.newts.api.indexing.ResourcePath;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;


public class CassandraResourceIndexITCase extends AbstractCassandraTestCase {

    @Test
    public void testResourceTreeFromRoot() {

        Multimap<String, String> metrics = HashMultimap.create();

        metrics.put(join("localhost", "chassis", "temps"), "temp0");
        metrics.put(join("localhost", "network", "eth0"), "inOctets");

        getResourceIndex().index(metrics);

        ResourcePath root = getResourceIndex().search();

        assertThat(root.getName(), is(equalTo(CassandraResourceIndex.ROOT_KEY)));
        assertThat(root.getChildren().size(), equalTo(1));
        assertThat(root.getMetrics().size(), equalTo(0));

        Optional<ResourcePath> localhost = findByName(root.getChildren(), "localhost");

        assertThat("missing child node", localhost.isPresent(), is(true));
        assertThat(localhost.get().getChildren().size(), equalTo(2));
        assertThat("missing child node", findByName(localhost.get().getChildren(), "chassis").isPresent(), is(true));
        assertThat("missing child node", findByName(localhost.get().getChildren(), "network").isPresent(), is(true));
        
    }

    @Test
    public void testResourceTree() {

        Multimap<String, String> metrics = HashMultimap.create();

        metrics.put(join("localhost", "chassis", "temps"), "temp0");
        metrics.put(join("localhost", "chassis", "temps"), "temp1");
        metrics.put(join("localhost", "chassis", "temps"), "temp2");
        metrics.put(join("localhost", "network", "eth0"), "inOctets");
        metrics.put(join("localhost", "network", "eth0"), "outOctets");
        metrics.put(join("localhost", "network", "eth1"), "inOctets");
        metrics.put(join("localhost", "network", "eth1"), "outOctets");

        getResourceIndex().index(metrics);

        ResourcePath root = getResourceIndex().search("localhost");

        assertThat(root.getChildren().size(), is(2));
        assertThat(root.getName(), is(equalTo("localhost")));
        assertThat(root.getMetrics().size(), is(0));

        Optional<ResourcePath> chassis = findByName(root.getChildren(), "chassis");
        Optional<ResourcePath> network = findByName(root.getChildren(), "network");

        assertThat("missing child node", chassis.isPresent(), is(true));
        assertThat(chassis.get().getChildren().size(), equalTo(1));
        assertThat(chassis.get().getName(), is(equalTo("chassis")));
        assertThat(chassis.get().getMetrics().size(), is(0));

        assertThat("missing child node", network.isPresent(), is(true));
        assertThat(network.get().getChildren().size(), is(2));
        assertThat(network.get().getName(), is(equalTo("network")));
        assertThat(network.get().getMetrics().size(), is(0));

        Optional<ResourcePath> temps = findByName(chassis.get().getChildren(), "temps");
        Optional<ResourcePath> eth0 = findByName(network.get().getChildren(), "eth0");
        Optional<ResourcePath> eth1 = findByName(network.get().getChildren(), "eth1");
        
        assertThat("missing child node", temps.isPresent(), is(true));
        assertThat(temps.get().getChildren().size(), equalTo(0));
        assertThat(temps.get().getName(), is(equalTo("temps")));
        assertThat(temps.get().getMetrics().size(), equalTo(3));
        assertThat(temps.get().getMetrics().contains("temp0"), is(true));
        assertThat(temps.get().getMetrics().contains("temp1"), is(true));
        assertThat(temps.get().getMetrics().contains("temp2"), is(true));
        
        assertThat("missing child node", eth0.isPresent(), is(true));
        assertThat(eth0.get().getChildren().size(), equalTo(0));
        assertThat(eth0.get().getName(), is(equalTo("eth0")));
        assertThat(eth0.get().getMetrics().size(), equalTo(2));
        assertThat(eth0.get().getMetrics().contains("inOctets"), is(true));
        assertThat(eth0.get().getMetrics().contains("outOctets"), is(true));
        
        assertThat("missing child node", eth1.isPresent(), is(true));
        assertThat(eth1.get().getChildren().size(), equalTo(0));
        assertThat(eth1.get().getName(), is(equalTo("eth1")));
        assertThat(eth1.get().getMetrics().size(), equalTo(2));
        assertThat(eth1.get().getMetrics().contains("inOctets"), is(true));
        assertThat(eth1.get().getMetrics().contains("outOctets"), is(true));

    }

    @Test
    public void testMetricNames() {

        Multimap<String, String> metrics = HashMultimap.create();

        metrics.put(join("localhost", "chassis", "temps"), "temp0");
        metrics.put(join("localhost", "chassis", "temps"), "temp1");
        metrics.put(join("localhost", "chassis", "temps"), "temp2");

        getResourceIndex().index(metrics);

        Collection<String> r = getResourceIndex().getMetrics(join("localhost", "chassis", "temps"));

        assertThat("wrong metric name count", r.size(), is(equalTo(3)));

        List<String> metricNames = Lists.newArrayList(r.iterator());
        assertThat(metricNames.contains("temp0"), is(true));
        assertThat(metricNames.contains("temp1"), is(true));
        assertThat(metricNames.contains("temp2"), is(true));

    }

    @Test
    public void testMixedMetricsTree() {

        Multimap<String, String> metrics = HashMultimap.create();

        metrics.put(join("localhost", "network", "eth0"), "inOctets");
        metrics.put(join("localhost", "network", "eth0"), "outOctets");
        metrics.put(join("localhost", "network", "eth1"), "inOctets");
        metrics.put(join("localhost", "network", "eth1"), "outOctets");
        metrics.put(join("localhost", "network"), "allInOctets");
        metrics.put(join("localhost", "network"), "allOutOctets");

        getResourceIndex().index(metrics);

        ResourcePath network = getResourceIndex().search("localhost", "network");

        assertThat(network.getName(), is(equalTo("network")));
        assertThat(network.getParent().isPresent(), is(true));
        assertThat(network.getParent().get().getName(), is(equalTo("localhost")));

        Optional<ResourcePath> eth0 = findByName(network.getChildren(), "eth0");
        Optional<ResourcePath> eth1 = findByName(network.getChildren(), "eth1");
        assertThat(eth0.isPresent(), is(true));
        assertThat(eth1.isPresent(), is(true));

        assertThat(network.getMetrics().size(), equalTo(2));
        assertThat(network.getMetrics().contains("allInOctets"), is(true));
        assertThat(network.getMetrics().contains("allOutOctets"), is(true));

    }

    private String join(String... args) {
        return Joiner.on(CassandraResourceIndex.DELIMITER).join(args);
    }

    static class ResourcePathPredicate implements Predicate<ResourcePath> {
        private final String m_name;

        ResourcePathPredicate(String name) {
            m_name = name;
        }

        @Override
        public boolean apply(ResourcePath input) {
            return input.getName().equals(m_name);
        }

    }

    private Optional<ResourcePath> findByName(Iterable<ResourcePath> paths, String name) {
        return Iterables.tryFind(paths, new ResourcePathPredicate(name));
    }

}
