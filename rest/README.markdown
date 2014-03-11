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

    $ curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost?start=0&end=1800000'; echo


