NewTS REST
==========

Quickstart
----------
Building::

    $ mvn clean package

Initialize the database (Cassandra must be running)::

    $ java -jar target/newts-rest-<version>-SNAPSHOT.jar init config.yaml

Starting the REST server from the build directory::

    $ java -jar target/newts-rest-<version>-SNAPSHOT.jar server config.yaml

To get started reading/writing data, try::

    $ curl \
        -D - \
        -X POST \
        -H "Content-Type: application/json" \
        -d @samples.txt \
        http://0.0.0.0:8080/samples

    $ curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost%3Achassis%3Atemps?start=1998-07-09T12:05:00-0500&end=1998-07-09T13:15:00-0500'; echo
    $ curl -D - -X GET 'http://0.0.0.0:8080/measurements/temps/localhost%3Achassis%3Atemps?end=1998-07-09T13:15:00-0500&start=1998-07-09T12:05:00-0500&resolution=15m'; echo

To search::

    $ curl -D - -X GET 'http://0.0.0.0:8080/search?q=americas'

    
API
---

See the Wiki_ for a complete API reference.

.. _Wiki: https://github.com/OpenNMS/newts/wiki/RestAPI
.. _ISO 8601: http://en.wikipedia.org/wiki/Iso8601

