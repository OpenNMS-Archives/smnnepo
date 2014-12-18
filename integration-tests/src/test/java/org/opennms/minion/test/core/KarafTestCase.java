package org.opennms.minion.test.core;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.osgi.Util;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

public abstract class KarafTestCase {

    private static Logger LOG = LoggerFactory.getLogger(KarafTestCase.class);

    public static class ResultInfo {

        private String bundleName;
        private List<String> resources = new ArrayList<>();

        public void setBundleName(String bundleName) {
            this.bundleName = bundleName;
        }

        public String getBundleName() {
            return bundleName;
        }

        public void addResource(String resource) {
            resources.add(resource);
        }

        public List<String> getResources() {
            return resources;
        }
    }

    public static String getKarafVersion() {
        final String karafVersion = System.getProperty("karafVersion");
        Objects.requireNonNull(karafVersion, "Please define a system property 'karafVersion'.");
        return karafVersion;
    }

    private static boolean isDebug() {
        String debugString = System.getProperty("debug");
        if ("".equals(debugString)) {
            return true;
        }
        return Boolean.valueOf(System.getProperty("debug")).booleanValue();
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

        if (isDebug()) {
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

    protected void installFeature(final String featureName, final String version) {
        try {
            featuresService.installFeature(featureName, version);
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

    protected List<ResultInfo> findResource(final String resourceName) {
        final Bundle[] bundles = bundleContext.getBundles();
        final String filter = "*" + resourceName + "*";
        List<ResultInfo> resultInfoList = new ArrayList<>();
        LOG.info("Find resources '{}' in all bundles.", filter);
        for (Bundle bundle : bundles) {
            final BundleWiring wiring = bundle.adapt(BundleWiring.class);
            if (wiring != null) {
                final Collection<String> resources = wiring.listResources("/", filter, BundleWiring.LISTRESOURCES_RECURSE);
                final String bundleName = Util.getBundleName(bundle);
                if (resources.size() > 0) {
                    LOG.info("{} results in bundle {} fetched.", resources.size(), bundleName);
                    final ResultInfo resultInfo = new ResultInfo();
                    resultInfo.setBundleName(Util.getBundleName(bundle));
                    for (String eachResource : resources) {
                        resultInfo.addResource(eachResource);
                    }
                    resultInfoList.add(resultInfo);
                }
            } else {
                LOG.warn("Bundle " + bundle.getBundleId() + " is not resolved.");
            }
        }
        return resultInfoList;
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
