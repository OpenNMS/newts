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

import static org.junit.Assert.*;

import org.junit.Test;
import org.opennms.newts.gsod.MergeSort.GSODLine;

public class GSODLineTest {

    @Test
    public void test() {
        String line = "722430 12960  19880101    51.0 24    47.5 24  1024.8 24  1020.9 24    7.8 24    9.6 24   12.0   19.0    64.9    43.0   0.19G 999.9  110000";
      
        GSODLine gsodLine = new GSODLine(line);
        
        assertEquals(722430, gsodLine.getStation());
        assertEquals(12960, gsodLine.getWBAN());
        assertEquals(19880101, gsodLine.getDate());
    }

}
