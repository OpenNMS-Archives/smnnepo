package org.opennms.netmgt.sampler.snmp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;

/**
 * This class converts a single {@link Package} with multiple {@link Service} entries 
 * into multiple {@link Package} entries that each contain a single {@link Service}
 * entry.
 */
public class PackageServiceSplitter implements AggregationStrategy {
	@Override
	public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
		List<Package> retval = new ArrayList<Package>();
		if (oldExchange != null && oldExchange.getIn() != null) {
			Package pkg = oldExchange.getIn().getBody(Package.class);
			for (Service svc : pkg.getServices()) {
				Package newPackage = new Package(pkg);
				newPackage.setServices(Collections.singletonList(svc));
				retval.add(newPackage);
			}
			oldExchange.getIn().setBody(retval);
		}
		return oldExchange;
	}
}
