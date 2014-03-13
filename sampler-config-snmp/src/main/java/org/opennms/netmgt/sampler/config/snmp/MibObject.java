package org.opennms.netmgt.sampler.config.snmp;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.MetricType;
import org.opennms.netmgt.api.sample.NanValue;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.api.sample.SampleValue;
import org.opennms.netmgt.snmp.CollectionTracker;
import org.opennms.netmgt.snmp.SingleInstanceTracker;
import org.opennms.netmgt.snmp.SnmpInstId;
import org.opennms.netmgt.snmp.SnmpObjId;
import org.opennms.netmgt.snmp.SnmpResult;
import org.opennms.netmgt.snmp.SnmpValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  <mibObj oid=".1.3.6.1.2.1.10.132.2" instance="0" alias="coffeePotCapacity" type="integer" />
 *  
 * @author brozow
 *
 */
@XmlRootElement(name="mibObj")
@XmlAccessorType(XmlAccessType.FIELD)
public class MibObject {
    protected static final Logger LOG = LoggerFactory.getLogger(MibObject.class);

    @XmlAttribute(name="oid")
	@XmlJavaTypeAdapter(SnmpObjIdXmlAdapter.class)
	private SnmpObjId m_oid;
	
	@XmlAttribute(name="alias")
	private String m_alias;
	
	@XmlAttribute(name="type")
	private String m_type;
	
	@XmlAttribute(name="instance")
	private String m_instance;

	@XmlTransient
	private Group m_group;

	public SnmpObjId getOid() {
		return m_oid;
	}

	public void setOid(SnmpObjId oid) {
		m_oid = oid;
	}

	public String getAlias() {
		return m_alias;
	}

	public void setAlias(String alias) {
		m_alias = alias;
	}

	public String getType() {
		return m_type;
	}

	public void setType(String type) {
		m_type = type;
	}

	public String getInstance() {
		return m_instance;
	}

	public void setInstance(String instance) {
		m_instance = instance;
	}
	
	public Group getGroup() {
	    return m_group;
	}

	public MetricType getMetricType() {
	    final String type = getType().toLowerCase();
	    if (type.startsWith("counter")) {
	        return MetricType.COUNTER;
	    } else if (type.startsWith("gauge")) {
	        return MetricType.GAUGE;
	    } else if (type.startsWith("integer")) {
	        return MetricType.GAUGE;
	    } else if (type.startsWith("timeticks")) {
	        return MetricType.GAUGE;
	    } else {
	        return null;
	    }
	}

	public Metric createMetric() {
		final MetricType type = getMetricType();
		if (type == null) return null;
		return new Metric(getAlias(), type, m_group.getName());
	}

    public CollectionTracker createCollectionTracker(final Resource resource, final SampleSet sampleSet) {
        return new SingleInstanceTracker(m_oid, new SnmpInstId(m_instance)) {
            @Override
            protected void storeResult(final SnmpResult res) {
                final Metric metric = createMetric();
                if (metric == null) {
                    final String errorMessage = "Unable to create metric for SnmpResult " + res + "!";
                    LOG.error(errorMessage + " (type={}, alias={}, metricType={}, group={})", getType(), getAlias(), getMetricType(), m_group);
                    throw new IllegalArgumentException(errorMessage);
                }
                final MetricType type = metric.getType();
                if (type == null) {
                    final String errorMessage = "Unable to determine type for SnmpResult " + res + "!";
                    LOG.error(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }
                final SnmpValue value = res.getValue();
                final SampleValue<?> sampleValue;
                if (value == null) {
                    sampleValue = new NanValue();
                } else {
                    sampleValue = type.getValue(value.toBigInteger());
                }
                sampleSet.addMeasurement(resource, metric, sampleValue);
            }
        };
    }

    public void initialize(final Group group) {
        m_group = group;
    }
	
}

