package org.opennms.newts.api.search.query;

import java.io.Reader;
import java.io.StringReader;

import org.opennms.newts.api.search.BooleanQuery;
import org.opennms.newts.api.search.Query;

public abstract class QueryParserBase {

    // Generated functions
    public abstract void ReInit(Reader stream);
    public abstract Query TopLevelQuery() throws ParseException;

    public Query parse(String query) throws ParseException {
        ReInit(new StringReader(query));
        try {
            Query q = TopLevelQuery();
            return q != null ? q : new BooleanQuery();
        } catch (ParseException qpe) {
            ParseException e = new ParseException("Cannot parse '" + query + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        } catch (TokenMgrError qpe) {
            ParseException e = new ParseException("Cannot parse '" + query + "': " + qpe.getMessage());
            e.initCause(qpe);
            throw e;
        }
    }

    protected String discardEscapeChar(String input) throws ParseException {
        char[] output = new char[input.length()];
        int length = 0;
        boolean lastCharWasEscapeChar = false;

        for (int i = 0; i < input.length(); i++) {
            char curChar = input.charAt(i);

            if (!lastCharWasEscapeChar && curChar == '\\') {
                lastCharWasEscapeChar = true;
            } else {
                output[length++] = curChar;
                lastCharWasEscapeChar = false;
            }
        }

        if (lastCharWasEscapeChar) {
            throw new ParseException("Term can not end with escape character.");
        }

        return new String(output, 0, length);
    }
}
