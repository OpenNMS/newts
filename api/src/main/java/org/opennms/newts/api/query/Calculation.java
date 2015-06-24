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
package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;


public class Calculation implements Serializable {
    private static final long serialVersionUID = -1273272566473340413L;

    private final String m_label;
    private final CalculationFunction m_calculationFunction;
    private final String[] m_args;

    public Calculation(String label, CalculationFunction calculationFunction, String... args) {
        m_label = checkNotNull(label, "label argument");
        m_calculationFunction = checkNotNull(calculationFunction, "calculation function argument");

        checkArgument(args.length > 0, "one or more function arguments are required");
        m_args = args;

    }

    public String getLabel() {
        return m_label;
    }

    public CalculationFunction getCalculationFunction() {
        return m_calculationFunction;
    }

    public String[] getArgs() {
        return m_args;
    }

    @Override
    public String toString() {
        return String.format(
                "%s[%s, function=%s, args=%s]",
                getClass().getSimpleName(),
                getLabel(),
                getCalculationFunction(),
                Arrays.asList(getArgs()));
    }

}
