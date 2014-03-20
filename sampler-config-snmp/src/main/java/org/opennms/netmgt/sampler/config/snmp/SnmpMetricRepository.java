package org.opennms.netmgt.sampler.config.snmp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricRepository;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpMetricRepository implements MetricRepository, CollectionConfiguration<SnmpAgent, SnmpCollectionRequest> {
	
	private static class Parser {
		Unmarshaller m_unmarshaller;
		
		public Parser() throws JAXBException {
			m_unmarshaller = JAXBContext.newInstance(DataCollectionConfig.class, DataCollectionGroup.class).createUnmarshaller();
		}
		
		public DataCollectionConfig getDataCollectionConfig(URL path) throws JAXBException, IOException {
			return parse(path, DataCollectionConfig.class);
		}
		
		public DataCollectionGroup getDataCollectionGroup(URL path) throws JAXBException, IOException {
			return parse(path, DataCollectionGroup.class);
		}
		
		private <T> T parse(URL url, Class<T> declaredType) throws JAXBException, IOException {
			InputStream urlStream = null;
			try {
				LOG.debug("Unmarshalling {} as {}", url, declaredType);
				urlStream = url.openStream();
				JAXBElement<T> jaxbElement = m_unmarshaller.unmarshal(new StreamSource(urlStream), declaredType);
				return jaxbElement == null ? null : jaxbElement.getValue();		
			} catch (IOException e) {
				LOG.warn("Could not unmarshal " + url.toString() + " as " + declaredType.getName(), e);
				return null;
			} finally {
				if (urlStream != null) {
					urlStream.close();
				}
			}
		}
	}
	
	private static Logger LOG = LoggerFactory.getLogger(SnmpMetricRepository.class); 
	
	private final URL m_dataCollectionConfigURL;
	private final URL[] m_dataCollectionGroupURLs;
	
	private DataCollectionConfig m_dataCollectionConfig;
	
	private static URL[] findEntries(Bundle bundle, String dir) {
		List<URL> urls = new ArrayList<URL>();
		
		Enumeration<URL> en = bundle.findEntries(dir, null, true);
		while(en.hasMoreElements()) {
			urls.add(en.nextElement());
		}
		
		return urls.toArray(new URL[urls.size()]);
	}

	/**
	 * @deprecated Should be initialized with URIs for config files instead
	 */
	public SnmpMetricRepository(String dataCollectionPath, String dataCollectionDir, Bundle bundle) throws Exception {
		this(bundle.getEntry(dataCollectionPath), findEntries(bundle, dataCollectionDir));
	}
	
	
	
	public SnmpMetricRepository(URL dataCollectionConfigURL, URL... dataCollectionGroupURLs) throws Exception {
		m_dataCollectionConfigURL = dataCollectionConfigURL;
		m_dataCollectionGroupURLs = dataCollectionGroupURLs;
		refresh();
	}
	
	/**
	 * Load the specified data collection config file and then load all of the collection 
	 * groups from a comma-separated string value.
	 * 
	 * @param dataCollectionConfigURL
	 * @param groupURLs
	 * @throws Exception
	 */
	public SnmpMetricRepository(URL dataCollectionConfigURL, String groupURLs) throws Exception {
		m_dataCollectionConfigURL = dataCollectionConfigURL;
		List<URL> urls = new ArrayList<URL>();
		if (groupURLs != null && !"".equals(groupURLs)) {
			for (String url : groupURLs.split(",")) {
				try {
					urls.add(new URL(url.trim()));
				} catch (MalformedURLException e) {
					LOG.warn("Invalid URL specified in {} configuration: {}", getClass().getSimpleName(), url);
				}
			}
		}
		m_dataCollectionGroupURLs = urls.toArray(new URL[0]);
		refresh();
	}
	
	public void refresh() throws JAXBException, IOException {
		
		Parser parser = new Parser();
		
		Map<String, DataCollectionGroup> dataCollectionGroups = new HashMap<String, DataCollectionGroup>();
		
		
		Map<String, ResourceType> typeMap = new HashMap<String, ResourceType>();
		Map<String, Table> tableMap = new HashMap<String, Table>();
		Map<String, Group> groupMap = new HashMap<String, Group>();

		if (m_dataCollectionGroupURLs != null) {
			for(URL dataCollectionGroupURL : m_dataCollectionGroupURLs) {
				DataCollectionGroup group = parser.getDataCollectionGroup(dataCollectionGroupURL);
				if (group != null) {
					group.gatherSymbols(typeMap, tableMap, groupMap);
					dataCollectionGroups.put(group.getName(), group);
				}
			}
		}
		
		for(DataCollectionGroup group : dataCollectionGroups.values()) {
			group.initialize(typeMap, tableMap, groupMap);
		}
				

		m_dataCollectionConfig = parser.getDataCollectionConfig(m_dataCollectionConfigURL);
		if (m_dataCollectionConfig != null) {
			m_dataCollectionConfig.initialize(dataCollectionGroups);
		}
	}
	
	
	public SnmpCollectionRequest createRequestForAgent(SnmpAgent agent) {
		
		SnmpCollectionRequest request = new SnmpCollectionRequest(agent);
		if (m_dataCollectionConfig != null) {
			m_dataCollectionConfig.fillRequest(request);
		}
		return request;
	}

	public Set<Metric> getMetrics(String groupName) {
		if (m_dataCollectionConfig == null) {
			return null;
		} else {
			return m_dataCollectionConfig.getMetricsForGroup(groupName);
		}
	}
	
	public Metric getMetric(String metricName) {
		if (m_dataCollectionConfig == null) {
			return null;
		} else {
			return m_dataCollectionConfig.getMetric(metricName);
		}
	}
}
