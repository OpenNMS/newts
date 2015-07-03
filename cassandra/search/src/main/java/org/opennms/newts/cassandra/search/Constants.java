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
package org.opennms.newts.cassandra.search;


class Constants {

    static String DEFAULT_TERM_FIELD = "_all";

    // Used for hierarchical indexing
    static final String PARENT_TERM_FIELD = "_parent";
    static final String TOP_LEVEL_PARENT_TERM_VALUE = "_root";

    static class Schema {
        // Terms storage
        static final String T_TERMS = "terms";
        static final String C_TERMS_CONTEXT = "context";
        static final String C_TERMS_FIELD = "field";
        static final String C_TERMS_VALUE = "value";
        static final String C_TERMS_RESOURCE = "resource";

        // Attributes
        static final String T_ATTRS = "resource_attributes";
        static final String C_ATTRS_CONTEXT = "context";
        static final String C_ATTRS_RESOURCE = "resource";
        static final String C_ATTRS_ATTR = "attribute";
        static final String C_ATTRS_VALUE = "value";

        // Metrics
        static final String T_METRICS = "resource_metrics";
        static final String C_METRICS_CONTEXT = "context";
        static final String C_METRICS_RESOURCE = "resource";
        static final String C_METRICS_NAME = "metric_name";
    }
}
