CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.terms (
    application text,
    field text,
    term text,
    resource text,
    PRIMARY KEY((application, field, term), resource)
);

