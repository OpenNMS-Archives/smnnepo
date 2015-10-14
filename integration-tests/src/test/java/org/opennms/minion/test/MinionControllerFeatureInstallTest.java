package org.opennms.minion.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.IOException;
import java.util.List;

import org.apache.camel.CamelContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opennms.minion.test.core.SmnnepoKarafTest;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;

/**
 * The minion-controller feature requires special configuration.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class MinionControllerFeatureInstallTest extends SmnnepoKarafTest {

    @Override
    @Configuration
    public Option[] config() {
        List<Option> optionsList = super.configAsList();
        optionsList.add(editConfigurationFilePut("etc/org.opennms.minion.controller.cfg", "location", "dummy"));
        // Add the Camel bundle to the container so we can use it in this test
        optionsList.add(mavenBundle().groupId("org.apache.camel").artifactId("camel-core").version("2.14.1"));
        return optionsList.toArray(new Option[0]);
    }

    @Test
    public void testMinionController() throws IOException, InvalidSyntaxException {
        // Start an ActiveMQ broker
        installFeature("activemq-broker-noweb");
        // Install the controller feature
        installFeature("minion-controller");

        // Change the Camel shutdown timeout since we're leaving messages in-flight
        CamelContext context = getOsgiService(CamelContext.class);
        context.getShutdownStrategy().setTimeout(3);
    }

}
