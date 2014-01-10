package org.opennms.netmgt.sampler.config.snmp;

import static junit.framework.Assert.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.xml.sax.SAXException;

public class DataCollectionParseTest {
	
	private class StringSource extends StreamSource {
		public StringSource(String data) {
			super(new StringReader(data), "sample data");
		}
	}

	private static final String dataCollectionConfig =
			"<?xml version=\"1.0\"?>" + 
			"<datacollection-config>" + 
			"    <snmp-collection name=\"default\" snmpStorageFlag=\"select\">" + 
			"        <include-collection dataCollectionGroup=\"MIB2\"/>" + 
			"        <include-collection dataCollectionGroup=\"Net-SNMP\"/>" + 
			"    </snmp-collection>" + 
			"</datacollection-config>" + 
			"";
	
	private static final String dataCollectionGroup = "<?xml version=\"1.0\"?>\n" + 
			"<datacollection-group name=\"MIB2\">\n" + 
			"\n" + 
			"      <resourceType name=\"hrStorageIndex\" label=\"Storage (MIB-2 Host Resources)\">\n" + 
			"        <resourceName>" +
			"          <template>${hrStorageDescr}</template>" +
			"         </resourceName>\n" + 
			"        <resourceLabel><template>${hrStorageDescr}</template></resourceLabel>\n" + 
			"        <resourceKind><template>${hrStorageType}</template></resourceKind>\n" + 
			"        <column oid=\".1.3.6.1.2.1.25.2.3.1.2\" alias=\"hrStorageType\"  type=\"string\" />\n" + 
			"        <column oid=\".1.3.6.1.2.1.25.2.3.1.3\" alias=\"hrStorageDescr\" type=\"string\" />\n" + 
			"      </resourceType>\n" + 
			"\n" + 
			"      <table name=\"mib2-host-resources-storage\" instance=\"hrStorageIndex\">\n" + 
			"        <column oid=\".1.3.6.1.2.1.25.2.3.1.4\" alias=\"hrStorageAllocUnits\" type=\"gauge\" />\n" + 
			"        <column oid=\".1.3.6.1.2.1.25.2.3.1.5\" alias=\"hrStorageSize\"       type=\"gauge\" />\n" + 
			"        <column oid=\".1.3.6.1.2.1.25.2.3.1.6\" alias=\"hrStorageUsed\"       type=\"gauge\" />\n" + 
			"      </table>\n" +
			"\n" + 
			"      <group name=\"mib2-coffee-rfc2325\">\n" + 
			"        <mibObj oid=\".1.3.6.1.2.1.10.132.2\"     instance=\"0\" alias=\"coffeePotCapacity\" type=\"integer\" />\n" + 
			"        <mibObj oid=\".1.3.6.1.2.1.10.132.4.1.2\" instance=\"0\" alias=\"coffeePotLevel\"    type=\"integer\" />\n" + 
			"        <mibObj oid=\".1.3.6.1.2.1.10.132.4.1.6\" instance=\"0\" alias=\"coffeePotTemp\"     type=\"integer\" />\n" + 
			"      </group>\n" + 
			"\n" + 
			"      <systemDef name=\"Enterprise\">\n" + 
			"        <sysoidMask>.1.3.6.1.4.1.</sysoidMask>\n" + 
			"        <collect>\n" + 
			"          <include>mib2-host-resources-storage</include>\n" + 
			"          <include>mib2-coffee-rfc2325</include>\n" + 
			"        </collect>\n" + 
			"      </systemDef>\n" + 
			"\n" + 
			"</datacollection-group>\n" + 
			"";
	
	@Before
	public void setUp() {
		XMLUnit.setIgnoreWhitespace(true);
	}

	@Test
	public void testDataCollectionUnmarshall() throws JAXBException, SAXException, IOException {
		JAXBContext context = JAXBContext.newInstance(DataCollectionConfig.class);
		
		
		DataCollectionConfig dataCollection = context.createUnmarshaller().unmarshal(new StringSource(dataCollectionConfig), DataCollectionConfig.class).getValue();
		
		assertNotNull(dataCollection);
		assertNotNull(dataCollection.getSnmpCollections());
		assertEquals(1, dataCollection.getSnmpCollections().length);
		
		SnmpCollection snmpCollection = dataCollection.getSnmpCollections()[0];
		assertNotNull(snmpCollection);
		assertEquals("default", snmpCollection.getName());
		assertEquals("select", snmpCollection.getSnmpStorageFlag());
		assertNotNull(snmpCollection.getIncludedGroups());
		assertEquals(2, snmpCollection.getIncludedGroups().length);
		
		GroupReference mib2GroupRef = snmpCollection.getIncludedGroups()[0];
		assertNotNull(mib2GroupRef);
		assertEquals("MIB2", mib2GroupRef.getDataCollectionGroup());
		
		
		GroupReference netSnmpGroupRef = snmpCollection.getIncludedGroups()[1];
		assertNotNull(netSnmpGroupRef);
		assertEquals("Net-SNMP", netSnmpGroupRef.getDataCollectionGroup());
		
		StringWriter out = new StringWriter();
		context.createMarshaller().marshal(dataCollection, out);
		String marshalled = out.toString();
		
		System.err.println(marshalled);
		
		assertXMLEqual(dataCollectionConfig, marshalled);
	}
	
