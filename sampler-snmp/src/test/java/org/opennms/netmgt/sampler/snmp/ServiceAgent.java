package org.opennms.netmgt.sampler.snmp;

import java.util.ArrayList;

public class ServiceAgent {
	
	public static class ServiceAgentList extends ArrayList<ServiceAgent> {};
	
	private String m_address;
	private int m_port;
	private String m_serviceName;
	
	public String getAddress() {
		return m_address;
	}
	public void setAddress(String address) {
		m_address = address;
	}
	public int getPort() {
		return m_port;
	}
	public void setPort(int port) {
		m_port = port;
	}
	public String getServiceName() {
		return m_serviceName;
	}
	public void setServiceName(String serviceName) {
		m_serviceName = serviceName;
	}
	

}
