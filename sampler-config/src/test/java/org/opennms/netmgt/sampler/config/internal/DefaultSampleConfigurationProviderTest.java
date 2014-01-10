package org.opennms.netmgt.sampler.config.internal;

import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.dao.api.DistPollerDao;
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
	
	@Autowired
	LocationMonitorDao m_locationMonitorDao; 
	
	@Autowired
	DistPollerDao m_distPollerDao;
	
	@Autowired
	NodeDao m_nodeDao;

	private DefaultSamplerConfigurationProvider m_provider;
	
	@Before
	public void setUp() throws Exception {
		
		m_provider = new DefaultSamplerConfigurationProvider();
		m_provider.setLocationMonitorDao(m_locationMonitorDao);
	}

	@Test
	public void test() {
		SamplerConfiguration config = m_provider.getConfigForLocation("RDU");
		assertNotNull(config);
		List<Agent> agents = config.getAgents("SNMP");
		//assertEquals(1, agents.size());
		
		
		
	}

}
