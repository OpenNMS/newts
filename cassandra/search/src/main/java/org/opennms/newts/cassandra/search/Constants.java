package org.opennms.newts.cassandra.search;


class Constants {

    static String DEFAULT_TERM_FIELD = "_all";

    static class Schema {
        // Terms storage
        static final String T_TERMS = "terms";
        static final String C_TERMS_CONTEXT = "context";
        static final String C_TERMS_FIELD = "field";
        static final String C_TERMS_VALUE = "value";
        static final String C_TERMS_RESOURCE = "resource";

        // Attributes
        static final String T_ATTRS = "resource_attributes";
        static final String C_ATTRS_CONTEXT = "context";
        static final String C_ATTRS_RESOURCE = "resource";
        static final String C_ATTRS_ATTR = "attribute";
        static final String C_ATTRS_VALUE = "value";

        // Metrics
        static final String T_METRICS = "resource_metrics";
        static final String C_METRICS_CONTEXT = "context";
        static final String C_METRICS_RESOURCE = "resource";
        static final String C_METRICS_NAME = "metric_name";
    }
}
