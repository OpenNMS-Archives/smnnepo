package org.opennms.netmgt.sampler.config.internal;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Agent.AgentList;
import org.opennms.netmgt.api.sample.PackageAgentList;
import org.opennms.netmgt.config.collectd.Package;

public class PackageAgentAggregator implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange pkgServiceExchange, Exchange svcAgentsExchange) {
		Package pkgService = pkgServiceExchange.getIn().getBody(Package.class);
		List<Agent> svcAgents = svcAgentsExchange.getIn().getBody(AgentList.class);
		
		PackageAgentList pkgAgents = new PackageAgentList(pkgService, svcAgents);
		
		pkgServiceExchange.getIn().setBody(pkgAgents);
		
		return pkgServiceExchange;
	}
}