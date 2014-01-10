package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Agent implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String m_protocol;
	private final InetSocketAddress m_agentAddress;
	private final String m_agentId;

	public Agent(InetSocketAddress agentAddress, String protocol, String agentId) {
		m_agentAddress = agentAddress;
		m_protocol = protocol;
		m_agentId = (agentId == null) ? getFauxId() : agentId;
	}

	@Deprecated
	public Agent(InetSocketAddress agentAddress, String protocol) {
		this(agentAddress, protocol, null);
	}

	@Deprecated
	public Agent(InetAddress addr, int port, String protocol) {
		this(new InetSocketAddress(addr, port), protocol);
	}

	public String getProtocol() {
		return m_protocol;
	}

	public InetSocketAddress getAgentAddress() {
		return m_agentAddress;
	}

	public InetAddress getInetAddress() {
		return m_agentAddress.getAddress();
	}

	public int getPort() {
		return m_agentAddress.getPort();
	}

	public String getId() {
		return m_agentId;
	}

	private String getFauxId() {
		return m_protocol+":"+ m_agentAddress.getAddress().getHostAddress()+":" + m_agentAddress.getPort();
	}
}
