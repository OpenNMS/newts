package org.opennms.newts.api.search;


import java.io.IOException;
import java.util.Collection;

import org.opennms.newts.api.Sample;


public interface Indexer {

    /**
     * Updates the search index from a collection of samples. The semantics are assumed to be that
     * of an <a href="http://en.wikipedia.org/wiki/Upsert">upsert</a>, (i.e. new values are added,
     * existing ones are updated to match). High frequency (re)submissions of unaltered sample
     * meta-data are the norm here.
     *
     * @param samples
     *            the samples to index
     * @throws IOExecption
     */
    public void update(Collection<Sample> samples) throws IOException;

}
