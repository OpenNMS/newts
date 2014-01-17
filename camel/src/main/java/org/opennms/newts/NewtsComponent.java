package org.opennms.newts;


import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;


/**
 * Represents the component that manages {@link NewtsEndpoint}.
 */
public class NewtsComponent extends DefaultComponent {

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        Endpoint endpoint = new NewtsEndpoint(uri, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

}
