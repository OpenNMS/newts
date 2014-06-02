package org.opennms.newts.gsod;

/**
 * Created by brozow on 4/23/14.
 */
public class JSONBuilder {
    StringBuilder buf = new StringBuilder();
    boolean object = false;
    boolean attr = false;


    JSONBuilder() {
        buf.append("\n[\n");
    }

    public JSONBuilder newObject() {
        if (object) buf.append("\n },\n");
        buf.append(" {\n");
        object = true;
        attr = false;
        return this;
    }

    public JSONBuilder attr(String name, long val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, int val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, double val) {
        if (Double.isNaN(val)) return this;
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": ").append(val);
        attr = true;
        return this;
    }

    public JSONBuilder attr(String name, String val) {
        if (attr) buf.append(",\n");
        buf.append("   \"").append(name).append("\"").append(": \"").append(val).append("\"");
        attr = true;
        return this;
    }

    public String toString() {
        if (object) buf.append("\n }\n");
        buf.append("]\n");
        object = false;
        attr = false;
        return buf.toString();
    }
}
