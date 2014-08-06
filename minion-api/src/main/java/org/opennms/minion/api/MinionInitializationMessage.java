package org.opennms.minion.api;

import java.util.List;

public interface MinionInitializationMessage extends MinionMessage {
    public String getMinionId();
    public List<String> getFeatureRepositories();
    public List<Container> getContainers();
}
