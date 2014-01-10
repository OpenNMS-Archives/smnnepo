package org.opennms.netmgt.sampler.config.snmp;

import java.io.IOException;
import java.io.InputStream;
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
				s_log.debug("Unmarshalling {} as {}", url, declaredType);
				urlStream = url.openStream();
				JAXBElement<T> jaxbElement = m_unmarshaller.unmarshal(new StreamSource(urlStream), declaredType);
				return jaxbElement == null ? null : jaxbElement.getValue();		
			} finally {
				if (urlStream != null) {
					urlStream.close();
				}
			}
		}
	}
	
	private static Logger s_log = LoggerFactory.getLogger(SnmpMetricRepository.class); 
	
	private final DataCollectionConfig m_dataCollectionConfig;
	
	private static URL[] findEntries(Bundle bundle, String dir) {
		List<URL> urls = new ArrayList<URL>();
		
		Enumeration<URL> en = bundle.findEntries(dir, null, true);
		while(en.hasMoreElements()) {
			urls.add(en.nextElement());
		}
		
		return urls.toArray(new URL[urls.size()]);
	}
	
	public SnmpMetricRepository(String dataCollectionPath, String dataCollectionDir, Bundle bundle) throws Exception {
		this(bundle.getEntry(dataCollectionPath), findEntries(bundle, dataCollectionDir));
	}
	
	public SnmpMetricRepository(URL dataCollectionConfigURL, URL... dataCollectionGroupURLs) throws Exception {
		Parser parser = new Parser();
		
		Map<String, DataCollectionGroup> dataCollectionGroups = new HashMap<String, DataCollectionGroup>();
		
		
		Map<String, ResourceType> typeMap = new HashMap<String, ResourceType>();
		Map<String, Table> tableMap = new HashMap<String, Table>();
		Map<String, Group> groupMap = new HashMap<String, Group>();

		for(URL dataCollectionGroupURL : dataCollectionGroupURLs) {
			DataCollectionGroup group = parser.getDataCollectionGroup(dataCollectionGroupURL);
			group.gatherSymbols(typeMap, tableMap, groupMap);
			dataCollectionGroups.put(group.getName(), group);
		}
		
		
		for(DataCollectionGroup group : dataCollectionGroups.values()) {
			group.initialize(typeMap, tableMap, groupMap);
		}
				

		m_dataCollectionConfig = parser.getDataCollectionConfig(dataCollectionConfigURL);
		m_dataCollectionConfig.initialize(dataCollectionGroups);
		
	}
	
	
	public SnmpCollectionRequest createRequestForAgent(SnmpAgent agent) {
		
		SnmpCollectionRequest request = new SnmpCollectionRequest(agent);
		m_dataCollectionConfig.fillRequest(request);
		return request;

	}

	public Set<Metric> getMetrics(String groupName) {
		return m_dataCollectionConfig.getMetricsForGroup(groupName);
	}
	
	public Metric getMetric(String metricName) {
		return m_dataCollectionConfig.getMetric(metricName);
	}
}
