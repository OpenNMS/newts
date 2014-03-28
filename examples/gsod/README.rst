GSOD Example
============

The `Global Surface Summary of Day`_ (GSOD) is a dataset produced by the
National Climatic Data Center (NCDC) in Asheville NC, using daily summaries
of weather conditions compiled by the USAF Climatology Center.  Elements
available from each station include min, mean, and max temperatures, dew
point, pressures, visibility, wind speeds, and precipitation.

The entire GSOD dataset is quite large (more than 7GB at the time of
writing), but a small sample covering 6 Texas cities for the year 1988 is
provided for purposes of running this demo.

Getting started
---------------

This demo requires that a Cassandra cluster and Newts `REST endpoint`_ are
already setup and running.  The `REST endpoint`_ comes with a sample
configuration (``example-config.yaml``) that contains a ``gsod`` report
definition.  If you are not running the endpoint with the included sample
config, then make sure to copy that report definition into your config.

To build the GSOD example code::

   mvn install

Importing Data
~~~~~~~~~~~~~~

Use the following to import the included sample data::

   mvn exec:java -Dexec.mainClass="org.opennms.newts.gsod.ImportRunner" \
           -Dexec.arguments="ftp.ncdc.noaa.gov/pub/data/gsod/1988/"

You can override the Cassandra hostname, port, and keyspace name using system
properties, if needed::

   mvn exec:java -Dexec.mainClass="org.opennms.newts.gsod.ImportRunner" \
          -Dexec.arguments="ftp.ncdc.noaa.gov/pub/data/gsod/1988/" \
          -Dcassandra.keyspace=newts -Dcassandra.host=localhost -Dcassandra.port=9042
  
Starting Demo Webserver
~~~~~~~~~~~~~~~~~~~~~~~
Issue the following to start the web server::

   mvn exec:java -Dexec.mainClass="org.opennms.newts.gsod.Web"

View Examples
~~~~~~~~~~~~~
You can either view individual graphs of the `6 Texas stations`_, or see a
report of all 6 for the `Summer of 1988`_.

.. _Global Surface Summary of Day: https://gis.ncdc.noaa.gov/geoportal/catalog/search/resource/details.page?id=gov.noaa.ncdc:C00516

.. _REST endpoint: https://github.com/OpenNMS/newts/blob/master/rest/README.rst

.. _6 Texas stations: http://localhost:4567/stations

.. _Summer of 1988: http://localhost:4567/summer88

