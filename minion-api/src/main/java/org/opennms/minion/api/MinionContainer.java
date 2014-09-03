package org.opennms.minion.api;

import java.util.List;

public interface MinionContainer {
    public String getName();
    public String getPid();
    public List<String> getFeatures();
    public List<String> getFeatureRepositories();
    public String getScript();
    public List<String> getScriptArguments();
    public List<MinionContainerConfiguration> getConfigurations();
}
