NewTS REST
==========

To build:

    $ mvn clean package

To start the REST server from the build directory, use:

    $ java -jar target/newts-rest-<version>-SNAPSHOT.jar server config.yaml

To get started reading/writing data, try:

    $ curl \
        -D - \
        -X POST \
        -H "Content-Type: application/json" \
        -d @measurements.txt \
        http://0.0.0.0:8080/samples

    $ curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost.temps?start=900003900000&end=900008100000'; echo
    $ curl -D - -X GET 'http://0.0.0.0:8080/measurements/temps/localhost.temps?end=900008100000&start=900003900000&resolution=1200000'; echo


