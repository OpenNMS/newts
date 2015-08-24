1.3.1
~~~~~

Resource id separators can now be escaped. When enabled, colons prefixed with
a backslash will be treated as literal characters. This can be enabled
by setting the *separatorEscaping* flag.

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
