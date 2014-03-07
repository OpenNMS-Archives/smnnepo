package org.opennms.netmgt.api.sample;

import java.util.ArrayList;

/**
 * JSON-serialized object representing an agent to run a collection on.
 */
public class ServiceAgent {
	
	@SuppressWarnings("serial")
	public static class ServiceAgentList extends ArrayList<ServiceAgent> {};
	
	private String id;
	private String m_address;
	private int m_port;
	private String m_serviceName;
	private String m_sysObjectId;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
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
	public String getSysObjectId() {
		return m_sysObjectId;
	}
	public void setSysObjectId(String sysObjectId) {
		this.m_sysObjectId = sysObjectId;
	}
}
