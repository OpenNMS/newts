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
package org.opennms.newts.rest;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opennms.newts.api.Timestamp;

import io.dropwizard.jersey.params.AbstractParam;


/**
 * JAX-RS parameter that encapsulates creation of {@link Timestamp} instances from ISO 8601
 * timestamps, or seconds since the Unix epoch. Non-parseable values will result in a
 * {@code 400 Bad Request} response.
 *
 * @author eevans
 */
public class TimestampParam extends AbstractParam<Timestamp> {

    private static final DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();

    public TimestampParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return String.format("Unable to parse '%s' as date-time", input);
    }

    @Override
    protected Timestamp parse(String input) throws Exception {

        if (input.matches("^[\\d]+$")) {
            return Timestamp.fromEpochSeconds(Integer.valueOf(input));
        }

        DateTime dateTime = parser.parseDateTime(input);

        return Timestamp.fromEpochMillis(dateTime.getMillis());
    }

}
