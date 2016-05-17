package org.opennms.minion.test.core;

import org.junit.Before;

import static org.ops4j.pax.exam.CoreOptions.maven;

import org.opennms.core.test.karaf.KarafTestCase;

public abstract class SmnnepoKarafTest extends KarafTestCase {
    @Before
    public void registerMinionFeatures() {
        // TODO remove constant version and use versionAsInProject instead (see PJSM-244)
        addFeaturesUrl(maven().groupId("org.opennms.netmgt.sample").artifactId("karaf").version("19.0.0-SNAPSHOT").type("xml").getURL());
    }
}
