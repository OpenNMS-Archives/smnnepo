package org.opennms.netmgt.sampler.snmp.internal;

import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.Timestamp;
import org.opennms.netmgt.sampler.config.snmp.SnmpCollectionRequest;
import org.opennms.netmgt.sampler.snmp.SnmpCollector;
import org.opennms.netmgt.snmp.SnmpUtils;
import org.opennms.netmgt.snmp.SnmpWalker;

public class DefaultSnmpCollector implements SnmpCollector {

	
	/* (non-Javadoc)
	 * @see org.opennms.netmgt.sampler.snmp.SnmpCollector#collect(org.opennms.distributed.configuration.snmp.SnmpCollectionRequest)
	 */
	@Override
	public SampleSet collect(SnmpCollectionRequest request) throws Exception {

		SampleSet sampleSet = new SampleSet(Timestamp.now());
		SnmpWalker walker = SnmpUtils.createWalker(
				request.getAgentConfig(),
				request.getAgent().getId(),
				request.getCollectionTracker(sampleSet)
		);
		
		walker.start();
		
		walker.waitFor();
		
		if (!walker.timedOut() && walker.failed()) {
			throw new RuntimeException(walker.getErrorThrowable());
		}
		
		return sampleSet;
		
	}
	
}