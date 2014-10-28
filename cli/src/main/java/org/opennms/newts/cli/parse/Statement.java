package org.opennms.newts.cli.parse;

public abstract class Statement {

    public static enum Type {
        SAMPLES_GET, EXIT;
    }

    public abstract Type getType();

}