	@Test
	public void testDataCollectionGroups() throws JAXBException {
		
		
		JAXBContext context = JAXBContext.newInstance(DataCollectionGroup.class);
		
		DataCollectionGroup group = context.createUnmarshaller().unmarshal(new StringSource(dataCollectionGroup), DataCollectionGroup.class).getValue();
		
		assertEquals("MIB2", group.getName());
		
		assertNotNull(group.getResourceTypes());
		assertEquals(1, group.getResourceTypes().length);
		
		ResourceType resourceType = group.getResourceTypes()[0];
		
		assertEquals("hrStorageIndex", resourceType.getTypeName());
		assertEquals("Storage (MIB-2 Host Resources)", resourceType.getLabel());
		assertNotNull(resourceType.getResourceNameExpression());
		assertEquals("${hrStorageDescr}", resourceType.getResourceNameExpression().getTemplate());
		assertNotNull(resourceType.getResourceLabelExpression());
		assertEquals("${hrStorageDescr}", resourceType.getResourceLabelExpression().getTemplate());
		assertNotNull(resourceType.getResourceKindExpression());
		assertEquals("${hrStorageType}", resourceType.getResourceKindExpression().getTemplate());
		assertNotNull(resourceType.getColumns());
		assertEquals(2, resourceType.getColumns().length);
		
		assertColumnEq(".1.3.6.1.2.1.25.2.3.1.2", "hrStorageType", "string", resourceType.getColumns()[0]);
		assertColumnEq(".1.3.6.1.2.1.25.2.3.1.3", "hrStorageDescr", "string", resourceType.getColumns()[1]);
		
		assertNotNull(group.getTables());
		assertEquals(1, group.getTables().length);
		
		Table table = group.getTables()[0];
		assertNotNull(table);
		
		assertNotNull(table.getColumns());
		assertEquals(3, table.getColumns().length);
		
		assertColumnEq(".1.3.6.1.2.1.25.2.3.1.4", "hrStorageAllocUnits", "gauge", table.getColumns()[0]);
		assertColumnEq(".1.3.6.1.2.1.25.2.3.1.5", "hrStorageSize", "gauge", table.getColumns()[1]);
		assertColumnEq(".1.3.6.1.2.1.25.2.3.1.6", "hrStorageUsed", "gauge", table.getColumns()[2]);
		
		assertNotNull(group.getGroups());
		assertEquals(1, group.getGroups().length);
		
		Group g = group.getGroups()[0];
		assertNotNull(g);
		assertEquals("mib2-coffee-rfc2325", g.getName());
		assertNotNull(g.getMibObjects());
		assertEquals(3, g.getMibObjects().length);
		
		assertMibObjectEq(".1.3.6.1.2.1.10.132.2", "0", "coffeePotCapacity", "integer", g.getMibObjects()[0]);
		assertMibObjectEq(".1.3.6.1.2.1.10.132.4.1.2", "0", "coffeePotLevel", "integer", g.getMibObjects()[1]);
		assertMibObjectEq(".1.3.6.1.2.1.10.132.4.1.6", "0", "coffeePotTemp", "integer", g.getMibObjects()[2]);
		
		assertNotNull(group.getSystemDefs());
		assertEquals(1, group.getSystemDefs().length);
		
		SystemDef systemDef = group.getSystemDefs()[0];
		assertEquals("Enterprise", systemDef.getName());
		assertEquals(".1.3.6.1.4.1.", systemDef.getSysoidMask());
		assertNull(systemDef.getSysoid());
		assertNotNull(systemDef.getIncludes());
		assertEquals(2, systemDef.getIncludes().length);
		
		assertEquals("mib2-host-resources-storage", systemDef.getIncludes()[0]);
		assertEquals("mib2-coffee-rfc2325", systemDef.getIncludes()[1]);
		
	}
	
	private void assertColumnEq(String oid, String alias, String type, Column column) {
		assertNotNull(column);
		
		assertEquals(SnmpObjId.get(oid), column.getOid());
		assertEquals(alias, column.getAlias());
		assertEquals(type, column.getType());

	}
	
	private void assertMibObjectEq(String oid, String instance, String alias, String type, MibObject mibObject) {
		assertNotNull(mibObject);
		
		assertEquals(SnmpObjId.get(oid), mibObject.getOid());
		assertEquals(instance, mibObject.getInstance());
		assertEquals(alias, mibObject.getAlias());
		assertEquals(type, mibObject.getType());

	}
	

}
