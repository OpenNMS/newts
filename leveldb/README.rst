newts-cassandra
===============

Integration tests are skipped by default, to enable::

    $ mvn install -DskipITs=false

The schema is stored as a file in ``src/main/resources`` and can be applied
to a Cassandra cluster using cqlsh, or alternately by issuing the following
command::

    mvn exec:java -Dexec.mainClass=org.opennms.newts.persistence.cassandra.SchemaManager -Dexec.arguments=create
