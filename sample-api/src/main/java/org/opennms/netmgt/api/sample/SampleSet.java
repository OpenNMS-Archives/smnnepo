package org.opennms.netmgt.api.sample;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is not thread-safe.
 * 
 * @author eevans
 *
 */
@XmlRootElement(name="sample-set")
@XmlAccessorType(XmlAccessType.NONE)
public class SampleSet implements Serializable {
    private static final long serialVersionUID = 2L;

    @XmlElement(name="timestamp")
    private final Timestamp m_timestamp;

    @XmlElementWrapper(name="samples")
    @XmlElement(name="sample")
    private final List<Sample> m_measurements = new LinkedList<Sample>();

    private transient Map<Resource, Map<String, LinkedList<Sample>>> m_byResourceGroup;
    private transient Map<Resource, LinkedList<Sample>> m_byResource;
    private transient Map<String, LinkedList<Sample>> m_byGroup;

    private boolean m_dirtyByResourceGroup = false;
    private boolean m_dirtyByResource = false;
    private boolean m_dirtyByGroup = false;

    public SampleSet() {
        m_timestamp = Timestamp.now();
    }

    public SampleSet(Timestamp timestamp) {
        m_timestamp = timestamp;
    }

    public void addMeasurement(Resource r, Metric m, SampleValue<?> value) {
        Sample sample = new Sample(r, m, m_timestamp, value);

        setDirty();

        m_measurements.add(sample);
    }

    private void setDirty() {
        m_dirtyByResourceGroup = true;
        m_dirtyByResource = true;
        m_dirtyByGroup = true;
    }

    /*
     * The transient HashMaps will be initialized to null for deserialized
     * objects, when this is the case create new (empty )maps and set the
     * respective dirty so that it will can rebuilt.
     */

    private void validateByResourceGroup() {
        if (m_byResourceGroup == null) {
            m_byResourceGroup = new HashMap<Resource, Map<String, LinkedList<Sample>>>();
            m_dirtyByResourceGroup = true;
        }
    }

    private void validateByResource() {
        if (m_byResource == null) {
            m_byResource = new HashMap<Resource, LinkedList<Sample>>();
            m_dirtyByResource = true;
        }
    }

    private void validateByGroup() {
        if (m_byGroup == null) {
            m_byGroup = new HashMap<String, LinkedList<Sample>>();
            m_dirtyByGroup = true;
        }
    }

    // Rebuild the byResourceGroup map as needed
    private void indexByResourceGroup() {
        validateByResourceGroup();

        if (m_dirtyByResourceGroup) {
            for (Sample sample : m_measurements) {
                Resource r = sample.getResource();
                String group = sample.getMetric().getGroup();

                if (!m_byResourceGroup.containsKey(r)) m_byResourceGroup.put(r, new HashMap<String, LinkedList<Sample>>());
                if (!m_byResourceGroup.get(r).containsKey(group)) m_byResourceGroup.get(r).put(group, new LinkedList<Sample>());
                m_byResourceGroup.get(r).get(group).add(sample);
            }
            m_dirtyByResourceGroup = false;
        }
    }

    // Rebuild the byResource map as needed
    private void indexByResource() {
        validateByResource();

        if (m_dirtyByResource) {
            for (Sample sample : m_measurements) {
                Resource r = sample.getResource();

                if (!m_byResource.containsKey(r)) m_byResource.put(r, new LinkedList<Sample>());
                m_byResource.get(r).add(sample);
            }
            m_dirtyByResource = false;
        }
    }

    // Rebuild the byGroup map as needed
    private void indexByGroup() {
        validateByGroup();

        if (m_dirtyByGroup) {
            for (Sample sample : m_measurements) {
                String group = sample.getMetric().getGroup();

                if (!m_byGroup.containsKey(group)) m_byGroup.put(group, new LinkedList<Sample>());
                m_byGroup.get(group).add(sample);
            }
            m_dirtyByGroup = false;
        }
    }

    public Set<Resource> getResources() {
        indexByResourceGroup();
        return m_byResourceGroup.keySet();
    }

    public Timestamp getTimestamp() {
        return m_timestamp;
    }

    public Set<String> getGroups() {
        indexByGroup();
        return m_byGroup.keySet();
    }

    public Set<String> getGroups(Resource resource) {
        indexByResourceGroup();
        return m_byResourceGroup.containsKey(resource) ? m_byResourceGroup.get(resource).keySet() : null;
    }

    public Collection<Sample> getSamples(String groupName) {
        indexByGroup();
        return m_byGroup.get(groupName);
    }

    public Collection<Sample> getSamples(Resource resource) {
        indexByResource();
        return m_byResource.get(resource);
    }

    public Collection<Sample> getSamples(Resource resource, String groupName) {
        indexByResourceGroup();
        return m_byResourceGroup.containsKey(resource) ? m_byResourceGroup.get(resource).get(groupName) : null;
    }

    public Collection<Sample> getMeasurements() {
        return m_measurements;
    }

    public Collection<Sample> getSamples() {
        return m_measurements;
    }

    public String toString() {
        return "SampleSet"+getSamples();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_measurements == null) ? 0 : m_measurements.hashCode());
        result = prime * result + ((m_timestamp == null) ? 0 : m_timestamp.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof SampleSet)) {
            return false;
        }
        final SampleSet other = (SampleSet) obj;
        if (m_measurements == null) {
            if (other.m_measurements != null) {
                return false;
            }
        } else if (!m_measurements.equals(other.m_measurements)) {
            return false;
        }
        if (m_timestamp == null) {
            if (other.m_timestamp != null) {
                return false;
            }
        } else if (!m_timestamp.equals(other.m_timestamp)) {
            return false;
        }
        return true;
    }
}
