package org.opennms.netmgt.api.sample.support;

import org.apache.camel.InOnly;
import org.opennms.netmgt.api.sample.scheduler.CollectionRequest;

public interface Dispatcher {
    @InOnly
    public void dispatch(CollectionRequest request);
}