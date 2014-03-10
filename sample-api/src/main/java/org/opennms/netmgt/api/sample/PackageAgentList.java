package org.opennms.netmgt.api.sample;

import java.util.ArrayList;
import java.util.List;

import org.opennms.netmgt.config.collectd.Package;

public class PackageAgentList {
    private final Package m_package;
    private final List<ServiceAgent> m_agents = new ArrayList<ServiceAgent>();

    public PackageAgentList(Package packageService, List<ServiceAgent> agents) {
        super();
        m_package = packageService;
        if (agents != null) {
            m_agents.addAll(agents);
        }
    }

    public Package getPackage() {
        return m_package;
    }

    public List<ServiceAgent> getAgents() {
        return m_agents;
    }

    public Long getInterval() {
        return m_package.getServices().get(0).getInterval();
    }

    public String getServiceName() {
        return m_package.getServices().get(0).getName();
    }

}
