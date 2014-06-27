package org.opennms.newts.stress;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.api.Duration;

public class DurationOptionHandler extends OneArgumentOptionHandler<Duration> {

    public DurationOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Duration> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Duration parse(String argument) throws NumberFormatException, CmdLineException {
        return Duration.seconds(Integer.valueOf(argument));
    }

    @Override
    public String getDefaultMetaVariable() {
        return "<duration>";
    }

}
