.. Newts documentation master file, created by
   sphinx-quickstart on Fri Apr 18 11:08:03 2014.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

About
=====

Newts is a time-series data store based on `Apache Cassandra`_.  It's features
include:

High throughput
  Newts is built upon `Apache Cassandra`_, a write-optimized, fully
  distributed partioned row store.
Grouped access
  It's common to collect, store, and retrieve metrics together, (think bytes
  in and bytes out, or 1, 5, and 15 minute load averages.); Newts allows for
  similar metrics to be grouped together for more efficient storage and
  retrieval.
Late aggregation
  Most time-series solutions perform in-line aggregations for purposes of
  later ploting visualizations, even though the ratio of reads to write is
  staggeringly small.  Newts performs plot aggregations at the time of the
  read.


Documentation
=============

.. toctree::
   :maxdepth: 2

   manual/index
   getting_started/index


.. _Apache Cassandra: http://cassandra.apache.org
