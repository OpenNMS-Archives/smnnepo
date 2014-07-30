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

## sampler-scheduler
* *scheduler*: Bean that implements the SchedulerService interface. This bean takes incoming PackageAgentList messages, adds them to a scheduler, and when the task is scheduled to execute it enqueues them to all services registered under the *org.opennms.netmgt.api.sample.AgentDispatcher* interface.

## sampler-config-snmp
This project uses Camel to load SNMP-specific configuration data via REST from the OpenNMS server and then provides that configuration data as OSGi services for use by the *sampler-snmp* project.
~~~
<service ref="snmpConfigFactory" interface="org.opennms.netmgt.api.sample.support.SingletonBeanFactory">
  <service-properties>
    <entry key="beanClass" value="org.opennms.netmgt.config.snmp.SnmpConfig" />
  </service-properties>
</service>

<service ref="snmpMetricRepository">
  <interfaces>
    <value>org.opennms.netmgt.api.sample.CollectionConfiguration</value>
    <value>org.opennms.netmgt.api.sample.MetricRepository</value>
  </interfaces>
  <service-properties>
    <entry key="protocol" value="SNMP"/>
  </service-properties>
</service>

<service ref="snmpAgentRepository" interface="org.opennms.netmgt.api.sample.AgentRepository">
  <service-properties>
    <entry key="protocol" value="SNMP"/>
  </service-properties>
</service>
~~~

## sampler-snmp
This context registers a bean named *snmpSampler* as an *org.opennms.netmgt.api.sample.AgentDispatcher* which forwards the message into the *seda:collectAgent* endpoint in the **collectAgent** route.
* blueprint.xml
    * **collectAgent**: Enhances the Agent message with SNMP-specific information (OIDs to collect, SNMP credentials) and then collects it using the *snmpCollector* bean.
    * **sampleSet**: Sends the completed SampleSet to all registered *org.opennms.netmgt.api.sample.SampleSetDispatcher* services.
    * **seda:saveToRepository**: This endpoint is serviced by the *sampleSetDispatcher* bean. This whiteboard consumes from the *seda:saveToRepository* endpoint and invokes the *save* method on all OSGi services that are registered with the *org.opennms.netmgt.api.sample.SampleSetDispatcher* interface.
    ~~~
    <bean id="sampleSetDispatcher" class="org.opennms.netmgt.api.sample.support.DispatcherWhiteboard">
        <argument value="seda:saveToRepository"/>
        <property name="context" ref="blueprintBundleContext"/>
        <property name="messageClass" value="org.opennms.netmgt.api.sample.SampleSet"/>
        <property name="serviceClass" value="org.opennms.netmgt.api.sample.SampleSetDispatcher"/>
        <property name="methodName" value="save"/>
    </bean>
    ~~~

## sampler-repo
## sampler-repo-exclude
## sampler-repo-webapp

## sampler-routes

## sample-storage-cassandra
## sample-storage-newts
## sample-storage-rrd
