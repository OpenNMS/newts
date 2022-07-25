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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import com.datastax.oss.driver.api.core.cql.AsyncResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterators;


public class ConcurrentResultWrapper implements Iterator<Row> {

    private Iterator<Row> m_rowIter;

    public ConcurrentResultWrapper(Collection<CompletionStage<AsyncResultSet>> futures) {
        m_rowIter = Iterators.concat(getIterators(futures));
    }

    private Iterator<Iterator<Row>> getIterators(Collection<CompletionStage<AsyncResultSet>> futures) {
        return Iterators.transform(futures.iterator(), new Function<CompletionStage<AsyncResultSet>, Iterator<Row>>() {
            @Override
            public Iterator<Row> apply(CompletionStage<AsyncResultSet> input) {
                try {
                    return toBlockingIterator(input.toCompletableFuture().get());
                } catch (ExecutionException|InterruptedException e) {
                    throw Throwables.propagate(e);
                }
            }
        });
    }

    /**
     * Converts a {@link AsyncResultSet} to a blocking iterator.
     * This iterator blocks when the next page is loading - loading only one page at a time.
     */
    public Iterator<Row> toBlockingIterator(AsyncResultSet rs) {
        AtomicReference<Iterator<Row>> currentPageRef = new AtomicReference<>(rs.currentPage().iterator());
        return new Iterator<Row>() {
            @Override
            public boolean hasNext() {
                Iterator<Row> currentPage = currentPageRef.get();
                if (!currentPage.hasNext() && rs.hasMorePages()) {
                    try {
                        currentPageRef.set(rs.fetchNextPage().toCompletableFuture().get().currentPage().iterator());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                return currentPage.hasNext();
            }

            @Override
            public Row next() {
                return currentPageRef.get().next();
            }
        };
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
