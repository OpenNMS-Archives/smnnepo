package org.opennms.netmgt.sampler.config.internal;

import java.net.InetSocketAddress;
import java.util.List;

import org.opennms.core.config.api.ConfigurationResource;
import org.opennms.core.config.api.ConfigurationResourceException;
import org.opennms.netmgt.config.SnmpPeerFactory;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.Service;
import org.opennms.netmgt.config.monitoringLocations.LocationDef;
import org.opennms.netmgt.config.monitoringLocations.MonitoringLocationsConfiguration;
import org.opennms.netmgt.dao.api.IpInterfaceDao;
import org.opennms.netmgt.dao.api.LocationMonitorDao;
import org.opennms.netmgt.dao.api.NodeDao;
import org.opennms.netmgt.model.OnmsIpInterface;
import org.opennms.netmgt.sampler.config.SamplerConfiguration;
import org.opennms.netmgt.sampler.config.SamplerConfigurationProvider;
import org.opennms.netmgt.sampler.config.snmp.SnmpAgent;
import org.opennms.netmgt.snmp.SnmpAgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSamplerConfigurationProvider implements SamplerConfigurationProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSamplerConfigurationProvider.class);

    private LocationMonitorDao m_locationMonitorDao;
    private NodeDao m_nodeDao;
    private IpInterfaceDao m_ipInterfaceDao;
    private SnmpPeerFactory m_snmpPeerFactory;
    private ConfigurationResource<CollectdConfiguration> m_collectdResource;
    private ConfigurationResource<MonitoringLocationsConfiguration> m_monitoringLocationsResource;

    @Override
    public SamplerConfiguration getConfigForLocation(final String location) {
        MonitoringLocationsConfiguration monitoringConfig;
        try {
            monitoringConfig = m_monitoringLocationsResource.get();
        } catch (final ConfigurationResourceException e) {
            throw new IllegalStateException("Unable to get monitoring configuration!", e);
        }

        LOG.debug("monitoring config: {}", monitoringConfig);
        final LocationDef def = monitoringConfig.getLocation(location);
        if (def == null) {
            LOG.warn("Unable to find monitoring location {}", location);
            return null;
        }

        final String collectionPackageName = def.getCollectionPackageName();
        if (collectionPackageName == null || "".equals(collectionPackageName)) {
            LOG.warn("Monitoring location {} does not have a collection package defined.", location);
            return null;
        }

        try {
            final CollectdConfiguration collectdConfig = m_collectdResource.get().getCollectdConfigurationForPackage(collectionPackageName);
            final org.opennms.netmgt.config.collectd.Package pack = collectdConfig.getPackages().get(0);

            final SamplerConfiguration samplerConfiguration = new SamplerConfiguration();

            // TODO: find all beans that implement ServiceCollector and figure out ServiceCollector -> FooAgent() class type
            for (final Service svc : pack.getServices()) {
                if (svc.getName().equalsIgnoreCase("SNMP")) {
                    final List<OnmsIpInterface> ifaces = m_ipInterfaceDao.findByServiceType(svc.getName());
                    for (final OnmsIpInterface iface : ifaces) {
                        final SnmpAgentConfig agentConfig = m_snmpPeerFactory.getAgentConfig(iface.getIpAddress());
                        final InetSocketAddress socketAddress = new InetSocketAddress(agentConfig.getEffectiveAddress(), agentConfig.getPort());
                        samplerConfiguration.addAgent(new SnmpAgent(socketAddress, iface.getNode().getSysObjectId(), null));
                    }
                }
            }

            
            return samplerConfiguration;
        } catch (final ConfigurationResourceException e) {
            throw new IllegalStateException("Failed to get collectd configuration!", e);
        }
    }

    public void setLocationMonitorDao(final LocationMonitorDao locationMonitorDao) {
        m_locationMonitorDao = locationMonitorDao;
    }

    public void setCollectdConfigurationResource(final ConfigurationResource<CollectdConfiguration> collectdConfigurationResource) {
        m_collectdResource = collectdConfigurationResource;
    }

    public void setMonitoringLocationsConfigurationResource(final ConfigurationResource<MonitoringLocationsConfiguration> monitoringLocationsResource) {
        m_monitoringLocationsResource = monitoringLocationsResource;
    }

    public void setIpInterfaceDao(final IpInterfaceDao ipInterfaceDao) {
        m_ipInterfaceDao = ipInterfaceDao;
    }

    public void setSnmpPeerFactory(final SnmpPeerFactory snmpPeerFactory) {
        m_snmpPeerFactory = snmpPeerFactory;
    }

}
