---
layout: docs
title: Welcome
next_section: quickstart
permalink: /docs/home/
---

Newts is a time-series data store based on `Apache Cassandra`_.

Features
--------
High throughput
  Newts is built upon `Apache Cassandra`_, a write-optimized, fully distributed partioned row store.
Grouped access
  It's common to collect, store, and retrieve metrics together, (think bytes in and bytes out, or 1, 5, and 15 minute load averages.); Newts allows for similar metrics to be grouped together for more efficient storage and retrieval.
Late aggregation
  Most time-series solutions perform in-line aggregations for purposes of later ploting visualizations, even though the ratio of reads to write is staggeringly small.  Newts performs plot aggregations at the time of the read.

Bugs
----

http://issues.opennms.org/browse/NEWTS
  
Terminology
-----------

Resource
~~~~~~~~
Resources are unique strings that identify a metric, or group of metrics.

Measurements vs. Samples
~~~~~~~~~~~~~~~~~~~~~~~~
Samples represent the raw, or *sampled* data.  Measurements are calculated from the collected samples.

.. _Apache Cassandra: http://cassandra.apache.org