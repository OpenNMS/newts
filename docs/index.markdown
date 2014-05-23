---
layout: docs
title: Welcome
next_section: quickstart
permalink: /docs/home/
---

Newts is a time-series data store based on [Apache Cassandra].

Features
========

### High throughput
Newts is built upon [Apache Cassandra], a linearly scalable, write-optimized,
distributed database.

### Grouped access
It's common to collect, store, and retrieve groups of similar or related metrics,
(think bytes in and bytes out, or 1, 5, and 15 minute load averages); Newts
allows you to group metrics for more efficient storage and retrieval.

### Late aggregation
Many time-series solutions perform in-line, or batch computed aggregations of the
entire dataset, even when the ratio of reads to writes is staggeringly small.
Newts removes aggregation as a bottleneck to storage, and eliminates the wasted
computation resources caused by unused aggregate values.
  

[Apache Cassandra]: http://cassandra.apache.org "Apache Cassandra"
