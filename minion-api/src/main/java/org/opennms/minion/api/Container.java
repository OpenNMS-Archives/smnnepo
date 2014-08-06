package org.opennms.minion.api;

import java.util.List;

public interface Container {
    public String getName();
    public List<String> getFeatures();
    public String getScript();
    public List<String> getScriptArguments();
    public List<ContainerConfiguration> getConfigurations();
}
