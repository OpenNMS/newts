/*
 * Copyright 2014, The OpenNMS Group
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
