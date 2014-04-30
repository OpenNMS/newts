
CREATE KEYSPACE newts
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE newts.samples (
    resource text,
    collected_at timestamp,
    metric_name text,
    metric_type text,
    value blob,
    attributes map<text, text>,
    PRIMARY KEY(resource, collected_at, metric_name)
);

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
