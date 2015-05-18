package org.opennms.newts.api.search;

public enum Operator {

    OR(1), AND(2);

    private byte m_code;

    private Operator(int code) {
        m_code = (byte) code;
    }

    public byte getCode() {
        return m_code;
    }

    public static Operator fromCode(byte code) {
        for (Operator type : values()) {
            if (type.getCode() == code) {
                return type;
            }
        }
        throw new IllegalArgumentException(String.format("No such operator for 0x%x", code));
    }

}
