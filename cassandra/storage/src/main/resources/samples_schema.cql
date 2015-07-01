
CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.samples (
    context text,
    partition int,
    resource text,
    collected_at timestamp,
    metric_name text,
    value blob,
    attributes map<text, text>,
    PRIMARY KEY((context, partition, resource), collected_at, metric_name)
);
