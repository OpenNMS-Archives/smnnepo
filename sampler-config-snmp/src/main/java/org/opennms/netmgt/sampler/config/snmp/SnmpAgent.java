package org.opennms.netmgt.sampler.config.snmp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.opennms.netmgt.api.sample.Agent;

public class SnmpAgent extends Agent {
	private static final long serialVersionUID = 1L;

	private String m_systemObjId;
	private String m_community;
	private String m_version = "v2c";
	private int m_timeout = 800;
	private int m_retries = 2;

	public SnmpAgent(InetSocketAddress agentAddress, String systemObjId, String agentId) {
		super(agentAddress, "SNMP", agentId);
		m_systemObjId = systemObjId;
	}
	
	@Deprecated
	public SnmpAgent(InetSocketAddress agentAddress, String systemObjId) {
		this(agentAddress, systemObjId, null);
	}
	
	@Deprecated
	public SnmpAgent(InetAddress agentAddr, String systemObjId) {
		this(new InetSocketAddress(agentAddr, 161), systemObjId);
	}
	
	public String getSystemObjId() {
		return m_systemObjId;
	}

	public void setSystemObjId(String systemObjId) {
		m_systemObjId = systemObjId;
	}
	
	public String getCommunity() {
		return m_community;
	}

	public void setCommunity(String community) {
		m_community = community;
	}

	public String getVersion() {
		return m_version;
	}

	public void setVersion(String version) {
		m_version = version;
	}

	public int getTimeout() {
		return m_timeout;
	}

	public void setTimeout(int timeout) {
		m_timeout = timeout;
	}

	public int getRetries() {
		return m_retries;
	}

	public void setRetries(int retries) {
		m_retries = retries;
	}

	@Override
	public String toString() {
		return "SnmpAgent[name="+getId()
				+ ", community=" + m_community
				+ ", version=" + m_version
				+ ", timeout=" + m_timeout
				+ ", retries=" + m_retries
				+ ", sysOid="+m_systemObjId
				+"]";
	}

}
