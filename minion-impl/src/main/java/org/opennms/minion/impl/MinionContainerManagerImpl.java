package org.opennms.minion.impl;

import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.karaf.admin.AdminService;
import org.apache.karaf.admin.Instance;
import org.apache.karaf.admin.InstanceSettings;
import org.opennms.minion.api.MinionContainer;
import org.opennms.minion.api.MinionContainerConfiguration;
import org.opennms.minion.api.MinionContainerManager;
import org.opennms.minion.api.MinionException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionContainerManagerImpl implements MinionContainerManager {
    private static final Logger LOG = LoggerFactory.getLogger(MinionContainerManagerImpl.class);

    private static final int MIN_PORT_NUMBER = 1024;
    private static final int MAX_PORT_NUMBER = 65535;

    private AdminService m_adminService;
    private ConfigurationAdmin m_configurationAdmin;

    private void closeQuietly(final Closeable... closeables) {
        for (final Closeable c : closeables) {
            if (c == null) {
                continue;
            }
            try {
                c.close();
            } catch (final IOException ioe) {
                LOG.debug("Failed to close {}", c, ioe);
            }
        }
    }

    @Override
    public void createInstance(final MinionContainer fromContainer) throws MinionException {
        final String name = fromContainer.getName();
        final List<String> featureRepositories = fromContainer.getFeatureRepositories();
        final List<String> features = fromContainer.getFeatures();

        try {
            final Instance rootInstance = getRootInstance();

            if (rootInstance == null) {
                throw new MinionException("Unable to determine root instance!");
            }
            final InstanceSettings settings = new InstanceSettings(getFreePort(rootInstance.getSshPort()), getFreePort(rootInstance.getRmiRegistryPort()), getFreePort(rootInstance.getRmiServerPort()), null, rootInstance.getJavaOpts(), featureRepositories, features);
            m_adminService.cloneInstance(rootInstance.getName(), name, settings);

            final Instance instance = getInstance(name);
            final String location = instance.getLocation();
            final File f = new File(location + File.separator + "etc" + File.separator + "users.properties");
            LOG.info("Updating {}", f);

            final Properties users = new Properties();
            FileReader fr = null;
            FileWriter fw = null;
            try {
                fr = new FileReader(f);
                users.load(fr);

                if (!users.containsKey("admin")) {
                    users.put("admin", "admin,admin");
                }

                fw = new FileWriter(f);
                users.store(fw, null);
            } catch (final IOException ioe) {
                LOG.warn("Failed to add 'admin' user to {}", f, ioe);
            } finally {
                closeQuietly(fr, fw);
            }

            setConfigurationProperties(fromContainer, instance);
        } catch (final Exception e) {
            throw new MinionException("Failed to create '" + name + "' instance.", e);
        }
    }

    @Override
    public void destroyInstance(final String instanceName) throws MinionException {
        final Instance instance = getInstance(instanceName);
        if (instance == null) {
            throw new MinionException("destroyInstance() Instance ('" + instanceName + "') does not exist or is invalid.");
        }
        // destroy the existing one, just in case
        try {
            instance.stop();
        } catch (final Exception e) {
            LOG.warn("Failed to stop instance '{}', trying to destroy it.", instanceName, e);
            try {
                instance.destroy();
            } catch (final Exception ex) {
                throw new MinionException("Failed to destroy instance '" + instanceName + "'", e);
            }
        }
    }

    @Override
    public void startInstance(final String instanceName) throws MinionException {
        final Instance instance = getInstance(instanceName);
        if (instance == null) {
            throw new MinionException("startInstance(): Instance ('" + instanceName + "') does not exist or is invalid.");
        }
        try {
            instance.start(null);
            waitForInstance(instance);
        } catch (final Exception e) {
            throw new MinionException("Failed to start instance '" + instanceName + "'.", e);
        }
    }

    @Override
    public void stopInstance(final String instanceName) throws MinionException {
        final Instance instance = getInstance(instanceName);
        if (instance == null) {
            throw new MinionException("startInstance(): Instance ('" + instanceName + "') does not exist or is invalid.");
        }
        try {
            instance.stop();
        } catch (final Exception e) {
            throw new MinionException("Failed to stop instance '" + instanceName + "'.", e);
        }
    }

    @Override
    public boolean isRootInstance(final String instanceName) {
        final Instance instance = getInstance(instanceName);
        if (instance != null) {
            return instance.isRoot();
        }
        return false;
    }

    @Override
    public List<String> getInstanceNames(final boolean includeRoot) {
        final List<String> names = new ArrayList<String>();
        for (final Instance instance : m_adminService.getInstances()) {
            if (instance.isRoot() && !includeRoot) {
                continue;
            }
            names.add(instance.getName());
        }
        return names;
    }

    protected int getFreePort(final int currentPort) {
        final List<Integer> skip = new ArrayList<Integer>();

        for (final Instance i : m_adminService.getInstances()) {
            skip.add(i.getSshPort());
            skip.add(i.getRmiRegistryPort());
            skip.add(i.getRmiServerPort());
        }

        int port = currentPort;
        do {
            port += 1;
            
            if (skip.contains(port)) {
                // known taken port, skip it
                continue;
            }

            if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
                throw new IllegalArgumentException("Unable to determine a free port: " + port);
            }

            ServerSocket ss = null;
            DatagramSocket ds = null;
            try {
                ss = new ServerSocket(port);
                ss.setReuseAddress(true);
                ds = new DatagramSocket(port);
                ds.setReuseAddress(true);
                return port;
            } catch (IOException e) {
                // ignore IOException, just means we couldn't use the port
            } finally {
                closeQuietly(ds, ss);
            }
        } while (true);
    }

    protected Instance getRootInstance() {
        Instance rootInstance = null;
        for (final Instance instance : m_adminService.getInstances()) {
            if (instance.isRoot()) {
                rootInstance = instance;
                break;
            }
        }
        return rootInstance;
    }

    protected Instance getInstance(final String instanceName) {
        return m_adminService.getInstance(instanceName);
    }

    protected MinionContainerImpl cloneConfigurationProperties(final Instance fromInstance, final MinionContainer toContainer) throws MinionException {
        final MinionContainerImpl ret = MinionContainerImpl.fromContainer(toContainer);

        final Map<String,MinionContainerConfigurationImpl> configs = new HashMap<>();
        for (final MinionContainerConfiguration conf : toContainer.getConfigurations()) {
            configs.put(conf.getPid(), MinionContainerConfigurationImpl.fromConfiguration(conf));
        }

        for (final String pid : new String[] {
            "org.apache.karaf.features.repos",
            "org.apache.karaf.jaas",
            "org.apache.karaf.kar"
        }) {
            LOG.info("Cloning configuration for pid {} to {}", pid, toContainer.getName());
            try {
                final Configuration from = m_configurationAdmin.getConfiguration(pid);
                MinionContainerConfigurationImpl to = configs.get(pid);
                if (to == null) {
                    to = new MinionContainerConfigurationImpl(pid);
                }

                final Dictionary<String, Object> fromProperties = from.getProperties();
                final Enumeration<String> keys = fromProperties.keys();
                while (keys.hasMoreElements()) {
                    final String key = keys.nextElement();
                    if (to.containsKey(key)) {
                        continue;
                    } else {
                        final String value = fromProperties.get(key).toString();
                        to.setProperty(key, value);
                    }
                }
                configs.put(pid, to);
            } catch (final IOException e) {
                LOG.warn("Failed to get configuration for PID {}", pid, e);
            }
        }

        ret.setConfigurations(new ArrayList<MinionContainerConfiguration>(configs.values()));
        return ret;
    }

    protected void setConfigurationProperties(final MinionContainer fromContainer, final Instance toInstance) throws MinionException {
        final String instanceLocation = toInstance.getLocation();
        for (final MinionContainerConfiguration config : fromContainer.getConfigurations()) {
            final String pid = config.getPid();
            try {
                final Configuration configuration = m_configurationAdmin.getConfiguration(pid);
                final Hashtable<String,Object> properties = new Hashtable<>();
                final Dictionary<String,Object> existing = configuration.getProperties();
                for (final Enumeration<String> keys = existing.keys(); keys.hasMoreElements();) {
                    final String key = keys.nextElement();
                    Object value = existing.get(key);
                    if (value != null && value.toString().contains("${karaf.base}")) {
                        value = value.toString().replace("${karaf.base}", instanceLocation);
                    }
                    properties.put(key, value);
                }
                properties.putAll(config.getProperties());
                configuration.update(properties);
            } catch (final IOException e) {
                throw new MinionException("Failed to get configuration for pid '" + pid + "' while configuring container '" + fromContainer.getName() + "'", e);
            }
        }
    }

    protected String loadProperty(final MinionContainer fromContainer, final String propName) throws MinionException {
        final Configuration config = getConfiguration(fromContainer);
        final Dictionary<String,Object> properties = config.getProperties();
        if (properties == null) {
            return null;
        }

        final String property = (String)properties.get(propName);
        return property;
    }

    protected void saveProperty(final MinionContainer inContainer, final String key, final String value) throws MinionException {
        final Configuration config = getConfiguration(inContainer);
        final Dictionary<String,Object> properties = config.getProperties() == null? new Hashtable<String,Object>() : config.getProperties();
        properties.put(key, value);
        try {
            config.update(properties);
        } catch (final IOException e) {
            final MinionException ce = new MinionException("Failed to update configuration.", e);
            LOG.error("Failed to update configuration.", e);
            throw ce;
        }
    }

    protected Configuration getConfiguration(final MinionContainer forContainer) throws MinionException {
        final String pid = forContainer.getPid();
        try {
            final Configuration configuration = m_configurationAdmin.getConfiguration(pid);
            if (configuration == null) {
                final MinionException e = new MinionException("The OSGi configuration (admin) registry was found for pid " + pid + ", but a configuration could not be located/generated.  This shouldn't happen.");
                LOG.error("Error getting configuration.", e);
                throw e;

            }
            return configuration;
        } catch (final IOException e) {
            final MinionException ce = new MinionException("Failed to get configuration from OSGi configuration registry for pid " + pid + ".", e);
            LOG.error("Error getting configuration.", e);
            throw new MinionException(ce);
        }
    }

    protected void waitForInstance(final Instance instance) throws MinionException {
        final long waitUntil = System.currentTimeMillis() + (60 * 2 * 1000); // 2 minutes
        final String instanceName = instance.getName();

        while (System.currentTimeMillis() < waitUntil) {
            try {
                final String s = instance.getState();
                if (Instance.STARTED.equals(s)) {
                    LOG.info("Instance '{}' has started.", instanceName);
                    break;
                }
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                final MinionException minionException = new MinionException("Interrupted while waiting for instance '" + instanceName + "' to start.", e);
                LOG.error(minionException.getMessage(), e);
                Thread.currentThread().interrupt();
                throw minionException;
            } catch (final Exception e) {
                final MinionException minionException = new MinionException("Failed to get state from instance '" + instanceName + "' while waiting for it to start.", e);
                LOG.error(minionException.getMessage(), e);
                throw minionException;
            }
        }
    }

    public void setAdminService(final AdminService adminService) {
        m_adminService = adminService;
    }

    public void setConfigurationAdmin(final ConfigurationAdmin configurationAdmin) {
        m_configurationAdmin = configurationAdmin;
    }

}
