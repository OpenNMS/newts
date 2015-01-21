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
package org.opennms.newts.rest;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;

import java.util.ServiceLoader;

import net.sourceforge.argparse4j.inf.Namespace;

import org.opennms.newts.cassandra.Schema;
import org.opennms.newts.cassandra.SchemaManager;

public class InitCommand extends ConfiguredCommand<NewtsConfig> {

    private static ServiceLoader<Schema> s_schemas = ServiceLoader.load(Schema.class);

    protected InitCommand() {
        super("init", "Perform one-time application initialization");
    }

    @Override
    protected void run(Bootstrap<NewtsConfig> bootstrap, Namespace namespace, NewtsConfig config) throws Exception {
        try (SchemaManager m = new SchemaManager(config.getCassandraKeyspace(), config.getCassandraHost(), config.getCassandraPort())) {
            for (Schema s : s_schemas) {
                m.create(s, true);
            }
        }
    }

}
