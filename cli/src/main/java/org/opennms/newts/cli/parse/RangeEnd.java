package org.opennms.newts.cli.parse;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opennms.newts.api.Timestamp;


class RangeEnd extends Relation {

    private static final DateTimeFormatter PARSER = ISODateTimeFormat.dateTimeParser();

    RangeEnd(String token) {
        super("timestamp", Operator.LT, token);
    }

    Timestamp getTimestamp() {
        DateTime dateTime = PARSER.parseDateTime(m_value);
        return Timestamp.fromEpochMillis(dateTime.getMillis());
    }

}
