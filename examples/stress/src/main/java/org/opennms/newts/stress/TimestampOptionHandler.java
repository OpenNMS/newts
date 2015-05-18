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

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.api.Timestamp;

public class TimestampOptionHandler extends OneArgumentOptionHandler<Timestamp> {

    public TimestampOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Timestamp> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Timestamp parse(String argument) throws NumberFormatException, CmdLineException {
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        DateTime dateTime = parser.parseDateTime(argument);
        return Timestamp.fromEpochMillis(dateTime.getMillis());
    }

    @Override
    public String getDefaultMetaVariable() {
        return "<timestamp>";
    }

}
