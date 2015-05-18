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
package org.opennms.newts.stress;


import org.kohsuke.args4j.Option;
import org.opennms.newts.api.Duration;


class SelectConfig extends Config {

    private Duration m_resolution = Duration.seconds(3600);
    private Duration m_selectLength = Duration.seconds(86400);

    // XXX: selectLength should be validated; selectLength should be greater than resolution
    @Option(name = "-sl", aliases = "--select-length", metaVar = "<length>", usage = "Length of select in seconds.")
    void setSelectLength(Duration selectLength) {
        m_selectLength = selectLength;
    }

    // XXX: resolution should be validated; resolution should be greater than interval
    @Option(name = "-R", aliases = "--resolution", metaVar = "<resolution>", usage = "Aggregate resolution in seconds.")
    void setResolution(Duration resolution) {
        m_resolution = resolution;
    }

    Duration getResolution() {
        return m_resolution;
    }

    Duration getSelectLength() {
        return m_selectLength;
    }

}
