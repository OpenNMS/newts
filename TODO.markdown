TODO
====

 * REST interface
 * Implement temporal row-key partitioning
 * If calculations of calculations are permitted, then the current implementation
   is buggy, and calculations need to be performed in dependency-order.  If not,
   ResultDescriptor should except if you try this.  Either way, a test is needed.
 * Revisit the ValueType classes.  Is this the right way to go about it, (a blob
   that stores a binary encoded value according to the type).  Do we have the
   right set of types?
 * Use a prepared statement in CassandraSampleRepository#cassandraSelect
