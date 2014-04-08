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
import org.opennms.newts.api.Measurement;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Sample;
import org.opennms.newts.api.Timestamp;
import org.opennms.newts.api.ValueType;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.query.ResultDescriptor;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;


class Transform {

    private static final Function<Measurement, MeasurementDTO> MEASUREMENT_TO_DTO;
    private static final Function<SampleDTO, Sample> DTO_TO_SAMPLE;
    private static final Function<Sample, SampleDTO> SAMPLE_TO_DTO;

    static {
        MEASUREMENT_TO_DTO = new Function<Measurement, MeasurementDTO>() {

            @Override
            public MeasurementDTO apply(Measurement input) {
                return new MeasurementDTO(
                        input.getTimestamp().asSeconds(),
                        input.getResource(),
                        input.getName(),
                        input.getValue());
            }
        };

        DTO_TO_SAMPLE = new Function<SampleDTO, Sample>() {

            @Override
            public Sample apply(SampleDTO input) {
                return new Sample(
                        Timestamp.fromEpochSeconds(input.getTimestamp()),
                        input.getResource(),
                        input.getName(),
                        input.getType(),
                        ValueType.compose(input.getValue(), input.getType()));
            }
        };

        SAMPLE_TO_DTO = new Function<Sample, SampleDTO>() {

            @Override
            public SampleDTO apply(Sample input) {
                return new SampleDTO(
                        input.getTimestamp().asSeconds(),
                        input.getResource(),
                        input.getName(),
                        input.getType(),
                        input.getValue());
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

        rDescriptor.export(rDescriptorDTO.getExports());

        return rDescriptor;
    }

    /**
     * Convert a {@link Measurement} result set to the representation structure.
     *
     * @param measurements
     *            measurements to be converted
     * @return converted measurements
     */
    @Deprecated
    static Collection<Collection<MeasurementDTO>> measurements(Results<Measurement> measurements) {
        return Collections2.transform(measurements.getRows(), new Function<Row<Measurement>, Collection<MeasurementDTO>>() {

            @Override
            public Collection<MeasurementDTO> apply(Row<Measurement> input) {
                return Collections2.transform(input.getElements(), MEASUREMENT_TO_DTO);
            }
        });
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
     * Convert a {@link Sample} result set to the representation structure.
     *
     * @param samples
     *            samples to be converted
     * @return converted samples
     */
    @Deprecated
    static Collection<Collection<SampleDTO>> samples(Results<Sample> samples) {
        return Collections2.transform(samples.getRows(), new Function<Row<Sample>, Collection<SampleDTO>>() {

            @Override
            public Collection<SampleDTO> apply(Row<Sample> input) {
                return Collections2.transform(input.getElements(), SAMPLE_TO_DTO);
            }
        });
    }

    /**
     * Convert an {@link Optional} Integer (seconds since the Unix epoch) to an {@link Optional}
     * {@link Timestamp}.
     *
     * @param value
     *            the value to convert
     * @return the converted value
     */
    @Deprecated
    static Optional<Timestamp> fromOptionalSeconds(Optional<Integer> value) {
        return value.isPresent() ? Optional.of(Timestamp.fromEpochSeconds(value.get())) : Optional.<Timestamp> absent();
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
