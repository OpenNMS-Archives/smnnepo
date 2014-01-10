package org.opennms.netmgt.api.sample;

public interface CollectionRequest<A extends Agent> {
	
	A getAgent();
	
	String getProtocol();

}
