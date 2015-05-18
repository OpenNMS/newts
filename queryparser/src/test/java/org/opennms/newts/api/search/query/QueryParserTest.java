package org.opennms.newts.api.search.query;

import org.junit.Test;
import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Operator;
import org.opennms.newts.api.search.Query;
import org.opennms.newts.api.search.Term;
import org.opennms.newts.api.search.TermQuery;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Verifies that we can parse human readable query strings
 * into query objects used by the Search API.
 *
 * Objects are also parsed from their "toString()" representation
 * to verify the accuracy of these.
 */
public class QueryParserTest {

    @Test(expected=ParseException.class)
    public void failsWithBlankQueryString() throws ParseException {
        parse("");
    }

    @Test
    public void canParseSimpleQuerys() throws ParseException {
        Query query = new TermQuery(new Term("beef"));
        assertThat(parse(query), equalTo(query));
        assertThat(parse("beef"), equalTo(query));

        query = new TermQuery(new Term("meat", "beef"));
        assertThat(parse(query), equalTo(query));
        assertThat(parse("meat:beef"), equalTo(query));
    }

    @Test
    public void canParseCompoundQuerys() throws ParseException {
        BooleanQuery query = new BooleanQuery();
        query.add(new TermQuery(new Term("meat", "beef")), Operator.OR);
        query.add(new TermQuery(new Term("music", "rock")), Operator.OR);

        assertThat(parse(query), equalTo((Query)query));
        assertThat(parse("meat:beef music:rock"), equalTo((Query)query));
        assertThat(parse("meat:beef OR music:rock"), equalTo((Query)query));
        assertThat(parse("meat:beef || music:rock"), equalTo((Query)query));

        query = new BooleanQuery();
        query.add(new TermQuery(new Term("meat", "beef")), Operator.OR);
        query.add(new TermQuery(new Term("music", "rock")), Operator.AND);

        assertThat(parse(query), equalTo((Query)query));
        assertThat(parse("meat:beef AND music:rock"), equalTo((Query)query));
        assertThat(parse("meat:beef && music:rock"), equalTo((Query)query));

        query = new BooleanQuery();
        query.add(new TermQuery(new Term("meat", "beef")), Operator.OR);
        query.add(new TermQuery(new Term("music", "rock")), Operator.AND);
        query.add(new TermQuery(new Term("sauce")), Operator.OR);

        assertThat(parse(query), equalTo((Query)query));
        assertThat(parse("meat:beef AND music:rock OR sauce"), equalTo((Query)query));
        assertThat(parse("meat:beef && music:rock || sauce"), equalTo((Query)query));
    }

    @Test
    public void canParseGroupedQueries() throws ParseException {
        BooleanQuery subQuery1 = new BooleanQuery();
        subQuery1.add(new TermQuery(new Term("meat", "beef")), Operator.OR);
        subQuery1.add(new TermQuery(new Term("music", "rock")), Operator.OR);

        BooleanQuery subQuery2 = new BooleanQuery();
        subQuery2.add(new TermQuery(new Term("meat", "chicken")), Operator.OR);
        subQuery2.add(new TermQuery(new Term("music", "country")), Operator.AND);

        TermQuery subQuery3 = new TermQuery(new Term("sauce"));

        BooleanQuery query = new BooleanQuery();
        query.add(subQuery1, Operator.OR);
        query.add(subQuery2, Operator.OR);
        query.add(subQuery3, Operator.AND);

        assertThat(parse(query), equalTo((Query)query));
        assertThat(parse("(meat:beef OR music:rock) OR (meat:chicken AND music:country) AND sauce"), equalTo((Query)query));
    }

    @Test
    public void canEscapeColons() throws ParseException {
        Query query = new TermQuery(new Term("meat", "be:ef:"));

        assertThat(parse(query), equalTo((Query)query));
        assertThat(parse("meat:be\\:ef\\:"), equalTo(query));
    }

    private static Query parse(Query query) throws ParseException {
        return parse(query.toString());
    }

    private static Query parse(String query) throws ParseException {
        QueryParser qp = new QueryParser();
        return qp.parse(query);
    }
}
