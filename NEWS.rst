1.3.6
~~~~~

Upgraded the Cassandra driver to 3.0.1.

Exposed the ability to set the replication factor when initializing the schema via the SchemaManager API.

1.3.5
~~~~~

Upgraded the Cassandra driver to 3.0.0.

Upgraded cassandra-unit to 3.0.0.1. Running the integrations tests now require Java 8.

Added support for connecting to Cassandra via SSL.

Added support for passing custom JVM arguments to the *newts* and *init* scripts. This
will be useful for handling custom trust store settings.

Fixed the JSON output of the /search REST resource.

Fixed the Karaf feature files and removed the default blueprints.

Addressed a number of code lint issues. Thanks to @faisal-hameed from DevFactory.

1.3.4
~~~~~

Performance improvements related to indexing.

1.3.3
~~~~~

Upgraded the Cassandra driver to 3.0.0-rc1.

Fixed an issue where counters would lose precision when converted to rates
for aggregation and a number of other minor bugs.

1.3.2
~~~~~

Fixed an issue where the hearbeat period was not properly handled, leading to
values that should have been NaNs being misrepresented.

1.3.1
~~~~~

The consistency levels used for reading and writing values from Casssandra
can now be controlled on a per context basis. Use the *read-consistency* and
*write-consistency* context options to set these.

Resource id separators can now be escaped. When enabled, colons prefixed with
a backslash will be treated as literal characters. This can be enabled
by setting the *separatorEscaping* flag.

The *cassandra.host* option can now be used to specify a comma separated list
of hosts.

A username and password can be set for connecting to Cassandra with basic
authentication.

1.3.0
~~~~~

Added a Graphite protocol listener. You can now push samples to Newts using
Graphite's `plain text protocol`_.

Added support for hierarchical indexing. When enabled, resources are tagged with
additional attributes that allows the resource tree to be walked.

Added support for storing and retrieving samples in named contexts.

1.2.0
~~~~~

Improved `Search API`_ and query parsing with support for AND operators
and grouped terms.

1.1.0
~~~~~

Introduced a very simple web interface based on AngularJS_.  Practical
applications of this UI are probably quite limited, nevertheless it should
provide a handy means of performing quick resource index searches, or
generating simple ad hoc graphs.  To give it a try, point your browser at:
http://localhost:8080/ui/ (adjusting hostnames and port as needed).

Cassandra protocol compression can now be enabled (defaults to NONE).

Support for `HTTP Basic Auth`_ has been added.

The Karaf_ feature Maven_ module was renamed from ``karaf``, to ``newts-karaf``.
Update any dependencies accordingly.


.. _Search API: https://github.com/OpenNMS/newts/wiki/Search
.. _HTTP Basic Auth: http://en.wikipedia.org/wiki/Basic_access_authentication
.. _AngularJS: http://angularjs.org
.. _Karaf: http://karaf.apache.org
.. _Maven: http://maven.apache.org
.. _plain text protocol: http://graphite.readthedocs.org/en/latest/feeding-carbon.html#the-plaintext-protocol
