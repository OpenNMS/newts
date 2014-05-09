
CREATE KEYSPACE NEWTS_KEYSPACE
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE NEWTS_KEYSPACE.samples (
    resource text,
    collected_at timestamp,
    metric_name text,
    metric_type text,
    value blob,
    attributes map<text, text>,
    PRIMARY KEY(resource, collected_at, metric_name)
);
