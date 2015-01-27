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


.. _HTTP Basic Auth: http://en.wikipedia.org/wiki/Basic_access_authentication
.. _AngularJS: http://angularjs.org
.. _Karaf: http://karaf.apache.org
.. _Maven: http://maven.apache.org
