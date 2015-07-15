/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2006-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.collection.sampler;

import java.io.File;
import java.net.InetAddress;

import org.opennms.core.utils.InetAddressUtils;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.collection.api.CollectionAgent;
//import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;

/**
 */
public class SamplerCollectionAgent implements CollectionAgent {

	private final Agent m_agent;

	public SamplerCollectionAgent(Agent agent) {
		if (agent == null) {
			throw new IllegalArgumentException("Agent value cannot be null");
		}
		m_agent = agent;
	}

    /**
     * <p>getNodeLabel</p>
     *
     * @return a {@link java.lang.String} object.
     */
    @Override
    public String getNodeLabel() {
        return null;
    }

	/**
	 * <p>isStoreByForeignSource</p>
	 * 
	 * @return a {@link java.lang.Boolean} object.
	 */
	@Override
	public Boolean isStoreByForeignSource() {
		return false;
	}

	/**
	 * <p>getHostAddress</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	@Override
	public String getHostAddress() {
		return InetAddressUtils.str(m_agent.getInetAddress());
	}

	/**
	 * <p>setSavedIfCount</p>
	 *
	 * @param ifCount a int.
	 */
	@Override
	public void setSavedIfCount(int ifCount) {
		// Do nothing.
	}

	/**
	 * <p>getNodeId</p>
	 *
	 * @return a int.
	 */
	@Override
	public int getNodeId() {
		Integer nodeId = m_agent.getNodeId();
		if (nodeId != null) {
			return nodeId;
		} else {
			return -1;
		}
	}

	/**
	 * <p>getForeignSource</p>
	 * 
	 * @return a {@link java.lang.String} object.
	 */
	@Override
	public String getForeignSource() {
		return m_agent.getForeignSource();
	}

	/**
	 * <p>getForeignId</p>
	 * 
	 * @return a {@link java.lang.String} object.
	 */
	@Override
	public String getForeignId() {
		return m_agent.getForeignId();
	}

	/**
	 * <p>getStorageDir</p>
	 * 
	 * @return a {@link java.io.File} object.
	 */
	@Override
	public File getStorageDir() {
		int nodeId = getNodeId(); 
		if (nodeId > 0) {
			return new File(String.valueOf(nodeId));
		} else {
			return null;
		}
	}

	/**
	 * <p>getSysObjectId</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	@Override
	public String getSysObjectId() {
		//return m_agent.getParameters().get(SnmpAgent.PARAM_SYSOBJECTID);
		return m_agent.getParameters().get("sysObjectId");
	}

	/**
	 * <p>toString</p>
	 *
	 * @return a {@link java.lang.String} object.
	 */
	@Override
	public String toString() {
		return super.toString();
	}

	/**
	 * <p>getSavedSysUpTime</p>
	 *
	 * @return a long.
	 */
	@Override
	public long getSavedSysUpTime() {
		return 0;
	}

	/**
	 * <p>setSavedSysUpTime</p>
	 *
	 * @param sysUpTime a long.
	 */
	@Override
	public void setSavedSysUpTime(long sysUpTime) {
		// Do nothing.
	}

	@Override
	public int getType() {
		return org.opennms.netmgt.poller.NetworkInterface.TYPE_INET;
	}

	/**
	 * <p>getAddress</p>
	 *
	 * @return a {@link java.net.InetAddress} object.
	 */
	@Override
	public InetAddress getAddress() {
		return m_agent.getInetAddress();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <V> V getAttribute(String property) {
		return (V)m_agent.getParameter(property);
	}

	@Override
	public Object setAttribute(String property, Object value) {
		return null;
	}

}
