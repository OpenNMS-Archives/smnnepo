package org.opennms.netmgt.sampler.config.snmp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.snmp.AbstractSnmpValue;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.SnmpWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SnmpMetricRepositoryTest {
    private final class EndOfMibValue extends AbstractSnmpValue {
		@Override
		public boolean isEndOfMib() {
			return true;
		}

		@Override
		public boolean isError() {
			return false;
		}

		@Override
		public boolean isNull() {
			return true;
		}

		@Override
		public boolean isDisplayable() {
			return true;
		}

		@Override
		public boolean isNumeric() {
			return false;
		}

		@Override
		public int toInt() {
			return 0;
		}

		@Override
		public String toDisplayString() {
			return "END_OF_MIB";
		}

		@Override
		public InetAddress toInetAddress() {
			return null;
		}

		@Override
		public long toLong() {
			return 0;
		}

		@Override
		public BigInteger toBigInteger() {
			return null;
		}

		@Override
		public String toHexString() {
			return null;
		}

		@Override
		public int getType() {
			return SnmpValue.SNMP_END_OF_MIB;
		}

		@Override
		public byte[] getBytes() {
			return new byte[0];
		}

		@Override
		public SnmpObjId toSnmpObjId() {
			return null;
		}

		public String toString() {
			return toDisplayString();
		}
	}

	private static final Logger LOG = LoggerFactory.getLogger(SnmpMetricRepositoryTest.class);

    private SnmpMetricRepository m_repository;

	private ExecutorService m_executor;

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
        
        m_executor = Executors.newSingleThreadExecutor(new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				return new Thread(null, r, "Test-Executor-Thread");
			}
        	
        });
    }
    
    @After
    public void tearDown() {
    	m_executor.shutdown();
    }

    @Test
    public void testBogusAgent() throws Exception {

        // bogus agent... no collection should match
        Agent agent = new Agent(new InetSocketAddress("10.1.1.1", 161), "Smith", "1");
        agent.setParameter(SnmpAgent.PARAM_SYSOBJECTID, ".666");
        SnmpAgent snmpAgent = new SnmpAgent(agent);

        SnmpCollectionRequest request = m_repository.createRequestForAgent(snmpAgent);
        assertNotNull(request);

        assertSame(snmpAgent, request.getAgent());

        // gets the mib2 data since that's configured for everything
        assertEquals(2, request.getResourceTypes().size());
        assertEquals(2, request.getTables().size());
        assertEquals(2, request.getGroups().size());

    }

    @Test
    public void testBrokenNetSNMPAgent() throws Exception {

        Agent agent = new Agent(new InetSocketAddress("10.1.1.1", 161), "Smith", "1");
        agent.setParameter(SnmpAgent.PARAM_SYSOBJECTID, ".0.1");
        SnmpAgent snmpAgent = new SnmpAgent(agent);

        SnmpCollectionRequest request = m_repository.createRequestForAgent(snmpAgent);
        assertNotNull(request);

        assertSame(snmpAgent, request.getAgent());

        System.err.println(request);

        assertEquals(9, request.getResourceTypes().size());
        assertEquals(14, request.getTables().size());
        assertEquals(11, request.getGroups().size());
    }

    @Test
    public void testCollectionTracker() throws Exception {
        InetSocketAddress agentAddress = new InetSocketAddress("10.1.1.1", 161);

        Agent agent = new Agent(agentAddress, "Smith", "1");
        agent.setParameter(SnmpAgent.PARAM_SYSOBJECTID, ".666");
        SnmpAgent snmpAgent = new SnmpAgent(agent);
        final SnmpCollectionRequest request = m_repository.createRequestForAgent(snmpAgent);

        LOG.debug("groups: {}", request.getGroups());
        LOG.debug("tables: {}", request.getTables());
        LOG.debug("resource types: {}", request.getResourceTypes());

        final CollectionTracker tracker = request.getCollectionTracker(new SampleSet(Timestamp.now()));

        final SnmpWalker walker = new SnmpWalker(agentAddress.getAddress(), "TestWalker", 200, 10, tracker) {

            private int m_oidCount = 0;
            private int m_nonRepeaters;
            private int m_maxRepetitions;
            private SnmpObjId m_maxOid = null;

            @Override
            protected WalkerPduBuilder createPduBuilder(final int maxVarsPerPdu) {

                return new WalkerPduBuilder(maxVarsPerPdu) {
                    @Override
                    public void reset() {
                        LOG.debug("reset()");
                    }

                    @Override
                    public void addOid(final SnmpObjId snmpObjId) {
                        if (m_maxOid == null) {
                        	m_maxOid = snmpObjId;
                        } else {
                        	if (m_maxOid.compareTo(snmpObjId) < 0) {
                        		m_maxOid = snmpObjId;
                        	}
                        }
                        LOG.debug("oid({}): {}, max: {}", ++m_oidCount, snmpObjId, m_maxOid);

                        
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
            	Runnable r = new Runnable() {

					@Override
					public void run() {
						try {
							SnmpObjId lastOid = SnmpObjId.get(".1.4");
							SnmpValue endOfMib = new EndOfMibValue();
							for(int i = 0; i < m_oidCount; i++) {
								processResponse(lastOid, endOfMib);
							}
							buildAndSendNextPdu();
						} catch (IOException e) {
							handleFatalError(e);
						}
					}
            		
            	};
            	m_executor.execute(r);
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
