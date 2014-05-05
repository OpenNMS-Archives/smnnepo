package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Agent;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.config.api.collection.IColumn;
import org.opennms.netmgt.config.api.collection.IExpression;
import org.opennms.netmgt.config.api.collection.IResourceType;
import org.opennms.netmgt.sampler.config.snmp.PropertiesUtils.SymbolTable;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpRowResult;
import org.opennms.netmgt.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@XmlAccessorType(XmlAccessType.NONE)
public class ResourceType implements IResourceType {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceType.class);

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

    public ResourceType() {
    }

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

    public IExpression getResourceNameExpression() {
        return m_resourceNameExpression;
    }

    public void setResourceNameExpression(final IExpression expression) {
        m_resourceNameExpression = Expression.asExpression(expression);
    }

    public Expression getResourceLabelExpression() {
        return m_resourceLabelExpression;
    }

    public void setResourceLabelExpression(final IExpression expression) {
        m_resourceLabelExpression = Expression.asExpression(expression);
    }

    public Expression getResourceKindExpression() {
        return m_resourceKindExpression;
    }

    public void setResourceKindExpression(IExpression expression) {
        m_resourceKindExpression = Expression.asExpression(expression);
    }

    public IColumn[] getColumns() {
        return m_columns;
    }

    public void setColumns(final IColumn[] columns) {
        m_columns = Column.asColumns(columns);
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
                        if (value == null) {
                            LOG.warn("Value of column alias {} was not found in SNMP row: {}", column.getAlias(), row);
                            return null;
                        } else {
                            return column.getValue(value);
                        }
                    }
                }
                return null;
            }
        };
        return PropertiesUtils.substitute(nameTemplate, symbolTable);
    }

    public static ResourceType asResourceType(final IResourceType type) {
        if (type == null) return null;
        if (type instanceof ResourceType) {
            return (ResourceType)type;
        } else {
            final ResourceType newType = new ResourceType();
            newType.setName(type.getTypeName());
            newType.setLabel(type.getLabel());
            newType.setResourceNameExpression(type.getResourceNameExpression());
            newType.setResourceLabelExpression(type.getResourceLabelExpression());
            newType.setResourceKindExpression(type.getResourceKindExpression());
            newType.setColumns(type.getColumns());
            return newType;
        }
    }

    public static ResourceType[] asResourceTypes(final IResourceType[] resourceTypes) {
        if (resourceTypes == null) return null;

        final ResourceType[] newResourceTypes = new ResourceType[resourceTypes.length];
        for (int i=0; i < resourceTypes.length; i++) {
            newResourceTypes[i] = ResourceType.asResourceType(resourceTypes[i]);
        }
        return newResourceTypes;
    }


}
