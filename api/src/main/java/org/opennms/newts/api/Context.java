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

import java.io.Serializable;
import com.google.common.base.Objects;

public class Context implements Serializable {
    private static final long serialVersionUID = 6042152517787919500L;

    public static final Context DEFAULT_CONTEXT = new Context("G");

    private final String m_id;

    public Context(String id) {
        m_id = id;
    }

    public String getId() {
        return m_id;
    }

    @Override
    public String toString() {
        return "Context[m_id=" + m_id + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Context other = (Context) obj;
        return Objects.equal(this.m_id, other.m_id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.m_id);
    }
}
