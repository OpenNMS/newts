
CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.resource_idx (
    parent text,
    child text,
    PRIMARY KEY(parent, child)
);

CREATE TABLE $KEYSPACE$.metric_idx (
    resource text,
    metric_name text,
    PRIMARY KEY(resource, metric_name)
);
