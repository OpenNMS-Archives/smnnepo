package org.opennms.netmgt.sampler.config.snmp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final MibObject[] EMPTY_MIB_OBJECT_ARRAY = new MibObject[0];

    private static Logger LOG = LoggerFactory.getLogger(Group.class);

    @XmlAttribute(name="name")
    private String m_name;

    @XmlElement(name="mibObj")
    private List<MibObject> m_mibObjects = new ArrayList<MibObject>();

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    public IMibObject[] getMibObjects() {
        return m_mibObjects.toArray(EMPTY_MIB_OBJECT_ARRAY);
    }

    public void setMibObjects(final IMibObject[] mibObjects) {
        m_mibObjects = Arrays.asList(MibObject.asMibObjects(mibObjects));
    }

    public void initialize() {
        LOG.debug("{} initializing", m_name);
        for (final MibObject mibObj : m_mibObjects) {
            mibObj.initialize(this);
        }
        LOG.debug("{} finished initializing", m_name);
    }

    public void fillRequest(final SnmpCollectionRequest request) {
        request.addGroup(this);
    }

    public String toString() {
        return getName();
    }

    public Set<Metric> getMetrics() {
        final Set<Metric> metrics = new HashSet<Metric>();
        for(final MibObject mibObj : m_mibObjects) {
            final Metric metric = mibObj.createMetric();
            if (metric != null) { metrics.add(metric); }
        }
        return metrics;
    }

    public Metric getMetric(final String metricName) {
        for(final MibObject mibObj : m_mibObjects) {
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
