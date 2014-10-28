package org.opennms.newts.cli.parse;


public class ReportableError extends RuntimeException {

    private static final long serialVersionUID = 7441083285556104940L;

    public ReportableError(String msg) {
        super(msg);
    }

}
