package org.opennms.newts.cassandra;


public class CassandraException extends RuntimeException {

    private static final long serialVersionUID = 580764518584168297L;

    public CassandraException(Throwable cause) {
        super(cause);
    }

}
