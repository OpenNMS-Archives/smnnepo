package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.config.api.collection.IColumn;
import org.opennms.netmgt.config.api.collection.ITable;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpRowResult;
import org.opennms.netmgt.snmp.SnmpValue;
import org.opennms.netmgt.snmp.TableTracker;

/**
 *  <table name="mib2-host-resources-storage" instance="hrStorageIndex">"
 *      <column oid=".1.3.6.1.2.1.25.2.3.1.4" alias="hrStorageAllocUnits" type="gauge" />
 *      <column oid=".1.3.6.1.2.1.25.2.3.1.5" alias="hrStorageSize"       type="gauge" />
 *      <column oid=".1.3.6.1.2.1.25.2.3.1.6" alias="hrStorageUse\"       type="gauge" />
 *  </table>
 *  
 * @author brozow
 *
 */
@XmlRootElement(name="table")
@XmlAccessorType(XmlAccessType.NONE)
public class Table implements ITable {

    @XmlAttribute(name="name")
    private String m_name;

    @XmlAttribute(name="instance")
    private String m_instance;

    @XmlElement(name="column")
    private Column[] m_columns;

    @XmlTransient
    private ResourceType m_resourceType;

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public String getInstance() {
        return m_instance;
    }

    public void setInstance(String instance) {
        m_instance = instance;
    }

    @Override
    public IColumn[] getColumns() {
        return m_columns;
    }

    public void setColumns(final IColumn[] columns) {
        if (columns == null) {
            m_columns = null;
            return;
        }
        final Column[] newColumns = new Column[columns.length];
        for (int i=0; i < columns.length; i++) {
            newColumns[i] = Column.asColumn(columns[i]);
        }
        m_columns = newColumns;
    }

    public void initialize(final ResourceType type) {
        m_resourceType = type;
    }

    public void fillRequest(SnmpCollectionRequest request) {
        assertInitialized();
        request.addResourceType(m_resourceType);
        request.addTable(this);
    }

    private void assertInitialized() {
        if (m_resourceType == null) {
            throw new IllegalStateException("Table " + getName() + " is not initialied!");
        }
    }

    public String toString() {
        return getName();
    }

    public CollectionTracker createCollectionTracker(final SnmpAgent agent, final SampleSet sampleSet) {

        //System.err.println("Creating Table tracker for table " + getName());

        List<SnmpObjId> snmpObjIds = new ArrayList<SnmpObjId>();

        for(IColumn resourceColumn : m_resourceType.getColumns()) {
            snmpObjIds.add(resourceColumn.getOid());
        }

        for(Column tableColumn : m_columns) {
            snmpObjIds.add(tableColumn.getOid());
        }

        SnmpObjId[] ids = snmpObjIds.toArray(new SnmpObjId[snmpObjIds.size()]);
        return new TableTracker(ids) {

            @Override
            public void rowCompleted(SnmpRowResult row) {
                //System.err.println("row completed: " + row);
                Resource resource = m_resourceType.getResource(agent, row);

                for(Column column : m_columns) {
                    Metric metric = column.createMetric(getName());
                    if (metric != null) { 
                        SnmpValue snmpValue = row.getValue(column.getOid());

                        if (snmpValue != null) {
                            SampleValue<?> sampleValue = metric.getType().getValue(snmpValue.toBigInteger());
                            sampleSet.addMeasurement(resource, metric, sampleValue);
                        }
                    }
                }

            }

        };
    }

    public Set<Metric> getMetrics() {
        Set<Metric> metrics = new HashSet<Metric>();
        for(Column column : m_columns) {
            Metric metric = column.createMetric(getName());
            // string columns do not represetn metrics and null is returned
            if (metric != null) { metrics.add(metric); }
        }
        return metrics;
    }

    public Metric getMetric(String metricName) {
        for(Column column : m_columns) {
            if (column.getAlias().equals(metricName)) {
                return column.createMetric(getName());
            }
        }
        return null;
    }

    public static Table asTable(final ITable table) {
        if (table == null) return null;
        if (table instanceof Table) {
            return (Table)table;
        } else {
            final Table newTable = new Table();
            newTable.setName(table.getName());
            newTable.setInstance(table.getInstance());
            newTable.setColumns(table.getColumns());
            return newTable;
        }
    }

}
