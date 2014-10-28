# Newts #

Newts is a massively scalable time series data store based on
[Apache Cassandra][1].

[1]: http://cassandra.apache.org "Cassandra website"


## Configuration and Initialization ##

Newts ships with a sample configuration file `etc/config.yaml`.  Reasonable
defaults are provided wherever possible, but you should review it carefully
beforehand, as all but the most trivial of installations will require *some*
customization.

See the [configuration reference][2] on the project wiki for more
information.

### Initializing ###

Before starting Newts, you must first perform a one-time initialization.
This initialization includes the creation of Cassandra schema, so the
configuration file must reflect the correct Cassandra connection parameters
or initialization will fail.  To perform initialization, simply run the
`init` script with the configuration file as the only argument:

    $ bin/init etc/config.yaml

[2]: https://github.com/OpenNMS/newts/wiki/RestService#newts-specific-configuration


## Running ##

A startup script, `bin/newts`, can be used to start the Newts REST service,
running the script with `-h` will output usage information:

    $ bin/newts -h
    Usage: java NewtsDaemon -c CFGFILE [-D] [-p PIDFILE] [-h]
      -D (--daemonize)      : Detach and run in the background
      -c (--config) CFGFILE : Path to configuration file (required).
      -h (--help)           : Print usage informations.
      -p (--pid) PIDFILE    : Path to PID file (default: newtsd.pid).


### Foreground ###

Newts starts in the foreground by default, and the included configuration
defaults to console logging.  Example:

    $ bin/newts -c etc/config.yaml
    ...

Press `<CTRL>+c` to terminate when running in the foreground.


### Daemonizing ###

The included configuration defaults to console logging, which is *NOT*
suitable for daemonizing.  You *must* configure file-based logging when
daemonizing, or *important log output will be lost*.  To start as a
daemon, simply pass the `-D` argument.

    $ bin/newts -D -p /var/run/newts.pid -c etc/config.yaml

To shutdown Newts, send a TERM signal to its PID.  For example:

    $ kill `cat /var/run/newts.pid`
