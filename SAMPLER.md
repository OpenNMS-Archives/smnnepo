# Sampler Architecture
This document should help you understand the message flow through the Sampler projects. The captions in **bold** denote Camel route IDs. In general, within each project messages are passed using a Camel context definition that is defined in the *OSGI-INF/blueprint/.xml* files. Between projects, messages are passed by invoking methods on a DispatcherWhiteboard. This class implements the whiteboard pattern, invoking the specified 1-argument messaging method on any object instance that is registered in OSGi for a particular interface.

By using the whiteboard pattern, the modules can be completely decoupled from one another. This means that the larger modules do not have any runtime dependencies on one another and can be loaded in any order. However, if messages are passed to a DispatcherWhiteboard and zero services are registered for the interface that services that endpoint, then the messages will be dropped at that point in the processing.

## sample-api
Contains API and utility code that is reused or implemented in other modules.

## sampler-config
* blueprint-sampler-config.xml
    * **triggerStartSamplerConfig**: Fires once to endpoint direct:start to start up all messaging
    * **startLoadConfigurations**: Load all configuration objects by fetching REST content from the OpenNMS server
    * **schedulerStart**: BAD NAME (doesn't really start the scheduler or do anything really)
    * **loadCollectionPackages**: Splits the collectd configuration into packages and then into the individual services within the package. Splits the individual service messages based on protocol, eg. SNMP or JMX (this is unnecessary since there is no special processing per service yet).
    * **loadSnmpAgents/loadJmxAgents**: Does nothing. Could be used to add special protocol-specific processors.
    * **loadCollectionPackages**: Combines the package configuration with the agent list to create a complete configuration (org.opennms.netmgt.api.sample.PackageAgentList) for each agent.
    * **seda:scheduleAgents**: This endpoint is serviced by the *schedulingDispatcher* bean. This bean is an OSGi whiteboard which consumes from the *seda:scheduleAgents* endpoint and invokes the *schedule* method on all OSGi services that are registered with the *org.opennms.netmgt.api.sample.support.SchedulerService* interface.
    ~~~
    <bean id="schedulingDispatcher" class="org.opennms.netmgt.api.sample.support.DispatcherWhiteboard">
        <argument value="seda:scheduleAgents"/>
        <property name="context" ref="blueprintBundleContext"/>
        <property name="messageClass" value="org.opennms.netmgt.api.sample.PackageAgentList"/>
        <property name="serviceClass" value="org.opennms.netmgt.api.sample.support.SchedulerService"/>
        <property name="methodName" value="schedule"/>
    </bean>
    ~~~

## sampler-config-snmp

## sampler-repo
## sampler-repo-exclude
## sampler-repo-webapp

## sampler-routes

## sampler-scheduler

## sampler-snmp

## sample-storage-cassandra
## sample-storage-newts
## sample-storage-rrd
