package org.opennms.newts.api;

import com.google.common.primitives.UnsignedLong;


public class Absolute extends Counter {

    private static final long serialVersionUID = 1L;

    public Absolute(UnsignedLong value) {
        super(value);
    }

    @Override
    public MetricType getType() {
        return MetricType.ABSOLUTE;
    }

}
