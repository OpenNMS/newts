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

Prerequisites
-------------

Before attempting to start this demo, you should have a running Cassandra
cluster, and a Newts `REST endpoint`_.  Setting up Cassandra is beyond the
scope of this document, so have a look at the `Cassandra Wiki`_ if you need
help with that.  Directions on setting up a `REST endpoint`_ can be found
in the README for that module.

Building
--------

To build the GSOD example code, run:

.. code:: sh

   mvn install

Running
-------
All of the example commands below assume that you are in the top-level
directory of the *newts-gsod* module.  For example:

.. code:: sh

   cd <newts src>/examples/gsod


Importing Data
~~~~~~~~~~~~~~

To import the included data, run:

.. code:: sh

	java -cp target/newts-gsod-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
		org.opennms.newts.gsod.ImportRunner -p 100 \
		ftp.ncdc.noaa.gov/pub/data/gsod/1988

The importer accepts a single argument for the name of a directory that is
searched recursively for GSOD data files.  You can load additional data by
changing this argument accordingly:

.. code:: sh

	java -cp target/newts-gsod-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
		org.opennms.newts.gsod.ImportRunner -p 100 \
		/path/to/additional/data

The import process connects to Cassandra directly, if necessary you can
override the Cassandra hostname, port, and keyspace name using system
properties.  For example:

.. code:: sh

	java -Dcassandra.keyspace=newts -Dcassandra.host=localhost -Dcassandra.port=9042 \
		-cp target/newts-gsod-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
		org.opennms.newts.gsod.ImportRunner -p 100 \
		ftp.ncdc.noaa.gov/pub/data/gsod/1988

Usage for Importer
~~~~~~~~~~~~~~~~~~

java -cp target/newts-gsod-1.0.0-SNAPSHOT-jar-with-dependencies.jar org.opennms.newts.gsod.ImportRunner2 [options] sourceDir

 sourceDir                              : the source directory that contains
                                          gsod data to import. These must be
                                          gzip'd files
 -n (--samples-per-batch) sample-count  : the maxinum number of samples to
                                          include in each post to the repository
                                          (default: 1000)
 -p (--parallelism) thread-count        : when using direct the size of the
                                          thread pool that posts the results. 
                                          (defaults to 1 ie no parallelism)
 -q (--max-work-queue-size) batch-count : when using direct the max size of the
                                          work-queue (defaults to thread-count
                                          * 3)
 -u (--url) url                         : publish data via a Newts REST server
                                          at the given url (default: use direct
                                          access via Newts API)

  
Starting Demo Webserver
~~~~~~~~~~~~~~~~~~~~~~~
Issue the following to start the web server:

.. code:: sh

   java -cp target/newts-gsod-1.0.0-SNAPSHOT-jar-with-dependencies.jar org.opennms.newts.gsod.Web

View Examples
~~~~~~~~~~~~~
You can either view individual graphs of the `6 Texas stations`_, or see a
report of all 6 for the `Summer of 1988`_.

*Note: These links assume that you are launching a browser on the same machine
that Newts is running on.  You may need to alter the link accordingly if that
is not the case.*


.. _Global Surface Summary of Day: https://gis.ncdc.noaa.gov/geoportal/catalog/search/resource/details.page?id=gov.noaa.ncdc:C00516

.. _REST endpoint: https://github.com/OpenNMS/newts/blob/master/rest/README.rst

.. _6 Texas stations: http://localhost:4567/stations

.. _Summer of 1988: http://localhost:4567/summer88

.. _Cassandra Wiki: https://wiki.apache.org/cassandra/GettingStarted

