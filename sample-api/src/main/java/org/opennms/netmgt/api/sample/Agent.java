package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.core.network.IPAddress;
import org.opennms.core.network.InetAddressXmlAdapter;
import org.opennms.core.xml.JaxbMapAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement(name="agent")
@XmlAccessorType(XmlAccessType.NONE)
public class Agent implements Serializable {
    private static final long serialVersionUID = 5L;
    private static final Logger LOG = LoggerFactory.getLogger(Agent.class);

    @XmlElement(name="address")
    @XmlJavaTypeAdapter(InetAddressXmlAdapter.class)
    private final InetAddress m_agentAddress;

    @XmlElement(name="port")
    private final Integer m_port;

    @XmlElement(name="serviceName")
    private final String m_serviceName;

    @XmlElement(name="id")
    private final String m_agentId;

    @XmlElement(name = "parameters")
    @XmlJavaTypeAdapter(JaxbMapAdapter.class)
    protected final Map<String,String> m_parameters = new HashMap<String,String>();

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

    /**
     * Copy constructor.
     */
    public Agent(final Agent agent) {
        this(new InetSocketAddress(agent.getInetAddress(), agent.getPort()), agent.getServiceName(), agent.getId());
        m_parameters.putAll(agent.getParameters());
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

    public Agent(final IPAddress ipAddress, final int port, final String serviceName, final String agentId) {
        this(new InetSocketAddress(ipAddress.toInetAddress(), port), serviceName, agentId);
    }

    public String getId() {
        if (m_agentId == null) {
            return getFauxId();
        }
        return m_agentId;
    }

    public InetSocketAddress getAgentAddress() {
        return new InetSocketAddress(m_agentAddress, m_port);
    }

    public InetAddress getInetAddress() {
        return m_agentAddress;
    }

    public Integer getNodeId() {
        if (m_parameters != null && m_parameters.containsKey("nodeId")) {
            final String nodeId = m_parameters.get("nodeId");
            if (nodeId != null && !nodeId.trim().isEmpty()) {
                try {
                    return Integer.valueOf(nodeId);
                } catch (final NumberFormatException e) {
                    LOG.warn("Node ID ({}) exists, but is not a number!", nodeId);
                }
            }
        }
        return null;
    }

    public String getForeignSource() {
        if (m_parameters != null && m_parameters.containsKey("foreignSource") && !m_parameters.get("foreignSource").trim().isEmpty()) {
            return m_parameters.get("foreignSource");
        }
        return null;
    }

    public String getForeignId() {
        if (m_parameters != null && m_parameters.containsKey("foreignId") && !m_parameters.get("foreignId").trim().isEmpty()) {
            return m_parameters.get("foreignId");
        }
        return null;
    }

    public int getPort() {
        return m_port;
    }

    public String getServiceName() {
        return m_serviceName;
    }

    public Map<String,String> getParameters() {
        return Collections.unmodifiableMap(m_parameters);
    }

    public String getParameter(String key) {
        return m_parameters.get(key);
    }

    public void setParameter(String key, String value) {
        m_parameters.put(key, value);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[id=" + m_agentId + ", address=" + m_agentAddress + ", port=" + m_port + ", serviceName=" + m_serviceName + ", parameters=" + m_parameters + "]";
    };

    private String getFauxId() {
        final StringBuilder sb = new StringBuilder();
        sb.append(m_serviceName).append(":");
        if (m_agentAddress != null) {
            sb.append(m_agentAddress.getHostAddress());
        } else {
            sb.append("null");
        }
        sb.append(":").append(m_port);
        return sb.toString();
    }
}
