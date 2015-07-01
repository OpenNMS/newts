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
package org.opennms.newts.persistence.cassandra;


public class SchemaConstants {
    public static final String T_SAMPLES = "samples";

    public static final String F_CONTEXT = "context";
    public static final String F_PARTITION = "partition";
    public static final String F_RESOURCE = "resource";
    public static final String F_COLLECTED = "collected_at";
    public static final String F_METRIC_NAME = "metric_name";
    public static final String F_VALUE = "value";
    public static final String F_ATTRIBUTES = "attributes";
}
