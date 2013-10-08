package org.opennms.newts.rest;


import static spark.Spark.get;
import static spark.Spark.post;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.type.TypeReference;
import org.opennms.newts.api.MeasurementRepository;
import org.opennms.newts.api.Results;
import org.opennms.newts.api.Results.Row;
import org.opennms.newts.api.Timestamp;

import spark.Request;
import spark.Response;
import spark.Route;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.inject.Guice;
import com.google.inject.Injector;


public class Server {

    private Function<Results.Row, Collection<Measurement>> m_rowFunc = new Function<Results.Row, Collection<Measurement>>() {

        @Override
        public Collection<Measurement> apply(Row input) {
            return Collections2.transform(input.getMeasurements(), m_toMeasurementDTO);
        }
    };

    private Function<org.opennms.newts.api.Measurement, Measurement> m_toMeasurementDTO = new Function<org.opennms.newts.api.Measurement, Measurement>() {

        @Override
        public Measurement apply(org.opennms.newts.api.Measurement input) {
            Measurement output = new Measurement();
            Metric metric = new Metric();

            output.setResource(input.getResource());
            output.setTimestamp(input.getTimestamp().asMillis());
            output.setValue(input.getValue());

            metric.setName(input.getMetric().getName());
            metric.setType(input.getMetric().getType());
            output.setMetric(metric);

            return output;
        }
    };

    private Function<Measurement, org.opennms.newts.api.Measurement> m_fromMeasurementDTO = new Function<Measurement, org.opennms.newts.api.Measurement>() {

        @Override
        public org.opennms.newts.api.Measurement apply(Measurement m) {
            org.opennms.newts.api.Metric metric = new org.opennms.newts.api.Metric(
                    m.getMetric().getName(),
                    m.getMetric().getType());
            return new org.opennms.newts.api.Measurement(
                    new Timestamp(m.getTimestamp()),
                    m.getResource(),
                    metric,
                    m.getValue());
        }
    };

    private final MeasurementRepository m_repository;

    @Inject
    public Server(final MeasurementRepository repository) {
        m_repository = repository;
        initialize();
    }

    private void initialize() {

        post(new Route("/") {

            @Override
            public Object handle(Request request, Response response) {

                ObjectMapper mapper = new ObjectMapper();
                ObjectReader reader = mapper.reader(new TypeReference<List<Measurement>>() {});
                Collection<Measurement> measurements = null;

                try {
                    measurements = reader.readValue(request.body());
                }
                catch (IOException e) {
                    halt(400, String.format("Unable to parse request body as JSON (reason: %s) ", e.getMessage()));
                }

                m_repository.insert(Collections2.transform(measurements, m_fromMeasurementDTO));

                return "";
            }
        });

        get(new JsonTransformerRoute<Object>("/:resource") {

            @Override
            public Object handle(Request request, Response response) {

                String resource = request.params(":resource");
                String startParam = request.queryParams("start");
                String endParam = request.queryParams("end");

                Timestamp start = null, end = null;

                if (startParam != null) {
                    try {
                        start = new Timestamp(Integer.parseInt(startParam), TimeUnit.MILLISECONDS);
                    }
                    catch (NumberFormatException e) {
                        halt(400, "Invalid start parameter");
                    }
                }

                if (endParam != null) {
                    try {
                        end = new Timestamp(Integer.parseInt(endParam), TimeUnit.MILLISECONDS);
                    }
                    catch (NumberFormatException e) {
                        halt(400, "Invalid end parameter");
                    }
                }

                Results select = m_repository.select(resource, start, end);
                
                return Collections2.transform(select.getRows(), m_rowFunc);
            }
        });

    }

    public static void main(String... args) {

        Injector injector = Guice.createInjector(new Config());
        injector.getInstance(Server.class);

    }

}
