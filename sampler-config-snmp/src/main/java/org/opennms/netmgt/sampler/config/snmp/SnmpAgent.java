package org.opennms.netmgt.sampler.config.snmp;

import java.net.InetSocketAddress;

import org.opennms.netmgt.api.sample.Agent;

public class SnmpAgent extends Agent {
	private static final long serialVersionUID = 1L;

	public static final String SERVICE_NAME = "SNMP";

	public static final String PARAM_COMMUNITY = "community";
	public static final String PARAM_RETRIES = "retries";
	public static final String PARAM_SYSOBJECTID = "sysObjectId";
	public static final String PARAM_TIMEOUT = "timeout";
	public static final String PARAM_VERSION = "version";

	public SnmpAgent(Agent agent) {
		super(agent);
	}

	@Deprecated
	public SnmpAgent(InetSocketAddress agentAddress, String agentId) {
		super(agentAddress, SERVICE_NAME, agentId);
	}

	public String getSysObjectId() {
		return getParameter(PARAM_SYSOBJECTID);
	}

	public void setSysObjectId(String sysObjectId) {
		setParameter(PARAM_SYSOBJECTID, sysObjectId);
	}

	public String getCommunity() {
		return getParameter(PARAM_COMMUNITY);
	}

	public void setCommunity(String community) {
		setParameter(PARAM_COMMUNITY, community);
	}

	public String getVersion() {
		String value = getParameter(PARAM_VERSION);
		return value == null ? "v2c" : value;
	}

	public void setVersion(String version) {
		setParameter(PARAM_VERSION, version);
	}

	public int getTimeout() {
		String value = getParameter(PARAM_TIMEOUT);
		return value == null ? 800 : Integer.parseInt(value);
	}

	public void setTimeout(int timeout) {
		setParameter(PARAM_TIMEOUT, String.valueOf(timeout));
	}

	public int getRetries() {
		String value = getParameter(PARAM_RETRIES);
		return value == null ? 2 : Integer.parseInt(value);
	}

	public void setRetries(int retries) {
		setParameter(PARAM_RETRIES, String.valueOf(retries));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + super.toString() + ", name="+getId()
				+ ", community=" + getCommunity()
				+ ", version=" + getVersion()
				+ ", timeout=" + getTimeout()
				+ ", retries=" + getRetries()
				+"]";
	}
}
