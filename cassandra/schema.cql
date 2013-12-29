
DROP KEYSPACE newts;

CREATE KEYSPACE newts
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE newts.measurements (
    resource text,
    collected_at timestamp,
    metric_name text,
    metric_type text,
    value blob,
    attributes map<text, text>,
    PRIMARY KEY(resource, collected_at, metric_name)
);
