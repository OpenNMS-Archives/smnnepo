package org.opennms.netmgt.sampler.config.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.annotation.Resource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.core.config.api.ConfigurationResource;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.monitoringLocations.MonitoringLocationsConfiguration;
import org.opennms.netmgt.dao.api.DistPollerDao;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.dao.api.LocationMonitorDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.sampler.config.SamplerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={
        "classpath:META-INF/opennms/applicationContext-soa.xml",
        "classpath:META-INF/opennms/applicationContext-datasource.xml",
        "classpath*:/META-INF/opennms/applicationContext-daemon.xml",
        "classpath:META-INF/opennms/applicationContext-testDao.xml",
        "classpath:DefaultSampleConfigurationProviderTest-context.xml"
})
@Transactional
public class DefaultSampleConfigurationProviderTest {
    @Resource(name="collectd-configuration.xml")
    private ConfigurationResource<CollectdConfiguration> m_collectdResource;

    @Resource(name="monitoring-locations.xml")
    private ConfigurationResource<MonitoringLocationsConfiguration> m_monitoringLocationsResource;

    @Autowired
    LocationMonitorDao m_locationMonitorDao; 

    @Autowired
    DistPollerDao m_distPollerDao;

    @Autowired
    NodeDao m_nodeDao;
    
    @Autowired
    IpInterfaceDao m_ipInterfaceDao;
    
    @Autowired
    SnmpPeerFactory m_snmpPeerFactory;

    private DefaultSamplerConfigurationProvider m_provider;

    @Before
    public void setUp() throws Exception {

        m_provider = new DefaultSamplerConfigurationProvider();
        m_provider.setLocationMonitorDao(m_locationMonitorDao);
        m_provider.setCollectdConfigurationResource(m_collectdResource);
        m_provider.setMonitoringLocationsConfigurationResource(m_monitoringLocationsResource);
        m_provider.setIpInterfaceDao(m_ipInterfaceDao);
        m_provider.setSnmpPeerFactory(m_snmpPeerFactory);
    }

    @Test
    public void test() {
        SamplerConfiguration config = m_provider.getConfigForLocation("RDU");
        assertNotNull(config);
        List<Agent> agents = config.getAgents("SNMP");
        assertEquals(1, agents.size());



    }

}
