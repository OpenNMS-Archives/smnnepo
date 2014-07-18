# OpenNMS Minion

OpenNMS Minion is an instance of the Karaf OSGi service that can run distributed OpenNMS components that are packaged as Karaf features. Minion devices are configured with a minimum amount of information necessary to connect to the OpenNMS machine and identify themselves to it. The operation of each Minion is controlled from the Dominion service running on a central OpenNMS installation.

# Prerequisites

## User Management

User management of each Minion instance is performed by editing the */etc/sysconfig/smnnepo* file to provide credentials for accessing the public interfaces of the OpenNMS system.

```
USERNAME="admin"
PASSWORD="admin"
OPENNMS="http://127.0.0.1:8980"
BROKER="tcp://127.0.0.1:61616"
LOCATION="RDU"
```


* **Risk:** Initial setup requires creation or editing of this file on all Minion devices which could entail a lot of overhead on large Minion installations.
* **Risk:** Credentials will likely be shared among all Minion devices unless user administration of individual Minion accounts is feasible on the OpenNMS side (jetty, activemq).


* **Future:** Dynamically generate credentials for each Minion during a handshake/pairing setup process?
* **Future:** Distribute certificates to Minion devices for clientAuth SSL?

## Feature Repository

Minion devices download executable code in the form of OSGi bundle JAR files from a Maven repository. This Maven repository can either be the local Maven repository used by a developer, a filesystem source, or a webapp running on the OpenNMS system. The repository should contain the feature definition XMLs for all desired Minion features, the OSGi bundles that implement the features, and all of the transitive dependencies of the features.

### sampler-repo

The sampler-repo project is used to construct the feature Maven repository directory structure. If you wish to add Karaf features to this repository, you need to edit the configuration for the features-maven-plugin in the POM for the sampler-repo project.

### sampler-repo-webapp

The sampler-repo contents are wrapped in a webapp that can be served from the OpenNMS jetty server to provide a centralized distribution point for all connected Minion devices. This is the default configuration for Minion deployments.


* **Risk:** Jetty cannot handle amount of connections required when Minion devices download JAR files leading to DDoS of normal OpenNMS webapp
* **Risk:** Network traffic required to install features on Minion devices could be too high (50MB per instance * 4000 instances = 200GB of network traffic per deployment)


* **Future:** Ability to run repo-webapp as a Karaf WAR feature so that clusters could share one copy?

### Alternatives to sampler-repo-webapp

* **Future:** RPM installation of feature repository on Minion devices to reduce/eliminate network traffic required to install Minion features

## Configuration

The sampler-repo-webapp on OpenNMS provides Karaf scripts that can be used to install the Minion services on a remote machine. These scripts are invoked by the */etc/init.d/smnnepo* init file and the contents of the */etc/sysconfig/smnnepo* file are used as arguments to the script.
~~~
./bin/client -r 30 -a 8201 "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" root "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
~~~
The */etc/init.d/smnnepo* script sets up the root Karaf instance and if subinstances are created, the script is invoked again for each subinstance.
~~~
./bin/client -r 10 -a $PORT "source" "\"$SCRIPTDIR/smnnepo-setup.karaf\"" $INSTANCE "\"$USERNAME\"" "\"$PASSWORD\"" "\"$OPENNMS\"" "\"$LOCATION\"" >/tmp/smnnepo.log 2>&1
~~~

# Runtime Operation

The Minion process is started by using the *smnnepo* init.d service script. The startup procedure is the following:

* Start Karaf.
* Execute a hard-coded *.karaf* script that is hosted in the *sampler-repo-webapp* WAR that sets up the Minion Karaf instances in the desired manner.
* The *activemq* instance starts an ActiveMQ broker that listens for localhost TCP connections on port 61716 and connects to the remote OpenNMS ActiveMQ service. The local ActiveMQ URI is:
    ~~~
    tcp://127.0.0.1:61716/
    ~~~
    and the OpenNMS ActiveMQ URI is:
    ~~~
    tcp://[opennms_address]:61616/
    ~~~
* The *minion* instance installs the Minion Controller project that communicates status back to the Dominion service on the OpenNMS server.
* The *sampler* instance runs the SNMP collector code to perform remote collections.


* **Risk:** If connectivity to the OpenNMS machine is unavailable, it is impossible to install the required features. We currently have no way to retry or reconnect to OpenNMS in the event of a failure to install the features. This could be added to the Minion init script.
* **Risk:** We have no checks to ensure that the features were installed or functioning properly. These checks should be added to the init script and should be communicated back to OpenNMS by using the Minion Controller service (if ActiveMQ connectivity can be established).


* **Future:** Change the script based setup onto something more dynamic so that Karaf features can be configured and provisioned on the OpenNMS Dominion side. Should we do this with REST or ActiveMQ?
* **Future:** Should the Karaf scripts be dynamic content served by the OpenNMS system? ie. a REST call that takes parameters like http://127.0.0.1:8980/rest/minion/bootstrap?location=myLocation



## REST Service

Sampler features may require access to the REST API on OpenNMS for configuration and inventory information.

* **Risk:** There is no way to guarantee API compatibility besides unit testing.