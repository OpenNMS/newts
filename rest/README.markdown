NewTS REST
==========

To start the REST server from the build directory, use:

    $ mvn exec:java

To get started reading/writing data, try:

    $ curl \
        -D - \
        -X POST \
        -H "Content-Type: application/json" \
        -d @measurements.txt \
        http://0.0.0.0:4567/

    $ curl -D - -X GET 'http://0.0.0.0:4567/localhost?start=0&end=1800000'; echo


