package org.opennms.minion.controller.internal;

import org.apache.camel.InOnly;

@InOnly
public class DefaultMinionMessageSender {
    private final String m_endpointUri;
    
    public DefaultMinionMessageSender(final String endpointUri) {
        m_endpointUri = endpointUri;
    }
    
    public String getEndpointUri() {
        return m_endpointUri;
    }
}
