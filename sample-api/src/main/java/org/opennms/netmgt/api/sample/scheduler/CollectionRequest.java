package org.opennms.netmgt.api.sample.scheduler;

import org.opennms.netmgt.api.sample.Agent;


public class CollectionRequest {
    private String m_service;
    private Agent m_agent;

    public CollectionRequest() {
        
    }

    public CollectionRequest(final String service, final Agent agent) {
        m_service = service;
        m_agent = agent;
    }

    @Override
    public String toString() {
        return "CollectionRequest [service=" + m_service + ", agent=" + m_agent + "]";
    }

}
