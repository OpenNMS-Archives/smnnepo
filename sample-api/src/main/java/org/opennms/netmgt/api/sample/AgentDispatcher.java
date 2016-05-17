package org.opennms.netmgt.api.sample;

import org.apache.camel.InOnly;

@InOnly
public interface AgentDispatcher {
    void dispatch(Agent agent);
}
