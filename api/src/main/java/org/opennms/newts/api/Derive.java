package org.opennms.newts.api;


import com.google.common.primitives.UnsignedLong;


public class Derive extends Counter {

    private static final long serialVersionUID = 1L;

    public Derive(UnsignedLong value) {
        super(value);
    }

    @Override
    public MetricType getType() {
        return MetricType.DERIVE;
    }

}
