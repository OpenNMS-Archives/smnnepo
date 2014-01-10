package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="includedGroup")
public class GroupReference {

	 @XmlAttribute(name="dataCollectionGroup")
	 public String m_dataCollectionGroup;

	public String getDataCollectionGroup() {
		return m_dataCollectionGroup;
	}
}
