package org.opennms.newts.persistence.cassandra;

import java.io.InputStream;


public class Schema implements org.opennms.newts.cassandra.Schema {

    @Override
    public InputStream getInputStream() {
        return getClass().getResourceAsStream("/samples_schema.cql");
    }

}
