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
          "timestamp" : 900000000000,
          "resource"  : "localhost",
          "name"      : "temperature",
          "type"      : GAUGE,
          "value"     : 97.5
        },
        {
          "timestamp" : 900000000000,
          "resource"  : "localhost",
          "name"      : "humidity",
          "type"      : GAUGE,
          "value"     : 45.0
        },
        ...
    ]

The request body is composed of a JSON array of sample objects.  Sample objects have 5 mandatory, and 1 optional fields:

  timestamp
    The time this sample was taken; A numeric value representing the number
    of milliseconds since the Unix epoch.
  resource
    The unique name for a grouping of metrics.
  name
    Metric name.
  type
    The metric type (one of ``GAUGE``, ``COUNTER``, ``ABSOLUTE``, ``DERIVE``).
  value
    Numeric value of the sample
  attributes (optional)
    Abitrary key/values pairs to associate with the sample.


Reading
+++++++
::

    GET /samples/<resource>?start=<start>&end=<end>

Query arguments:

  start
    Query start time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to 24 hours less than end.*
  end
    Query end time.  Specified as seconds since the Unix epoch, or as an
    `ISO 8601`_ timestamp.  *Optional; defaults to the current time.*

Representation::

    [
      [
        {
          "name"      : "temperature",
          "timestamp" : 900000000000,
          "type"      : "GAUGE",
          "value"     : 97.5
        },
        {
          "name"      : "humidity",
          "timestamp" : 900000000000,
          "type"      : "GAUGE",
          "value"     : 45.0
        },
      ],
      [
        ...
      ],
      ...
    ]


Measurements
~~~~~~~~~~~~
Reading
+++++++
::

    GET /measurements/<report>/<resource>?start=<start>&end=<end>&resolution=<resolution>

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
    
Representation::

    [
      [
        {
          "name"      : "temperature",
          "timestamp" : 900000000000,
          "value"     : 97.5
        },
        {
          "name"      : "humidity",
          "timestamp" : 900000000000,
          "value"     : 45.0
        },
      ],
      [
        ...
      ],
      ...
    ]


.. _ISO 8601: http://en.wikipedia.org/wiki/Iso8601

