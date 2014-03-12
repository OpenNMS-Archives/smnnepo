/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.sampler.config.snmp;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "snmp", name = "add-agent", description="Add an agent to the list of agents to collect data for.")
public class SnmpAddAgentCommand extends OsgiCommandSupport {
	
	private static final transient Logger LOG = LoggerFactory.getLogger(SnmpAddAgentCommand.class);
	
	@Option(name="-v", aliases="--version", description="SNMP version either 1, 2c or 3", required=true, multiValued=false)
	String m_version;
	
	@Option(name="-c", aliases="--community", description="SNMP community string to use, defaults to 'public'", required=false, multiValued=false)
	String m_community="public";
	
	@Option(name="-p", aliases="--port", description="port to use to address the agent defaults to 161", required=false, multiValued=false)
	int m_port = -1;
	
	@Option(name="-t", aliases="--timeout", description="timeout for communicating with agent.", required=false, multiValued=false)
	int m_timeout = -1;
	
	@Option(name="-r", aliases="--retries", description="retries for communicating with agent", required=false, multiValued=false)
	int m_retries = -1;
	
	@Argument(index = 0, name = "host", description = "hostname/ipAddress of the system to walk", required = true, multiValued = false)
	String m_host;
	
	@Argument(index = 1, name = "sysObjectId", description = "hostname/ipAddress of the system to walk", required = true, multiValued = false)
	String m_systemObjectId;
	
	
	private SnmpAgentRepository m_snmpAgentRepository;
	
	public void setSnmpAgentRepository(SnmpAgentRepository agentRepository) {
		m_snmpAgentRepository = agentRepository;
	}
	
    @Override
    protected Object doExecute() throws Exception {
    	
    	LOG.debug("snmp:add-agent -v {} -c {} -p {} -t {} -r {} {}", m_version, m_community, m_port, m_timeout, m_retries, m_host);
    	
    	SnmpAgent agent = m_port > 0 
    		? new SnmpAgent(new InetSocketAddress(m_host, m_port), m_systemObjectId) : 
    		new SnmpAgent(InetAddress.getByName(m_host), m_systemObjectId);

    	agent.setCommunity(m_community);
    	if (m_timeout > 0) {
    		agent.setTimeout(m_timeout);
    	}
    	if (m_retries > 0) {
    		agent.setRetries(m_retries);
    	}
    	
    	m_snmpAgentRepository.addAgent(agent);
    	
    	return null;
    	
    }
}
