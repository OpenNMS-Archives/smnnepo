package org.opennms.netmgt.api.sample.support;

import org.opennms.netmgt.api.sample.PackageAgentList;

/**
 * 
 */
public interface SchedulerService {
	void schedule(PackageAgentList agents);
}
