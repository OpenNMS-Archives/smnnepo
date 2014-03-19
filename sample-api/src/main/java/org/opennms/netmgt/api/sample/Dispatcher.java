package org.opennms.netmgt.api.sample;

import org.apache.camel.InOnly;

public interface Dispatcher {
    @InOnly
    void dispatch(Agent agent);
}
