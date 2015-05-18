package org.opennms.newts.api.search;

public abstract class Query {

    /**
     * Rewrites queries into a BooleanQuery consisting of TermQuerys.
     */
    public Query rewrite() {
        return this;
    }

    public String toString() {
        return "";
    }

}
