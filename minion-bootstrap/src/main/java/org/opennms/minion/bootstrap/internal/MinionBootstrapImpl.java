package org.opennms.minion.bootstrap.internal;

import java.util.List;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.opennms.minion.api.MinionContainerManager;
import org.opennms.minion.api.MinionException;
import org.opennms.minion.impl.MinionContainerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionBootstrapImpl {
    private static final Logger LOG = LoggerFactory.getLogger(MinionBootstrapImpl.class);

    private BundleContext m_bundleContext;

    private AdminService m_adminService;
    private FeaturesService m_featuresService;
    private ConfigurationAdmin m_configurationAdmin;
    private MinionContainerManager m_containerManager;

    private String m_brokerUri;
    private String m_restRoot;

    public MinionBootstrapImpl() {
    }

    public void start() throws MinionException {
        LOG.info("MinionBootstrap starting up.");

        assert m_bundleContext != null : "BundleContext is missing!";

        if (m_adminService == null) {
            final ServiceReference<AdminService> sr = m_bundleContext.getServiceReference(AdminService.class);
            if (sr == null) {
                throw new IllegalStateException("Unable to locate AdminService!");
            }
            m_adminService = m_bundleContext.getService(sr);
        }
        if (m_featuresService == null) {
            final ServiceReference<FeaturesService> sr = m_bundleContext.getServiceReference(FeaturesService.class);
            if (sr == null) {
                throw new IllegalStateException("Unable to locate FeaturesService!");
            }
            m_featuresService = m_bundleContext.getService(sr);
        }

        assert m_adminService       != null : "AdminService is missing!";
        assert m_featuresService    != null : "FeaturesService is missing!";
        assert m_configurationAdmin != null : "ConfigurationAdmin is missing!";
        assert m_containerManager   != null : "MinionContainerManager is missing!";
        assert m_brokerUri          != null : "Broker URI is missing!";
        assert m_restRoot           != null : "OpenNMS ReST root is missing!";

        final String currentInstance = System.getProperty("karaf.name");
        if (!m_containerManager.isRootInstance(currentInstance)) {
            LOG.info("This is not the root instance, removing bootstrap.");
            for (final Bundle bundle : m_bundleContext.getBundles()) {
                if ("org.opennms.minion.bootstrap".equals(bundle.getSymbolicName())) {
                    try {
                        LOG.info("Found our bundle; uninstalling.");
                        bundle.uninstall();
                        return;
                    } catch (final BundleException e) {
                        LOG.info("Failed to uninstall ourselves.  Exiting.", e);
                        return;
                    }
                }
            }
        }

        final List<String> instanceNames = m_containerManager.getInstanceNames(false);
        if (instanceNames.contains("minion-controller")) {
            LOG.debug("Minion Controller instance already exists.  Destroying it to make way for the updated version.");
            m_containerManager.destroyInstance("minion-controller");
        }

        final MinionContainerImpl container = new MinionContainerImpl("minion-controller", "org.opennms.minion.controller");

        // add any feature repositories that also exist in the current/root instance
        for (final Repository r : m_featuresService.listRepositories()) {
            container.addFeatureRepository(r.getURI().toString());
        }

        container.addFeature("minion-controller");
        container.setConfigurationProperty("org.ops4j.pax.url.mvn", "org.ops4j.pax.url.mvn.repositories", m_restRoot + "/smnnepo@snapshots@id=opennms-repo");
        container.setConfigurationProperty("org.opennms.minion.controller", "dominionBrokerUri", m_brokerUri);
        container.setConfigurationProperty("org.opennms.minion.controller", "opennmsRestRoot", m_restRoot);

        m_containerManager.createInstance(container);
        m_containerManager.startInstance("minion-controller");

        LOG.info("MinionBootstrap started.");
    }

    public void stop() throws MinionException {
        LOG.info("MinionBootstrap shutting down.");

        final List<String> instanceNames = m_containerManager.getInstanceNames(false);
        for (final String instanceName : instanceNames) {
            m_containerManager.stopInstance(instanceName);
        }

        LOG.info("MinionBootstrap stopped.");
    }

    public void setBundleContext(BundleContext bundleContext) {
        m_bundleContext = bundleContext;
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        m_configurationAdmin = configurationAdmin;
    }

    public void setMinionContainerManager(final MinionContainerManager manager) {
        m_containerManager = manager;
    }

    public void setDominionBrokerUri(final String brokerUri) {
        m_brokerUri = brokerUri;
    }

    public void setOpennmsRestRoot(final String restRoot) {
        m_restRoot = restRoot;
    }

}
