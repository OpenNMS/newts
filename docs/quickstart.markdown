---
layout: docs
title: Quick-start guide
prev_section: home
permalink: /docs/quickstart/
---

You need: A Unix'ish environment w/ git, wget, tar, sed, etc

You need: Java 7 (OpenJDK 7, etc), and Maven 3

Cassandra
---------

    # Terminal 1
    wget -O http://www.apache.org/dist/cassandra/2.0.7/apache-cassandra-2.0.7-bin.tar.gz
    tar xvf apache-cassandra-2.0.7-bin.tar.gz
    cd apache-cassandra-2.0.7/
    
    sed -i "s/var\/lib/tmp/g" conf/cassandra.yaml
    sed -i "s/var\/log/tmp/g" conf/log4j-server.properties
    
    bin/cassandra -f

Newts
-----
At this point, if there were no exceptions, Cassandra will be running in the foreground, with log output printed to the console.  Open a new console, and enter the following::

    # Terminal 2
    git clone https://github.com/OpenNMS/newts.git
    NEWTSSRC=`pwd`/newts
    
    cd $NEWTSSRC
    mvn install

This downloads and builds the Newts source, and installs it to your Maven repository (usually ``~/.m2/repository``).

Next, we'll initialize (create the Newts schema), and start the REST endpoint::

    # Terminal 2
    cd $NEWTSSRC/rest
    java -jar target/newts-rest-1.0.0-SNAPSHOT.jar init example-config.yaml
    java -jar target/newts-rest-1.0.0-SNAPSHOT.jar server example-config.yaml

*Note:  During initialization, SLF4J may complain about logger implementations, you can safely ignore this.*

*Note:  During Newts server startup, you may see warnings about missing compression libraries from the Cassandra driver, you can safely ignore these.*

Testing
-------
``$NEWTS/rest/measurements.txt`` contains samples in the JSON representation that Newts expects.  Importing these samples is a quick way to be sure everything is working::

    # Terminal 3
    # Import test samples
    curl -D - -X POST -H "Content-Type: application/json" -d @measurements.txt \
            http://0.0.0.0:8080/samples

    # Read test samples
    curl -D - -X GET 'http://0.0.0.0:8080/samples/localhost%3Achassis%3Atemps?start=1998-07-09T12:05:00-0500&end=1998-07-09T13:15:00-0500'; echo

Next Steps
----------
Why not try the [Global Summary of Day (weather data) demo]?

[Global Summary of Day (weather data) demo]: {{ site.baseurl }}/docs/gsod
