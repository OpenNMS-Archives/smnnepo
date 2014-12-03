package org.opennms.minion.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.InvalidSyntaxException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

/**
 * The minion-controller feature requires special configuration.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class MinionControllerFeatureInstallTest extends SmnnepoKarafTest {

    @Override
    public Option[] config() {
        Option[] options = super.config();
        List<Option> optionsList = Arrays.asList(new Option[options.length + 1]);
        optionsList.add(editConfigurationFilePut("etc/org.opennms.minion.controller.cfg", "location", "dummy"));
        return (Option[]) optionsList.toArray();
    }

    @Test
    public void testMinionController() throws IOException, InvalidSyntaxException {
        installFeature("minion-controller");
    }

}
