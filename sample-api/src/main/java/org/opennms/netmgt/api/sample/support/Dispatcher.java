package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.opennms.netmgt.api.sample.Agent;

public interface Dispatcher {
    @InOnly
    void dispatch(Agent agent);
}
