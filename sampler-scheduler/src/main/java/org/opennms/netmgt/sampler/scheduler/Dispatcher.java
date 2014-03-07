package org.opennms.netmgt.sampler.scheduler;

import org.apache.camel.InOnly;

public interface Dispatcher {
    @InOnly
    public void dispatch(CollectionRequest request);
}
