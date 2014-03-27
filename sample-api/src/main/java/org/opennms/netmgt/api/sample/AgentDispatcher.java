package org.opennms.netmgt.api.sample;

import org.apache.camel.InOnly;

public interface AgentDispatcher {
    @InOnly
    void dispatch(Agent agent);
}
