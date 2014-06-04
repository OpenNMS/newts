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

import org.opennms.newts.persistence.cassandra.SchemaManager;

import net.sourceforge.argparse4j.inf.Namespace;

import com.yammer.dropwizard.cli.ConfiguredCommand;
import com.yammer.dropwizard.config.Bootstrap;

public class InitCommand extends ConfiguredCommand<NewtsConfig> {

    protected InitCommand() {
        super("init", "Perform one-time application initialization");
    }

    @Override
    protected void run(Bootstrap<NewtsConfig> bootstrap, Namespace namespace, NewtsConfig config) throws Exception {
        try (SchemaManager manager = new SchemaManager(config.getCassandraKeyspace(), config.getCassandraHost(), config.getCassandraPort())) {
            manager.create(true);
        }
    }

}
