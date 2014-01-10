package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.sampler.config.snmp.PropertiesUtils.SymbolTable;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpRowResult;
import org.opennms.netmgt.snmp.SnmpValue;

/**
 *  <resourceType name="hrStorageIndex" label="Storage (MIB-2 Host Resources)">
 *	  <resourceName>
 *      <template>${hrStorageDescr}</template>
 *    </resourceName>
 *    <resourceLabel><template>${hrStorageDescr}</template></resourceLabel>
 *    <resourceKind><template>${hrStorageType}</template></resourceKind>
 *    <column oid=".1.3.6.1.2.1.25.2.3.1.2" alias="hrStorageType"  type="string" />
 *    <column oid=".1.3.6.1.2.1.25.2.3.1.3" alias="hrStorageDescr" type="string" />
 *  </resourceType>
 *   
 * @author brozow
 *
 */

@XmlRootElement(name="resourceType")
@XmlAccessorType(XmlAccessType.FIELD)
public class ResourceType {
	
	@XmlAttribute(name="name")
	private String m_name;
	
	@XmlAttribute(name="label")
	private String m_label;
	
	@XmlElement(name="resourceName")
	private Expression m_resourceNameExpression;
	
	@XmlElement(name="resourceLabel")
	private Expression m_resourceLabelExpression;
	
	@XmlElement(name="resourceKind")
	private Expression m_resourceKindExpression;
	
	@XmlElement(name="column")
	private Column[] m_columns = new Column[0];

	public String getTypeName() {
		return m_name;
	}

	public void setName(String name) {
		m_name = name;
	}

	public String getLabel() {
		return m_label;
	}

	public void setLabel(String label) {
		m_label = label;
	}

	public Expression getResourceNameExpression() {
		return m_resourceNameExpression;
	}

	public void setResourceNameExpression(Expression resourceNameExpression) {
		m_resourceNameExpression = resourceNameExpression;
	}

	public Expression getResourceLabelExpression() {
		return m_resourceLabelExpression;
	}

	public void setResourceLabelExpression(Expression resourceLabelExpression) {
		m_resourceLabelExpression = resourceLabelExpression;
	}

	public Expression getResourceKindExpression() {
		return m_resourceKindExpression;
	}

	public void setResourceKindExpression(Expression resourceKindExpression) {
		m_resourceKindExpression = resourceKindExpression;
	}

	public Column[] getColumns() {
		return m_columns;
	}

	public void setColumns(Column[] columns) {
		m_columns = columns;
	}
	
	public String toString() {
		return getTypeName();
	}

	public Resource getResource(Agent agent, SnmpRowResult row) {
		String resourceName = createResourceName(row);
		Resource resource = new Resource(agent, getTypeName(), resourceName);
		
		for(Column column : m_columns) {
			SnmpValue val = row.getValue(column.getOid());
			if (val != null) {
				String attrValue = column.getValue(val);
				resource.setAttribute(column.getAlias(), attrValue);
			}
		}
		return resource;
	}
	
	private String createResourceName(final SnmpRowResult row) {
		String nameTemplate = getResourceNameExpression().getTemplate();
		
		SymbolTable symbolTable = new SymbolTable() {
			
			@Override
			public String getSymbolValue(String symbol) {
				for (Column column : m_columns) {
					if ("index".equals(symbol)) {
						return row.getInstance().toString();
					} else if (column.getAlias().equals(symbol)) {
						SnmpObjId base = column.getOid();
						SnmpValue value = row.getValue(base);
						return column.getValue(value);
					}
				}
				return null;
			}
		};
		return PropertiesUtils.substitute(nameTemplate, symbolTable);
	}
	

}
