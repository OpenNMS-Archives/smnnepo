package org.opennms.minion.api;

import java.util.List;

public interface MinionContainerManager {
    public List<String> getInstanceNames(final boolean includeRoot);
    public boolean isRootInstance(String currentInstance);

    public void createInstance(MinionContainer minionContainer) throws MinionException;
    public void destroyInstance(final String instanceName)      throws MinionException;
    public void startInstance(final String instanceName)        throws MinionException;
    public void stopInstance(final String instanceName)         throws MinionException;
}
