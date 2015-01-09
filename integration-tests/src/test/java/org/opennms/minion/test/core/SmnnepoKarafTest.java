package org.opennms.minion.test.core;

import org.junit.Before;

import static org.ops4j.pax.exam.CoreOptions.maven;

public abstract class SmnnepoKarafTest extends KarafTestCase {
    @Before
    public void registerOpenNMSFeatuers() {
        // TODO remove constant version and use versionAsInProject instead (see PJSM-244)
        addFeaturesUrl(maven().groupId("org.opennms.netmgt.sample").artifactId("karaf").version("15.0.0-PJSM-SNAPSHOT").type("xml").getURL());
    }
}
