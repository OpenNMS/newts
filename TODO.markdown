TODO
====

 * Implement indexing of resources
 * If calculations of calculations are permitted, then the current implementation
   is buggy, and calculations need to be performed in dependency-order.  If not,
   ResultDescriptor should except if you try this.  Either way, a test is needed.
 * Revisit the ValueType classes.  Is this the right way to go about it, (a blob
   that stores a binary encoded value according to the type).  Do we have the
   right set of types?
 * Add licensing
 * Have added to a CI system

newts-cassandra
---------------
 * Cassandra drivers DEBUG output is very chatty; Create a logging
   configuration for the tests, level INFO.
 * Validate query start/end times
 * Implement temporal row-key partitioning
 * Make column TTLs an option
 * Use a prepared statement in CassandraSampleRepository#cassandraSelect
 * Enable CQL driver compression

newts-rest
----------
 * Implement report/result descriptor creation via REST interface
 * Properly handle optional resolution query argument (default to something
   reasonable)
 * Add duration query arg (use in place of start/end)
 * Rewrite README in a more expressive markup
 * Handling of exceptions raised from the repository
 * Clean up unused code from Transform
