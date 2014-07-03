package org.opennms.newts.gsod;


import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.OneArgumentOptionHandler;
import org.kohsuke.args4j.spi.Setter;
import org.opennms.newts.api.Resource;


public class ResourceOptionHandler extends OneArgumentOptionHandler<Resource> {

    public ResourceOptionHandler(CmdLineParser parser, OptionDef option, Setter<? super Resource> setter) {
        super(parser, option, setter);
    }

    @Override
    protected Resource parse(String argument) throws NumberFormatException, CmdLineException {
        return new Resource(argument);
    }

    @Override
    public String getDefaultMetaVariable() {
        return "<resource>";
    }

}
