CREATE KEYSPACE $KEYSPACE$
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

CREATE TABLE $KEYSPACE$.lucene_files (
    index_name text,
    id uuid,
    segment int,
    data blob,
    PRIMARY KEY((index_name,id), segment)
);

CREATE TABLE $KEYSPACE$.lucene_files_idx (
    index_name text,
    name text,
    id uuid,
    PRIMARY KEY(index_name, name)
);

CREATE TABLE $KEYSPACE$.lucene_locks (
    name text PRIMARY KEY,
    locked boolean
);