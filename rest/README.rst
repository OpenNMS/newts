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
        -d @measurements.txt \
        http://0.0.0.0:8080/samples

    $ curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost.temps?start=1998-07-09T12:05:00-0500&end=1998-07-09T13:15:00-0500'; echo
    $ curl -D - -X GET 'http://0.0.0.0:8080/measurements/temps/localhost.temps?end=1998-07-09T13:15:00-0500&start=1998-07-09T12:05:00-0500&resolution=15m'; echo


API
---
Samples
~~~~~~~
Writing
+++++++
::
   
    POST /samples

Representation::

    [
        {
          "timestamp" : 900000000,
          "resource"  : "localhost",
          "name"      : "temperature",
          "type"      : GAUGE,
          "value"     : 97.5
        },
        {
          "timestamp" : 900000000,
          "resource"  : "localhost",
          "name"      : "humidity",
          "type"      : GAUGE,
          "value"     : 45.0
        },
        ...
    ]

Reading
+++++++
::

    GET /samples/<resource>?start=<start>&end=<end>

Representation::

    [
      [
        {
          "name"      : "temperature",
          "timestamp" : 900000000,
          "type"      : "GAUGE",
          "value"     : 97.5
        },
        {
          "name"      : "humidity",
          "timestamp" : 900000000,
          "type"      : "GAUGE",
          "value"     : 45.0
        },
      ],
      [
        ...
      ],
      ...
    ]

Query arguments:

  start
    Query start time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to 24 hours less than end.*
  end
    Query end time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to the current time.*


Measurements
~~~~~~~~~~~~
Reading
+++++++
::

    GET /measurements/<report>/<resource>?start=<start>&end=<end>&resolution=<resolution>

Representation::

    [
      [
        {
          "name"      : "temperature",
          "timestamp" : 900000000,
          "value"     : 97.5
        },
        {
          "name"      : "humidity",
          "timestamp" : 900000000,
          "value"     : 45.0
        },
      ],
      [
        ...
      ],
      ...
    ]

Query arguments:
    
  start
    Query start time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to 24 hours less than end.*
  end
    Query end time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to the current time.*
  resolution
    The resolution of measurements returned, specified as an integer value,
    followed by a resolution unit specifier character.  Valid unit specifiers
    are ``s``, ``m``, ``h``, ``d``, and ``w``.  *Required*.

    Examples: ``15m``, ``1d``, ``1w`` (for 15 minutes, 1 day, and 1 week
    respectively).


.. _ISO 8601: http://en.wikipedia.org/wiki/Iso8601

