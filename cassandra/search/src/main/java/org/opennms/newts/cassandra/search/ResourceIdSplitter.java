/*
 * Copyright 2015, The OpenNMS Group
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

import java.util.List;

/**
 * Used to split a resource id into elements and join these back
 * into a resource id.
 *
 * @author jwhite
 */
public interface ResourceIdSplitter {

    public static char SEPARATOR = ':';

    public List<String> splitIdIntoElements(String id);

    public String joinElementsToId(List<String> elements);
}
