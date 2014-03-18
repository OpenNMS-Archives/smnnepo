package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.netmgt.api.sample.Metric;
import org.opennms.netmgt.api.sample.Resource;
import org.opennms.netmgt.api.sample.SampleSet;
import org.opennms.netmgt.config.api.collection.IGroup;
import org.opennms.netmgt.config.api.collection.IMibObject;
import org.opennms.netmgt.snmp.AggregateTracker;
import org.opennms.netmgt.snmp.Collectable;
import org.opennms.netmgt.snmp.CollectionTracker;

/**
 * 	<group name="mib2-coffee-rfc2325">
 *      <mibObj oid=".1.3.6.1.2.1.10.132.2" instance="0" alias="coffeePotCapacity" type="integer" />
 *      <mibObj oid=".1.3.6.1.2.1.10.132.4.1.2" instance="0" alias="coffeePotLevel" type="integer" />
 *      <mibObj oid=".1.3.6.1.2.1.10.132.4.1.6" instance="0" alias="coffeePotTemp" type="integer" />
 *  </group>
 *  
 * @author brozow
 *
 */
@XmlRootElement(name="group")
@XmlAccessorType(XmlAccessType.NONE)
public class Group implements IGroup {

    @XmlAttribute(name="name")
    private String m_name;

    @XmlElement(name="mibObj")
    private MibObject[] m_mibObjects = new MibObject[0];

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public IMibObject[] getMibObjects() {
        return (IMibObject[]) m_mibObjects;
    }

    public void setMibObjects(final IMibObject[] mibObjects) {
        m_mibObjects = MibObject.asMibObjects(mibObjects);
    }

    public void initialize() {
        for (final MibObject mibObj : m_mibObjects) {
            mibObj.initialize(this);
        }
    }

    public void fillRequest(SnmpCollectionRequest request) {
        request.addGroup(this);
    }

    public String toString() {
        return getName();
    }

    public Set<Metric> getMetrics() {
        Set<Metric> metrics = new HashSet<Metric>();
        for(MibObject mibObj : m_mibObjects) {
            Metric metric = mibObj.createMetric();
            if (metric != null) { metrics.add(metric); }
        }
        return metrics;
    }

    public Metric getMetric(String metricName) {
        for(MibObject mibObj : m_mibObjects) {
            if (mibObj.getAlias().equals(metricName)) {
                return mibObj.createMetric();
            }
        }
        return null;
    }

    public CollectionTracker createCollectionTracker(final SnmpAgent agent, final SampleSet sampleSet) {
        final Resource groupResource = new Resource(agent, "node", m_name);

        final Collection<Collectable> trackers = new ArrayList<Collectable>();
        for (final MibObject mibObj : m_mibObjects) {
            trackers.add(mibObj.createCollectionTracker(groupResource, sampleSet));
        }

        return new AggregateTracker(trackers);
    }

    public static Group asGroup(final IGroup group) {
        if (group == null) return null;

        if (group instanceof Group) {
            return (Group)group;
        }

        final Group newGroup = new Group();
        newGroup.setName(group.getName());
        newGroup.setMibObjects(group.getMibObjects());
        return newGroup;
    }

    public static Group[] asGroups(final IGroup[] groups) {
        if (groups == null) return null;
        
        final Group[] newGroups = new Group[groups.length];
        for (int i=0; i < groups.length; i++) {
            newGroups[i] = Group.asGroup(groups[i]);
        }
        return newGroups;
    }
}
