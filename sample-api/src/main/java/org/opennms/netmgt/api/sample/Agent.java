package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.core.network.IPAddress;
import org.opennms.core.network.InetAddressXmlAdapter;

@XmlRootElement(name="agent")
@XmlAccessorType(XmlAccessType.NONE)
public class Agent implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlElement(name="address")
    @XmlJavaTypeAdapter(InetAddressXmlAdapter.class)
    private final InetAddress m_agentAddress;

    @XmlElement(name="port")
    private final Integer m_port;

    @XmlElement(name="serviceName")
    private final String m_serviceName;

    @XmlElement(name="id")
    private final String m_agentId;

    @XmlElement(name = "sysObjectId")
    protected String m_sysObjectId;

    public Agent() {
        m_serviceName = null;
        m_agentAddress = null;
        m_port = null;
        m_agentId = null;
    }

    public Agent(final InetSocketAddress agentAddress, final String serviceName, final String agentId) {
        m_agentAddress = agentAddress.getAddress();
        m_port = agentAddress.getPort();
        m_serviceName = serviceName;
        m_agentId = (agentId == null) ? getFauxId() : agentId;
    }

    @Deprecated
    public Agent(final InetSocketAddress agentAddress, final String serviceName) {
        this(agentAddress, serviceName, null);
    }

    @Deprecated
    public Agent(final InetAddress addr, final int port, final String serviceName) {
        m_agentAddress = addr;
        m_port = port;
        m_serviceName = serviceName;
        m_agentId = getFauxId();
    }

    public Agent(final IPAddress ipAddress, final int port, final String serviceName) {
        this(new InetSocketAddress(ipAddress.toInetAddress(), port), serviceName, null);
    }

    public String getId() {
        return m_agentId;
    }

    public InetSocketAddress getAgentAddress() {
        return new InetSocketAddress(m_agentAddress, m_port);
    }

    public InetAddress getInetAddress() {
        return m_agentAddress;
    }

    public int getPort() {
        return m_port;
    }

    public String getServiceName() {
        return m_serviceName;
    }

    public String getSysObjectId() {
        return m_sysObjectId;
    }

    public void setSysObjectId(String sysObjectId) {
        this.m_sysObjectId = sysObjectId;
    }

    @Override
    public String toString() {
        return "SnmpAgent [id=" + m_agentId + ", address=" + m_agentAddress + ", port=" + m_port + ", serviceName=" + m_serviceName + ", sysObjectId=" + m_sysObjectId + "]";
    };

    private String getFauxId() {
        return m_serviceName+":"+ m_agentAddress.getHostAddress()+":" + m_port;
    }

    public static class AgentList extends ArrayList<Agent> {
        private static final long serialVersionUID = 1L;
    }
}
