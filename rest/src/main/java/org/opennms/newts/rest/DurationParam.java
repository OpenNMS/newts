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

import org.opennms.newts.api.Duration;

import io.dropwizard.jersey.params.AbstractParam;


/**
 * JAX-RS parameter that encapsulates creation of {@link Duration} instances from a string
 * specifier. Non-parseable values will result in a {@code 400 Bad Request} response.
 *
 * @author eevans
 */
public class DurationParam extends AbstractParam<Duration> {

    protected DurationParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return String.format("Unable to parse '%s' as resolution", input);
    }

    @Override
    protected Duration parse(String input) throws Exception {
        if (input.matches("^[\\d]+$")) {
            return Duration.seconds(Integer.valueOf(input));
        }

        return Duration.parse(input);
    }

}
