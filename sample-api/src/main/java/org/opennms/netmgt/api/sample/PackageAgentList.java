package org.opennms.netmgt.api.sample;

import java.util.List;

import org.opennms.netmgt.config.collectd.Package;

public class PackageAgentList {
    private Package m_package;
    private List<ServiceAgent> m_agents;

    public PackageAgentList(Package packageService, List<ServiceAgent> agents) {
        super();
        m_package = packageService;
        m_agents = agents;
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
