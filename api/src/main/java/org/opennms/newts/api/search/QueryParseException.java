package org.opennms.newts.api.search;


// A wrapper for org.apache.lucene.queryparser.classic.ParseExceptions
public class QueryParseException extends Exception {

    private static final long serialVersionUID = 1L;

    public QueryParseException(Throwable cause) {
        super(cause);
    }

    public QueryParseException(String msg, Throwable cause) {
        super(msg, cause);
    }

}
