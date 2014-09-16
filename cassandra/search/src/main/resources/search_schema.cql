CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.terms (
    context text,
    field text,
    value text,
    resource text,
    PRIMARY KEY((context, field, value), resource)
);

CREATE TABLE $KEYSPACE$.resource_attributes (
    context text,
    resource text,
    attribute text,
    value text,
    PRIMARY KEY((context, resource), attribute)
);

CREATE TABLE $KEYSPACE$.resource_metrics (
    context text,
    resource text,
    metric_name text,
    PRIMARY KEY((context, resource), metric_name)
);
