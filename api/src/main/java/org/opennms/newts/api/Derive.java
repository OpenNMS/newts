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


import com.google.common.primitives.UnsignedLong;


public class Derive extends Counter {

    private static final long serialVersionUID = 1L;

    public Derive(UnsignedLong value) {
        super(value);
    }

    @Override
    public ValueType<UnsignedLong> delta(Number value) {
        return new Derive(getValue().minus(toUnsignedLong(value)));
    }

    @Override
    public MetricType getType() {
        return MetricType.DERIVE;
    }

}
