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


import static spark.Spark.get;
import static spark.Spark.staticFileLocation;

import java.util.Map;

import spark.Request;
import spark.Response;
import spark.template.velocity.VelocityRoute;

import com.google.common.collect.Maps;


public class Web {

    private static final Map<String, String> STATION_IDS = Maps.newHashMap();
    private static final Map<String, String> STATION_NAMES = Maps.newTreeMap();

    static {
        STATION_IDS.put("ksat", "722530");         // San Antonio
        STATION_IDS.put("kdal", "722585");         // Dallas
        STATION_IDS.put("kelp", "722700");         // El Paso
        STATION_IDS.put("kiah", "722430");         // Houston
        STATION_IDS.put("kaus", "722545");         // Austin
        STATION_IDS.put("klbb", "722670");         // Lubbock
        
        STATION_NAMES.put("ksat", "San Antonio");
        STATION_NAMES.put("kdal", "Dallas");
        STATION_NAMES.put("kelp", "El Paso");
        STATION_NAMES.put("kiah", "Houston");
        STATION_NAMES.put("kaus", "Austin");
        STATION_NAMES.put("klbb", "Lubbock");
    }

    public static void main(String... args) {

        staticFileLocation("/static");

        get(new VelocityRoute("/stations") {

            @Override
            public Object handle(Request request, Response response) {
                Map<String, Object> model = Maps.newHashMap();
                model.put("stationsMap", STATION_NAMES);
                return modelAndView(model, "index.wm");
            }
        });

        get(new VelocityRoute("/summer88") {

            @Override
            public Object handle(Request arg0, Response arg1) {
                Map<String, Object> model = Maps.newHashMap();
                model.put("stationIds", STATION_IDS);
                return modelAndView(model, "summer.wm");
            }
        });

        get(new VelocityRoute("/stations/:stationName") {

            @Override
            public Object handle(Request request, Response response) {

                String stationName = request.params(":stationName");
                String id = STATION_IDS.get(stationName);

                if (id == null) {
                    halt(404, "No such station");
                }

                Map<String, String> model = Maps.newHashMap();
                model.put("location", STATION_NAMES.get(stationName));
                model.put("id", id);
                model.put("start", request.queryParams("start"));
                model.put("end", request.queryParams("end"));
                model.put("resolution", request.queryParams("resolution"));

                return modelAndView(model, "station.wm");
            }
        });

    }

}
