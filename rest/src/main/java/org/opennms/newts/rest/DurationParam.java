package org.opennms.newts.rest;


import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.opennms.newts.api.Duration;

import com.yammer.dropwizard.jersey.params.AbstractParam;


/**
 * JAX-RS parameter that encapsulates creation of {@link Duration} instances from a string
 * specifier. Non-parseable values will result in a {@code 400 Bad Request} response.
 *
 * @author eevans
 */
public class DurationParam extends AbstractParam<Duration> {

    private static final PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendWeeks().appendSuffix("w")
            .appendDays().appendSuffix("d")
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .appendSeconds().appendSuffix("s")
            .toFormatter();

    protected DurationParam(String input) {
        super(input);
    }

    @Override
    protected String errorMessage(String input, Exception e) {
        return String.format("Unable to parse '%s' as resolution", input);
    }

    @Override
    protected Duration parse(String input) throws Exception {

        if (input.matches("^[\\d]+$")) {
            return Duration.seconds(Integer.valueOf(input));
        }
        
        Period period = formatter.parsePeriod(input);

        return Duration.seconds(period.toStandardDuration().getStandardSeconds());
    }

}
