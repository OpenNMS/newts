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


import java.util.Collection;

import javax.ws.rs.WebApplicationException;

import org.opennms.newts.api.Duration;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;


class Transform {

    private static final Function<SampleDTO, Sample> DTO_TO_SAMPLE;

    static {
        DTO_TO_SAMPLE = new Function<SampleDTO, Sample>() {

            @Override
            public Sample apply(SampleDTO input) {
                return new Sample(
                        Timestamp.fromEpochMillis(input.getTimestamp()),
                        input.getResource(),
                        input.getName(),
                        input.getType(),
                        ValueType.compose(input.getValue(), input.getType()),
                        input.getAttributes());
            }
        };
    }

    /**
     * Convert a {@link ResultDescriptorDTO} to {@link ResultDescriptor}.
     *
     * @param rDescriptorDTO
     *            the DTO to transform
     * @return the corresponding descriptor
     */
    static ResultDescriptor resultDescriptor(ResultDescriptorDTO rDescriptorDTO) {

        ResultDescriptor rDescriptor = new ResultDescriptor(Duration.seconds(rDescriptorDTO.getInterval()));

        for (ResultDescriptorDTO.Datasource ds : rDescriptorDTO.getDatasources()) {
            if (ds.getHeartbeat() != null) {
                rDescriptor.datasource(ds.getLabel(), ds.getSource(), Duration.seconds(ds.getHeartbeat()), ds.getFunction());
            }
            else {
                rDescriptor.datasource(ds.getLabel(), ds.getSource(), ds.getFunction());
            }
        }
        
        for (ResultDescriptorDTO.Expression expr : rDescriptorDTO.getExpressions()) {
            rDescriptor.expression(expr.getLabel(), expr.getExpression());
        }

        rDescriptor.export(rDescriptorDTO.getExports());

        return rDescriptor;
    }

    /**
     * Convert {@link SampleDTO}s to {@link Sample}s.
     *
     * @param samples
     *            samples to convert
     * @return converted samples
     */
    static Collection<Sample> sampleDTOs(Collection<SampleDTO> samples) {
        return Collections2.transform(samples, DTO_TO_SAMPLE);
    }

    /**
     * Parse an {@link Optional} String to {@link Timestamp}.
     * <p>
     * Parsing is done with {@link TimestampParam} which will raise the appropriate
     * {@link WebApplicationException} if a validation error occurs.
     * </p>
     *
     * @param value
     *            the string to parse
     * @return the timestamp wrapped in {@link Optional}
     */
    static Optional<Timestamp> timestampFromString(Optional<String> value) {
        return value.isPresent() ? Optional.of(new TimestampParam(value.get()).get()) : Optional.<Timestamp>absent();
    }

}
