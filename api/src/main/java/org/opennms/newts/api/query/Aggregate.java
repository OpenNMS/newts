package org.opennms.newts.api.query;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public class Aggregate {

    public Aggregate(Function func, String name, String... sources) {
        checkNotNull(func, "function argument");
        checkNotNull(name, "name argument");
        checkArgument(sources.length > 0, "No source specified");
    }

}
