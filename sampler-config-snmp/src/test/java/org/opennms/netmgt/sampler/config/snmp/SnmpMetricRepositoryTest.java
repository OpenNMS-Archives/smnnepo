package org.opennms.netmgt.sampler.config.snmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpMetricRepositoryTest {
    private static final Logger LOG = LoggerFactory.getLogger(SnmpMetricRepositoryTest.class);

    private SnmpMetricRepository m_repository;

    private static URL url(String path) throws MalformedURLException {
        return new URL("file:src/main/resources/" + path);
    }

    @Before
    public void setUp() throws Exception {
        m_repository = new SnmpMetricRepository(
                                                url("datacollection-config.xml"), 
                                                url("datacollection/mib2.xml"), 
                                                url("datacollection/netsnmp.xml"),
                                                url("datacollection/dell.xml")
                );
    }

    @Test
    public void testBogusAgent() throws Exception {

        // bogus agent... no collection should match
        SnmpAgent agent = new SnmpAgent(new InetSocketAddress("10.1.1.1", 161), ".666", "Smith");

        SnmpCollectionRequest request = m_repository.createRequestForAgent(agent);
        assertNotNull(request);

        assertSame(agent, request.getAgent());

        // gets the mib2 data since that's configured for everything
        assertEquals(2, request.getResourceTypes().size());
        assertEquals(2, request.getTables().size());
        assertEquals(2, request.getGroups().size());

    }

    @Test
    public void testBrokenNetSNMPAgent() throws Exception {

        SnmpAgent agent = new SnmpAgent(new InetSocketAddress("10.1.1.1", 161), ".0.1", "Smith");

        SnmpCollectionRequest request = m_repository.createRequestForAgent(agent);
        assertNotNull(request);

        assertSame(agent, request.getAgent());

        System.err.println(request);

        assertEquals(9, request.getResourceTypes().size());
        assertEquals(14, request.getTables().size());
        assertEquals(11, request.getGroups().size());
    }

    @Test
    public void testCollectionTracker() throws Exception {
        InetSocketAddress agentAddress = new InetSocketAddress("10.1.1.1", 161);

        final SnmpAgent agent = new SnmpAgent(agentAddress, ".666", "Smith");
        final SnmpCollectionRequest request = m_repository.createRequestForAgent(agent);

        LOG.debug("groups: {}", request.getGroups());
        LOG.debug("tables: {}", request.getTables());
        LOG.debug("resource types: {}", request.getResourceTypes());

        final CollectionTracker tracker = request.getCollectionTracker(new SampleSet(Timestamp.now()));

        final SnmpWalker walker = new SnmpWalker(agentAddress.getAddress(), "TestWalker", 200, 10, tracker) {

            @Override
            protected WalkerPduBuilder createPduBuilder(final int maxVarsPerPdu) {
                return new WalkerPduBuilder(maxVarsPerPdu) {
                    private int m_oidCount = 0;
                    private int m_nonRepeaters;
                    private int m_maxRepetitions;

                    @Override
                    public void reset() {
                        LOG.debug("reset()");
                    }

                    @Override
                    public void addOid(final SnmpObjId snmpObjId) {
                        LOG.debug("oid({}): {}", ++m_oidCount, snmpObjId);
                    }

                    @Override
                    public void setNonRepeaters(final int numNonRepeaters) {
                        LOG.debug("setNonRepeaters: {}", numNonRepeaters);
                        m_nonRepeaters = numNonRepeaters;
                    }

                    @Override
                    public void setMaxRepetitions(final int maxRepetitions) {
                        LOG.debug("setMaxRepetitions: {}", maxRepetitions);
                        m_maxRepetitions = maxRepetitions;
                    }};
            }

            @Override
            protected void sendNextPdu(final WalkerPduBuilder pduBuilder) throws IOException {
                throw new UnsupportedOperationException("Not yet implemented!");
            }

            @Override
            public void close() throws IOException {
                LOG.debug("close()");
            }};
            
        walker.start();
        walker.waitFor();
        
        assertFalse(walker.failed());
    }
}
