package org.opennms.newts.rest;

import org.opennms.newts.persistence.cassandra.SchemaManager;

import net.sourceforge.argparse4j.inf.Namespace;

import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;

public class InitCommand extends ConfiguredCommand<Config> {

    protected InitCommand() {
        super("init", "Perform one-time application initialization");
    }

    @Override
    protected void run(Bootstrap<Config> bootstrap, Namespace namespace, Config config) throws Exception {
        try (SchemaManager manager = new SchemaManager(config.getCassandraHost(), config.getCassandraPort())) {
            manager.create(true);
        }
    }

}
