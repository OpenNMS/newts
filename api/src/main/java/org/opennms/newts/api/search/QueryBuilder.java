package org.opennms.newts.api.search;

public class QueryBuilder {

    private QueryBuilder() { }

    public static Query matchAllValues(String... values) {
        final BooleanQuery query = new BooleanQuery();
        Operator op = Operator.OR;
        for (final String value : values) {
            query.add(new TermQuery(new Term(value)), op);
            op = Operator.AND;
        }
        return query;
    }

    public static Query matchAnyValue(String... values) {
        final BooleanQuery query = new BooleanQuery();
        for (final String value : values) {
            query.add(new TermQuery(new Term(value)), Operator.OR);
        }
        return query;
    }
}
