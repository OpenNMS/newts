package org.opennms.newts.cli.parse;


class Resource extends Relation {

    Resource(String token) {
        super("resource", Operator.EQ, token);
    }

    org.opennms.newts.api.Resource getResource() {
        return new org.opennms.newts.api.Resource(m_value);
    }

}
