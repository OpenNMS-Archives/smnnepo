package org.opennms.minion.impl;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
        LOG.info("createInstance: {}", fromContainer);
        final String name = fromContainer.getName();
        final List<String> featureRepositories = fromContainer.getFeatureRepositories();
        final List<String> features = fromContainer.getFeatures();

        Reader r = null;
        Writer w = null;
        try {
            final Instance rootInstance = getRootInstance();

            if (rootInstance == null) {
                throw new MinionException("Unable to determine root instance!");
            }
            final InstanceSettings settings = new InstanceSettings(getFreePort(rootInstance.getSshPort()), getFreePort(rootInstance.getRmiRegistryPort()), getFreePort(rootInstance.getRmiServerPort()), null, rootInstance.getJavaOpts(), featureRepositories, features);
            m_adminService.createInstance(name, settings);

            final Instance instance = getInstance(name);
            final MinionContainerImpl updatedContainer = cloneConfigurationProperties(rootInstance, fromContainer);
            setConfigurationProperties(updatedContainer, instance);

            final String location = instance.getLocation();
            final File f = new File(location + File.separator + "etc" + File.separator + "users.properties");

            LOG.info("Updating {}", f);

            final Properties users = new Properties();

            if (f.exists()) {
                r = new FileReader(f);
                users.load(r);
            }

            if (!users.containsKey("admin")) {
                users.put("admin", "admin,admin");
            }

            w = new FileWriter(f);
            users.store(w, null);
        } catch (final Exception e) {
            throw new MinionException("Failed to create '" + name + "' instance.", e);
        } finally {
            closeQuietly(r, w);
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
        LOG.info("Setting configuration properties in instance location {} for container {}: {}", instanceLocation, fromContainer.getName(), fromContainer.getConfigurations());
        for (final MinionContainerConfiguration config : fromContainer.getConfigurations()) {
            final String pid = config.getPid();

            final File f = new File(instanceLocation + File.separator + "etc" + File.separator + pid + ".cfg");
            Reader r = null;
            Writer w = null;
            BufferedWriter bw = null;
            try {
                final Properties existingProps = new Properties();
                
                if (f.exists()) {
                    r = new FileReader(f);
                    existingProps.load(r);
                }

                final Properties newProps = new Properties(existingProps);

                for (final Entry<Object,Object> entry : existingProps.entrySet()) {
                    String key = entry.getKey().toString();
                    if (existingProps.get(key) != null) {
                        String value = existingProps.get(key).toString();
                        if (value != null && value.contains("${karaf.base}")) {
                            value = value.replace("${karaf.base}", instanceLocation);
                            newProps.put(key.toString(), value);
                        }
                    }
                }

                for (final Entry<String,String> entry : config.getProperties().entrySet()) {
                    final String key = entry.getKey();
                    String value = entry.getValue();
                    if (value != null && value.contains("${karaf.base}")) {
                        value = value.replace("${karaf.base}", instanceLocation);
                    }
                    newProps.put(key, value);
                }

                w = new FileWriter(f);
                bw = new BufferedWriter(w);
                for (final Entry<Object,Object> entry : newProps.entrySet()) {
                    LOG.info("{}: {}={}", f, entry.getKey(), entry.getValue());
                    bw.write(entry.getKey() + " = " + entry.getValue() + "\n");
                }
            } catch (final IOException e) {
                LOG.error("Unable to set configuration properties in {}", f, e);
                throw new MinionException("Unable to set configuration properties in " + f, e);
            } finally {
                closeQuietly(r, bw, w);
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
