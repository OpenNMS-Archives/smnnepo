package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.opennms.netmgt.snmp.SnmpObjId;

public class SnmpObjIdXmlAdapter extends XmlAdapter<String, SnmpObjId> {

	@Override
	public String marshal(SnmpObjId snmpObjId) throws Exception {
		return snmpObjId.toString();
	}

	@Override
	public SnmpObjId unmarshal(String oid) throws Exception {
		return SnmpObjId.get(oid);
	}

}
