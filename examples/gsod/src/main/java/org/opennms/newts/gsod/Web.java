package org.opennms.newts.gsod;


import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

import java.util.Map;

import spark.Request;
import spark.Response;
import spark.template.velocity.VelocityRoute;

import com.google.common.collect.Maps;


public class Web {

    private static final Map<String, String> STATIONS = Maps.newHashMap();

    static {
        STATIONS.put("ksat", "722530");         // San Antonio
        STATIONS.put("kdal", "722585");         // Dallas
        STATIONS.put("kelp", "722700");         // El Paso
        STATIONS.put("kiah", "722430");         // Houston
        STATIONS.put("kaus", "722545");         // Austin
        STATIONS.put("klbb", "722670");         // Lubbock
    }

    public static void main(String... args) {

        staticFileLocation("/static");

        get(new VelocityRoute("/stations/:stationName") {

            @Override
            public Object handle(Request request, Response response) {

                String stationName = request.params(":stationName");
                String id = STATIONS.get(stationName);

                if (id == null) {
                    halt(404, "No such station");
                }

                Map<String, String> model = Maps.newHashMap();
                model.put("stationName", stationName.toUpperCase());
                model.put("id", id);

                return modelAndView(model, "station.wm");
            }
        });

    }

}
