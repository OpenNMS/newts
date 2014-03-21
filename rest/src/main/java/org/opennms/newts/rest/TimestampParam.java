package org.opennms.newts.rest;


import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opennms.newts.api.Timestamp;

import com.yammer.dropwizard.jersey.params.AbstractParam;


/**
 * JAX-RS parameter that encapsulates creation of {@link Timestamp} instances from ISO 8601
 * timestamps, or seconds since the Unix epoch. Non-parseable values will result in a
 * {@code 400 Bad Request} response.
 *
 * @author eevans
 */
public class TimestampParam extends AbstractParam<Timestamp> {

    private static final DateTimeFormatter parser = ISODateTimeFormat.dateTimeParser();

    public TimestampParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return String.format("Unable to parse '%s' as date-time", input);
    }

    @Override
    protected Timestamp parse(String input) throws Exception {

        if (input.matches("^[\\d]+$")) {
            return Timestamp.fromEpochSeconds(Integer.valueOf(input));
        }

        DateTime dateTime = parser.parseDateTime(input);

        return Timestamp.fromEpochMillis(dateTime.getMillis());
    }

}
