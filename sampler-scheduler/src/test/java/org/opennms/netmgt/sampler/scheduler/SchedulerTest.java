package org.opennms.netmgt.sampler.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.opennms.core.network.IPAddress;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.api.sample.ServiceAgent;
import org.opennms.netmgt.config.collectd.Filter;
import org.opennms.netmgt.config.collectd.IncludeRange;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Parameter;
import org.opennms.netmgt.config.collectd.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

public class SchedulerTest extends CamelBlueprintTestSupport {
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerTest.class);
    private CountDownLatch m_countdownLatch;
    private Consumer m_consumer;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseDebugger() {
        return true;
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

    // The location of our Blueprint XML file to be used for testing
    @Override
    protected String getBlueprintDescriptor() {
        return "file:src/main/resources/OSGI-INF/blueprint/blueprint.xml";
    }

    @Test
    public void testScheduleAgents() throws InterruptedException {
        final Scheduler scheduler = new Scheduler(1);
        final CountDownLatch latch = new CountDownLatch(5);
        scheduler.setDispatcher(new Dispatcher() {
            @Override public void dispatch(final CollectionRequest request) {
                LOG.debug("Received request: {}", request);
                latch.countDown();
            }
        });

        final List<ServiceAgent> agents = getAgents();
        final PackageAgentList agentSchedule = new PackageAgentList(getPackage(), agents);
        scheduler.onAgentSchedule(agentSchedule);
        assertTrue(latch.await(8, TimeUnit.SECONDS));
    }

    @Override
    protected void doPostSetup() throws Exception {
        m_countdownLatch = new CountDownLatch(5);
        final Endpoint endpoint = context.getEndpoint("seda:dispatch");
        m_consumer = endpoint.createConsumer(new Processor() {
            @Override public void process(final Exchange exchange) throws Exception {
                LOG.debug("process exchange: {}", exchange);
                m_countdownLatch.countDown();
            }
        });
    }

    @Test
    public void testScheduleAgentsWithCamelDirectEndpoint() throws Exception {
        m_consumer.start();

        final List<ServiceAgent> agents = getAgents();
        final PackageAgentList agentSchedule = new PackageAgentList(getPackage(), agents);
        sendBody("seda:scheduleAgents", agentSchedule);
        assertTrue(m_countdownLatch.await(8, TimeUnit.SECONDS));
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
}
