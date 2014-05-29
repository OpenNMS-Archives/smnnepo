/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.trapd.camel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.opennms.core.concurrent.LogPreservingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 */
public class CamelTrapEventProcessor implements Processor {

	private static final Logger LOG = LoggerFactory.getLogger(CamelTrapEventProcessor.class);

	private final ExecutorService m_executor = Executors.newFixedThreadPool(5, new LogPreservingThreadFactory(getClass().getSimpleName(), 5, false));

	@Override
	public void process(Exchange exchange) throws Exception {
		final String message = exchange.getIn().getBody(String.class);

		m_executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				final SnmpMessageParser parser = new SnmpMessageParser();
				SnmpMessage snmpAgent = parser.parse(message);
				LOG.debug("Received trap: {}", snmpAgent.toString());
				// TODO: Convert the message into an OpenNMS event
				return null;
			}
		});
	}

	/**
	 * TODO: Make this into JAXB?
	 */
	public static class SnmpMessage {
		private String enterprise;
		private String agentAddr;
		private int genericTrap;
		private int specificTrap;
		private long timestamp;
		private Map<String,String> m_oids = new HashMap<String,String>();

		public String getEnterprise() {
			return enterprise;
		}

		public void setEnterprise(String enterprise) {
			this.enterprise = enterprise;
		}

		public String getAgentAddr() {
			return agentAddr;
		}

		public void setAgentAddr(String agentAddr) {
			this.agentAddr = agentAddr;
		}

		public int getGenericTrap() {
			return genericTrap;
		}

		public void setGenericTrap(int genericTrap) {
			this.genericTrap = genericTrap;
		}

		public int getSpecificTrap() {
			return specificTrap;
		}

		public void setSpecificTrap(int specificTrap) {
			this.specificTrap = specificTrap;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}

		public void putOid(String oid, String value) {
			m_oids.put(oid, value);
		}

		public Map<String,String> getOids() {
			return Collections.unmodifiableMap(m_oids);
		}

		@Override
		public String toString() {
			return new ToStringBuilder(this)
				.append("enterprise", getEnterprise())
				.append("agentAddr", getAgentAddr())
				.append("genericTrap", getGenericTrap())
				.append("specificTrap", getSpecificTrap())
				.append("timestamp", getTimestamp())
				.append("oids", getOids())
				.toString()
			;
		}
	}

	/**
	 * This class is NOT thread-safe.
	 */
	public static class SnmpMessageParser extends DefaultHandler {

		private SnmpMessage snmpMessage;
		private String temp;
		private String oid;
		private String value;

		private final SAXParser parser;

		public SnmpMessageParser() {
			try {
				SAXParserFactory spfac = SAXParserFactory.newInstance();
				parser = spfac.newSAXParser();
			} catch (ParserConfigurationException e) {
				throw new IllegalStateException(e.getMessage(), e);
			} catch (SAXException e) {
				throw new IllegalStateException(e.getMessage(), e);
			}
		}

		public SnmpMessage parse(String message) throws SAXException, IOException {
			snmpMessage = new SnmpMessage();
			parser.parse(new ByteArrayInputStream(message.getBytes("UTF-8")), this);
			return snmpMessage;
		}

		@Override
		public void characters(char[] buffer, int start, int length) {
			temp = new String(buffer, start, length);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("entry")) {
				oid = null;
				value = null;
			}
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {

			if (qName.equalsIgnoreCase("enterprise")) {
				snmpMessage.setEnterprise(temp);
			} else if (qName.equalsIgnoreCase("agent-addr")) {
				snmpMessage.setAgentAddr(temp);
			} else if (qName.equalsIgnoreCase("generic-trap")) {
				snmpMessage.setGenericTrap(Integer.parseInt(temp));
			} else if (qName.equalsIgnoreCase("specific-trap")) {
				snmpMessage.setSpecificTrap(Integer.parseInt(temp));
			} else if (qName.equalsIgnoreCase("time-stamp")) {
				snmpMessage.setTimestamp(Long.parseLong(temp));
			} else if (qName.equalsIgnoreCase("entry")) {
				if (oid == null && value == null) {
					// Do nothing, we already stored the value during another endElement() call
				} else if (oid != null && value != null) {
					snmpMessage.putOid(oid, value);
				} else {
					throw new IllegalStateException(String.format("Null OID or value found: %s -> %s", oid, value));
				}
			} else if (qName.equalsIgnoreCase("oid")) {
				oid = temp;
			} else if (qName.equalsIgnoreCase("value")) {
				value = temp;
			}
		}
	}
}
