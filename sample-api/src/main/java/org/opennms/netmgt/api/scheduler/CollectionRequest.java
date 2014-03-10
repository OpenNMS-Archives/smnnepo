package org.opennms.netmgt.api.scheduler;

import org.opennms.netmgt.api.sample.ServiceAgent;


public class CollectionRequest {
    private String m_service;
    private ServiceAgent m_agent;

    public CollectionRequest() {
        
    }

    public CollectionRequest(final String service, final ServiceAgent agent) {
        m_service = service;
        m_agent = agent;
    }

    @Override
    public String toString() {
        return "CollectionRequest [service=" + m_service + ", agent=" + m_agent + "]";
    }

}
