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


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.api.Resource;


public class ResourceOptionHandler extends OneArgumentOptionHandler<Resource> {

    public ResourceOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Resource> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Resource parse(String argument) throws NumberFormatException, CmdLineException {
        return new Resource(argument);
    }

    @Override
    public String getDefaultMetaVariable() {
        return "<resource>";
    }

}
