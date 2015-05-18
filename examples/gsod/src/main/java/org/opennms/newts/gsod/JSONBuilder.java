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
package org.opennms.newts.gsod;

/**
 * Created by brozow on 4/23/14.
 */
public class JSONBuilder {
    StringBuilder buf = new StringBuilder();
    boolean object = false;
    boolean attr = false;


    JSONBuilder() {
        buf.append("\n[\n");
    }

    public JSONBuilder newObject() {
        if (object) buf.append("\n },\n");
        buf.append(" {\n");
        object = true;
        attr = false;
        return this;
    }

    public JSONBuilder attr(String name, long val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, int val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, double val) {
        if (Double.isNaN(val)) return this;
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, String val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": \"").append(val).append("\"");
        attr = true;
        return this;
    }

    public String toString() {
        if (object) buf.append("\n }\n");
        buf.append("]\n");
        object = false;
        attr = false;
        return buf.toString();
    }
}
