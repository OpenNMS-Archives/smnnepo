package org.opennms.netmgt.sampler.snmp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.netmgt.config.collectd.Package;
import org.opennms.netmgt.config.collectd.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class converts a single {@link Package} with multiple {@link Service} entries 
 * into multiple {@link Package} entries that each contain a single {@link Service}
 * entry.
 */
public class PackageServiceSplitter implements Processor {
	private static final Logger LOG = LoggerFactory.getLogger(PackageServiceSplitter.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		//LOG.debug("RUNNING {} with args {}", getClass().getSimpleName(), exchange);

		List<Package> retval = new ArrayList<Package>();
		if (exchange != null && exchange.getIn() != null) {
			Package pkg = exchange.getIn().getBody(Package.class);
			for (Service svc : pkg.getServices()) {
				//LOG.debug("SERVICE {}", svc);
				Package newPackage = new Package(pkg);
				newPackage.setServices(Collections.singletonList(svc));
				//LOG.debug("PACKAGE {}", newPackage);
				retval.add(newPackage);
			}
			exchange.getIn().setBody(retval);
		}
	}
}
