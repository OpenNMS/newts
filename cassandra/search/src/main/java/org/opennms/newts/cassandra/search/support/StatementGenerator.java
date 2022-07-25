/*
 * Copyright 2016, The OpenNMS Group
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
package org.opennms.newts.cassandra.search.support;

import org.opennms.newts.api.Context;

import com.datastax.oss.driver.api.core.cql.BatchableStatement;

/**
 * Used to group and de-duplicate statements before they are generated and executed.
 *
 * @author jwhite
 */
public interface StatementGenerator {

    /**
     * Returns a key which can be used to group statements into batches, or null
     * if the statement should never be be batched.
     * 
     * @return key or null
     */
    String getKey();

    /**
     * Returns the context with this this statement is associated.
     *
     * @return context
     */
    Context getContext();

    /**
     * Generates the statement.
     *
     * @return statement
     */
    BatchableStatement<?> toStatement();
}