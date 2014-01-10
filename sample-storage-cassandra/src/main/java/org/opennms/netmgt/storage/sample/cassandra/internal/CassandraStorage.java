package org.opennms.netmgt.storage.sample.cassandra.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Query;
import com.datastax.driver.core.Session;

public abstract class CassandraStorage {
	private static final Logger LOG = LoggerFactory.getLogger(CassandraStorage.class);

	// Schema element constants.
	protected static final String	T_SAMPLES		= "samples";
	protected static final String	T_METADATA		= "metrics";
	protected static final String	T_RESOURCES		= "resources";
	protected static final String	T_METRICS_IDX	= "metrics_x_resource";
	protected static final String	T_RESOURCES_IDX	= "resources_x_endpoint";
	protected static final String	F_METRIC		= "metric";
	protected static final String	F_TYPE			= "type";
	protected static final String	F_GROUP			= "group";
	protected static final String	F_RESOURCE		= "resource";
	protected static final String	F_COLLECTED_AT	= "collected_at";
	protected static final String	F_VALUE			= "value";
	protected static final String	F_ENDPOINT		= "endpoint";
	protected static final String	F_ATTRIBUTE		= "attribute";

	protected final String m_cassandraHost;
	protected final int m_cassandraPort;
	protected final String m_cassandraKeyspace;
	protected final Session m_session;

	public CassandraStorage(String cassandraHost, int cassandraPort, String cassandraKeyspace) {
		m_cassandraHost = cassandraHost;
		m_cassandraPort = cassandraPort;
		m_cassandraKeyspace = cassandraKeyspace;

		LOG.info("Connecting to {}:{}...", m_cassandraHost, m_cassandraPort);

		Cluster cluster = Cluster.builder().withPort(m_cassandraPort).addContactPoint(m_cassandraHost).build();
		m_session = cluster.connect(m_cassandraKeyspace);

		LOG.info("Connected.");
	}

	/*
	 * FIXME: Needs exception handling; Everything the driver throws extends
	 * RuntimeException in some way, but there are likely situations that can be
	 * better handled than by propagating. Read/write timeout exceptions
	 * perhaps? Unavailable exceptions?
	 */
	protected com.datastax.driver.core.ResultSet executeQuery(Query query) {
		LOG.trace("Executing CQL Query: {}", query);
		return m_session.execute(query);
	}
}
