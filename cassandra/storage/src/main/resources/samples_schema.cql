
CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.samples (
    partition int,
    resource text,
    collected_at timestamp,
    metric_name text,
    value blob,
    attributes map<text, text>,
    PRIMARY KEY((partition, resource), collected_at, metric_name)
);
