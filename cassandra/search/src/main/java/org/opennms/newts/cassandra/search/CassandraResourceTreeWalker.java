/*
 * Copyright 2015, The OpenNMS Group
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
package org.opennms.newts.cassandra.search;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opennms.newts.api.search.QueryBuilder.matchKeyAndValue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;

import javax.inject.Inject;

import org.opennms.newts.api.Context;
import org.opennms.newts.api.Resource;
import org.opennms.newts.api.search.SearchResults;
import org.opennms.newts.api.search.SearchResults.Result;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

/**
 * Walks the resource tree by searching for the appropriate _parent attributes.
 *
 * @author jwhite <jesse@opennms.org>
 */
public class CassandraResourceTreeWalker {

    private static final Resource TOP_LEVEL_RESOURCE = new Resource(
            Constants.TOP_LEVEL_PARENT_TERM_VALUE);

    private final CassandraSearcher m_searcher;

    public static interface SearchResultVisitor {

        /**
         * Called to visit a particular search result.
         *
         * @return true if the caller should continue visiting, false otherwise
         */
        public boolean visit(Result result);
    }

    @Inject
    public CassandraResourceTreeWalker(CassandraSearcher searcher) {
        m_searcher = checkNotNull(searcher, "searcher argument");
    }

    /**
     * Visits all nodes in the resource tree using breadth-first search.
     */
    public void breadthFirstSearch(Context context, SearchResultVisitor visitor) {
        breadthFirstSearch(context, visitor, TOP_LEVEL_RESOURCE);
    }

    /**
     * Visits all nodes in the resource tree bellow the given resource using
     * breadth-first search.
     */
    public void breadthFirstSearch(Context context, SearchResultVisitor visitor, Resource root) {
        Queue<Resource> queue = Lists.newLinkedList();
        queue.add(root);
        while (!queue.isEmpty()) {
            Resource r = queue.remove();
            for (SearchResults.Result result : m_searcher
                    .search(context, matchKeyAndValue(Constants.PARENT_TERM_FIELD, r.getId()))) {
                if (!visitor.visit(result)) {
                    return;
                }
                queue.add(result.getResource());
            }
        }
    }

    /**
     * Visits all nodes in the resource tree using depth-first search.
     */
    public void depthFirstSearch(Context context, SearchResultVisitor visitor) {
        depthFirstSearch(context, visitor, TOP_LEVEL_RESOURCE);
    }

    /**
     * Visits all nodes in the resource tree bellow the given resource using
     * depth-first search.
     */
    public void depthFirstSearch(Context context, SearchResultVisitor visitor, Resource root) {
        ArrayDeque<SearchResults.Result> stack = Queues.newArrayDeque();

        // Build an instance of a SearchResult for the root resource
        // but don't invoke the visitor with it
        boolean skipFirstVisit = true;
        SearchResults initialResults = new SearchResults();
        initialResults.addResult(root, new ArrayList<String>(0));
        stack.add(initialResults.iterator().next());

        while (!stack.isEmpty()) {
            SearchResults.Result r = stack.pop();
            if (skipFirstVisit) {
                skipFirstVisit = false;
            } else {
                if (!visitor.visit(r)) {
                    return;
                }
            }

            // Reverse the order of the results so we walk the left-most
            // branches first
            ImmutableList<SearchResults.Result> results = ImmutableList.copyOf(m_searcher.search(
                    context, matchKeyAndValue(Constants.PARENT_TERM_FIELD, r.getResource().getId())));
            for (SearchResults.Result result : results.reverse()) {
                stack.push(result);
            }
        }
    }
}
