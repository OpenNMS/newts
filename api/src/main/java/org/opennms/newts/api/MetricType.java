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
package org.opennms.newts.api;


public enum MetricType {

    COUNTER(1), ABSOLUTE(2), DERIVE(3), GAUGE(4);

    private byte m_code;

    private MetricType(int code) {
        m_code = (byte) code;
    }

    public byte getCode() {
        return m_code;
    }

    public static MetricType fromCode(byte code) {
        for (MetricType type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("No such type for 0x%x", code));
    }

}
