package org.opennms.minion.api;

import java.util.Map;

public interface ContainerConfiguration {
    public String getPid();
    public Map<String,String> getProperties();
}
