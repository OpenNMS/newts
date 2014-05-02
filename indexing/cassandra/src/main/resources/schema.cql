
CREATE KEYSPACE newts
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE newts.resource_idx (
    parent text,
    child text,
    PRIMARY KEY(parent, child)
);

CREATE TABLE newts.metric_idx (
    resource text,
    metric_name text,
    PRIMARY KEY(resource, metric_name)
);
