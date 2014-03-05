package org.opennms.netmgt.sampler.snmp;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.sampler.snmp.ServiceAgent.ServiceAgentList;

public class PackageAgentAggregator implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange pkgServiceExchange, Exchange svcAgentsExchange) {
		Package pkgService = pkgServiceExchange.getIn().getBody(Package.class);
		List<ServiceAgent> svcAgents = svcAgentsExchange.getIn().getBody(ServiceAgentList.class);
		
		PackageAgentList pkgAgents = new PackageAgentList(pkgService, svcAgents);
		
		pkgServiceExchange.getIn().setBody(pkgAgents);
		
		return pkgServiceExchange;
	}
}