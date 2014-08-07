CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.terms (
    application text,
    field text,
    value text,
    resource text,
    PRIMARY KEY((application, field, value), resource)
);

CREATE TABLE $KEYSPACE$.resource_attributes (
    application text,
    resource text,
    attribute text,
    value text,
    PRIMARY KEY((application, resource), attribute)
);

CREATE TABLE $KEYSPACE$.resource_metrics (
    application text,
    resource text,
    metric_name text,
    PRIMARY KEY((application, resource), metric_name)
);
