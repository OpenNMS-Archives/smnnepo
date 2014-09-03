package org.opennms.minion.api;

import java.util.Map;

public interface MinionContainerConfiguration {
    public String getPid();
    public boolean containsKey(String key);
    public String getProperty(final String key);
    public Map<String,String> getProperties();
}
