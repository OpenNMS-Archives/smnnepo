package org.opennms.minion.test;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

public class KarafTestCase {

    private static Logger LOG = LoggerFactory.getLogger(KarafTestCase.class);

    public static String getKarafVersion() {
        final String karafVersion = System.getProperty("karafVersion");
        Objects.requireNonNull(karafVersion, "Please define a system property 'karafVersion'.");
        return karafVersion;
    }

    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected FeaturesService featuresService;

    @Configuration
    public Option[] config() {
        Option[] options = new Option[]{
                karafDistributionConfiguration().frameworkUrl(
                        maven()
                                .groupId("org.apache.karaf")
                                .artifactId("apache-karaf")
                                .type("tar.gz")
                                .version(getKarafVersion()))
                        .karafVersion(getKarafVersion())
                        .name("Apache Karaf")
                        .unpackDirectory(new File("target/paxexam/")),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),

                // we overwrite mvn repository settings, because we want a "naked" repository
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories", ""),
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/${karaf.default.repository}@snapshots@id=karaf.${karaf.default.repository}"),
                editConfigurationFileExtend("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.defaultRepositories", "file:${karaf.home}/../test-repo@snapshots@id=default-repo"),
                editConfigurationFileExtend("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", "file:${karaf.home}/../opennms-repo@snapshots@id=opennms-repo"),
        };

        if (Boolean.valueOf(System.getProperty("debug"))) {
            options = Arrays.copyOf(options, options.length + 1);
            options[options.length -1] = debugConfiguration("8889", true);
        }
        return options;
    }

    protected void addFeaturesUrl(String url) {
        try {
            featuresService.addRepository(URI.create(url));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    protected void installFeature(String featureName) {
        try {
            featuresService.installFeature(featureName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes a shell command and returns output as a String.
     * Commands have a default timeout of 10 seconds.
     *
     * @param command
     * @return
     */
    protected String executeCommand(final String command) {
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final PrintStream printStream = new PrintStream(byteArrayOutputStream);) {

            final CommandProcessor commandProcessor = getOsgiService(CommandProcessor.class);
            final CommandSession commandSession = commandProcessor.createSession(System.in, printStream, System.err);
            LOG.info("{}", command);
            Object response = commandSession.execute(command);
            LOG.info("Response: {}", response);
            printStream.flush();
            return byteArrayOutputStream.toString();
        } catch (Exception e) {
            LOG.error("Error while executing command", e);
            throw new RuntimeException(e);
        }
    }

    protected <T> T getOsgiService(Class<T> type) {
        ServiceReference<T> serviceReference = bundleContext.getServiceReference(type);
        if (serviceReference != null) {
            return type.cast(bundleContext.getService(serviceReference));
        }
        return null;
    }
}
