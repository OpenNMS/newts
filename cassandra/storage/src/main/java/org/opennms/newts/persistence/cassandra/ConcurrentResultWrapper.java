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
package org.opennms.newts.persistence.cassandra;


import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Future;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;


public class ConcurrentResultWrapper implements Iterator<Row> {

    private Iterator<Row> m_rowIter;

    public ConcurrentResultWrapper(Collection<Future<ResultSet>> futures) {
        m_rowIter = Iterators.concat(getIterators(futures));
    }

    private Iterator<Iterator<Row>> getIterators(Collection<Future<ResultSet>> futures) {
        return Iterators.transform(futures.iterator(), new Function<Future<ResultSet>, Iterator<Row>>() {

            @Override
            public Iterator<Row> apply(Future<ResultSet> input) {
                try {
                    return input.get().iterator();
                }
                catch (Exception e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }

    @Override
    public boolean hasNext() {
        return m_rowIter.hasNext();
    }

    @Override
    public Row next() {
        return m_rowIter.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
