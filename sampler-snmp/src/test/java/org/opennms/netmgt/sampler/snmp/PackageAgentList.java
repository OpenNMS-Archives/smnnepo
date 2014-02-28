package org.opennms.netmgt.sampler.snmp;

import java.util.List;

public class PackageAgentList {
	private PackageService m_packageService;
	private List<ServiceAgent> m_agents;
	
	public PackageAgentList(PackageService packageService, List<ServiceAgent> agents) {
		super();
		m_packageService = packageService;
		m_agents = agents;
	}

	public PackageService getPackageService() {
		return m_packageService;
	}

	public List<ServiceAgent> getAgents() {
		return m_agents;
	}

}