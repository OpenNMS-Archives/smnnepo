package org.opennms.netmgt.api.sample;

public interface CollectionConfiguration<A extends Agent, R extends CollectionRequest<A>> {

	public R createRequestForAgent(A agent);
	
}
