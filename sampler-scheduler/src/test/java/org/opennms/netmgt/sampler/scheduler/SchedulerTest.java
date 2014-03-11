package org.opennms.netmgt.sampler.scheduler;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.apache.camel.util.KeyValueHolder;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.network.IPAddress;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.ServiceAgent;
import org.opennms.netmgt.api.sample.support.Dispatcher;
import org.opennms.netmgt.api.sample.support.SchedulerService;
import org.opennms.netmgt.api.scheduler.CollectionRequest;
import org.opennms.netmgt.config.collectd.Filter;
import org.opennms.netmgt.config.collectd.IncludeRange;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Parameter;
import org.opennms.netmgt.config.collectd.Service;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public class SchedulerTest extends CamelBlueprintTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerTest.class);
    private LatchDispatcher m_latchDispatcher;

    
    @Override
    protected String getBlueprintDescriptor() {
        return "OSGI-INF/blueprint/blueprint.xml,OSGI-INF/blueprint/blueprint-test.xml";
    };

    @Override
    @SuppressWarnings("rawtypes")
    protected void addServicesOnStartup(final Map<String, KeyValueHolder<Object, Dictionary>> services) {
        final Properties props = new Properties();
        props.put("org.opennms.netmgt.sampler.scheduler.serviceName", "SNMP");
        m_latchDispatcher = new LatchDispatcher(5);
        services.put(Dispatcher.class.getName(), new KeyValueHolder<Object,Dictionary>(m_latchDispatcher, props));
    }
    
    @Before
    @Override
    public void setUp() throws Exception {
        // bad bad bad!
        for (final String pack : new String[]{"org.apache.camel", "org.apache.aries"}) {
            final ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(pack);
            logger.setLevel(Level.INFO);
        }
        super.setUp();
    }

    @Test
    public void testScheduleAgents() throws InterruptedException {
        final Scheduler scheduler = new Scheduler(1);
        scheduler.setDispatcher("SNMP", m_latchDispatcher);

        final List<ServiceAgent> agents = getAgents();
        final PackageAgentList agentSchedule = new PackageAgentList(getPackage(), agents);
        scheduler.schedule(agentSchedule);
        assertTrue(m_latchDispatcher.await(8, TimeUnit.SECONDS));
    }

    @Test
    public void testScheduleAgentsFromBundle() throws Exception {
        context.start();
        final ServiceReference<SchedulerService> ref = getBundleContext().getServiceReference(SchedulerService.class);
        assertNotNull(ref);
        final SchedulerService schedulerService = getBundleContext().getService(ref);
        schedulerService.schedule(new PackageAgentList(getPackage(), getAgents()));
        assertTrue(m_latchDispatcher.await(8, TimeUnit.SECONDS));
    }

    protected List<ServiceAgent> getAgents() {
        final List<ServiceAgent> agents = new ArrayList<ServiceAgent>();
        agents.add(new ServiceAgent(new IPAddress("192.168.0.1"), 161, "SNMP"));
        agents.add(new ServiceAgent(new IPAddress("192.168.0.2"), 161, "SNMP"));
        agents.add(new ServiceAgent(new IPAddress("192.168.0.3"), 161, "SNMP"));
        agents.add(new ServiceAgent(new IPAddress("192.168.0.4"), 161, "SNMP"));
        agents.add(new ServiceAgent(new IPAddress("192.168.0.5"), 161, "SNMP"));
        return agents;
    }

    protected Package getPackage() {
        final Package pack = new Package();
        pack.setName("foo");
        pack.setFilter(new Filter("IPADDR != '0.0.0.0'"));
        pack.addIncludeRange(new IncludeRange("1.1.1.1", "254.254.254.254"));

        final Service snmp = new Service();
        snmp.setName("SNMP");
        snmp.setInterval(5 * 1000l);
        snmp.setUserDefined("false");
        snmp.setStatus("on");
        snmp.addParameter(new Parameter("collection", "default"));
        snmp.addParameter(new Parameter("thresholding-enabled", "true"));
        pack.addService(snmp);

        return pack;
    }

    private static class LatchDispatcher implements Dispatcher {
        private CountDownLatch m_latch;
        public LatchDispatcher(final int start) {
            m_latch = new CountDownLatch(start);
        }

        @Override
        public void dispatch(final CollectionRequest request) {
            LOG.debug("dispatch: {}", request);
            m_latch.countDown();
        }

        public boolean await(final long timeout, final TimeUnit unit) throws InterruptedException {
            return m_latch.await(timeout, unit);
        }
    }
}
