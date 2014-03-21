NewTS REST
==========

Quickstart
----------
Building::

    $ mvn clean package

Starting the REST server from the build directory::

    $ java -jar target/newts-rest-<version>-SNAPSHOT.jar server config.yaml

To get started reading/writing data, try::

    $ curl \
        -D - \
        -X POST \
        -H "Content-Type: application/json" \
        -d @measurements.txt \
        http://0.0.0.0:8080/samples

    $ curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost.temps?start=1998-07-09T12:05:00-0500&end=1998-07-09T13:15:00-0500'; echo
    $ curl -D - -X GET 'http://0.0.0.0:8080/measurements/temps/localhost.temps?end=1998-07-09T13:15:00-0500&start=1998-07-09T12:05:00-0500&resolution=15m'; echo


API
---
Samples
~~~~~~~
::

    POST /samples
    GET /samples/<resource>?start=<start>&end=<end>

Query arguments:

  sample
    Query start time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.
  end
    Query end time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.


Measurements
~~~~~~~~~~~~
::

    GET /measurements/<report>/<resource>?start=<start>&end=<end>&resolution=<resolution>

Query arguments:
    
  sample
    Query start time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.
  end
    Query end time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.
  resolution
    The resolution of measurements returned, specified as an integer value,
    followed by a resolution unit specifier character.  Valid unit specifiers
    are ``s``, ``m``, ``h``, ``d``, and ``w``.

    Examples: ``15m``, ``1d``, ``1w`` (for 15 minutes, 1 day, and 1 week
    respectively).


.. _ISO 8601: http://en.wikipedia.org/wiki/Iso8601

