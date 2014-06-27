package org.opennms.newts.stress;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.api.Timestamp;

public class TimestampOptionHandler extends OneArgumentOptionHandler<Timestamp> {

    public TimestampOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Timestamp> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Timestamp parse(String argument) throws NumberFormatException, CmdLineException {
        DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();
        DateTime dateTime = parser.parseDateTime(argument);
        return Timestamp.fromEpochMillis(dateTime.getMillis());
    }

    @Override
    public String getDefaultMetaVariable() {
        return "<timestamp>";
    }

}
