package org.opennms.netmgt.sampler.config.snmp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.opennms.netmgt.api.sample.CollectionConfiguration;
import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricRepository;
import org.osgi.framework.Bundle;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpMetricRepository implements MetricRepository, CollectionConfiguration<SnmpAgent, SnmpCollectionRequest>, ManagedService {
    private static Logger LOG = LoggerFactory.getLogger(SnmpMetricRepository.class); 

    private URL m_dataCollectionConfigURL;
    private URL[] m_dataCollectionGroupURLs;
    private String m_username = "admin";
    private String m_password = "admin";

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
        m_dataCollectionGroupURLs = parseUrlArray(groupURLs);
        refresh();
    }

    public final void refresh() throws JAXBException, IOException {
        LOG.debug("refresh() called");
        final Parser parser = new Parser(m_username, m_password);

        final DataCollectionInitializationCache cache = new DataCollectionInitializationCache();

        if (m_dataCollectionGroupURLs != null) {
            LOG.debug("parsing datacollection group URLs: {}", Arrays.asList(m_dataCollectionGroupURLs));
            for(final URL dataCollectionGroupURL : m_dataCollectionGroupURLs) {
                final DataCollectionGroup group = parser.getDataCollectionGroup(dataCollectionGroupURL);
                if (group != null) {
                    cache.addDataCollectionGroup(group);
                    group.gatherSymbols(cache);
                }
            }
        }

        LOG.debug("pre-initializing {} groups", cache.getDataCollectionGroups().size());
        for (final DataCollectionGroup group : cache.getDataCollectionGroups()) {
            group.initialize(cache);
        }

        LOG.debug("parsing datacollection config: {}", m_dataCollectionConfigURL);
        final DataCollectionConfig config = parser.getDataCollectionConfig(m_dataCollectionConfigURL);
        if (config == null) {
            LOG.warn("Data collection config is null! ({})", m_dataCollectionConfigURL);
        } else {
            LOG.debug("initializing datacollection config");
            config.initialize(cache);
        }

        LOG.debug("finished initializing config");
        m_dataCollectionConfig = config;
    }


    public SnmpCollectionRequest createRequestForAgent(final SnmpAgent agent) {
        final SnmpCollectionRequest request = new SnmpCollectionRequest(agent);
        if (m_dataCollectionConfig == null) {
            LOG.warn("Unable to create request for agent {}, no data collection config!", agent);
        } else {
            m_dataCollectionConfig.fillRequest(request);
        }
        return request;
    }

    public Set<Metric> getMetrics(final String groupName) {
        if (m_dataCollectionConfig == null) {
            LOG.warn("Unable to get metrics for group {}, no data collection config!", groupName);
            return null;
        } else {
            return m_dataCollectionConfig.getMetricsForGroup(groupName);
        }
    }

    public Metric getMetric(final String metricName) {
        if (m_dataCollectionConfig == null) {
            LOG.warn("Unable to get metric {}, no data collection config!", metricName);
            return null;
        } else {
            return m_dataCollectionConfig.getMetric(metricName);
        }
    }
    
    public String getUsername() {
        return m_username;
    }
    public void setUsername(final String username) {
        m_username = username;
    }
    
    public String getPassword() {
        return m_password;
    }
    public void setPassword(final String password) {
        m_password = password;
    }

    private static URL[] parseUrlArray(String groupURLs) {
        List<URL> urls = new ArrayList<URL>();
        if (groupURLs != null && !"".equals(groupURLs)) {
            for (String url : groupURLs.split(",")) {
                try {
                    urls.add(new URL(url.trim()));
                } catch (MalformedURLException e) {
                    LOG.warn("Invalid URL specified in {} configuration: {}", SnmpMetricRepository.class.getSimpleName(), url);
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null) {
            return;
        }

        boolean refresh = false;

        String property = (String)properties.get("datacollectionFileUrl");
        if (property != null && !m_dataCollectionConfigURL.toString().equals(property)) {
            try {
                m_dataCollectionConfigURL = new URL(property);
            } catch (MalformedURLException e) {
                throw new ConfigurationException("datacollectionFileUrl", "Malformed URL", e);
            }
            refresh = true;
        }

        property = (String)properties.get("datacollectionGroupUrls"); 
        if (property != null) {
            m_dataCollectionGroupURLs = parseUrlArray(property);
            refresh = true;
        }
        if (refresh) {
            try {
                refresh();
            } catch (JAXBException e) {
                throw new ConfigurationException("datacollectionFileUrl", "Could not load new configuration", e);
            } catch (IOException e) {
                throw new ConfigurationException("datacollectionFileUrl", "Could not load new configuration", e);
            }
        }
    }
}
