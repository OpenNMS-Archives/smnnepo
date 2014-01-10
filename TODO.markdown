TODO
====

In order of importance:

 1. Integrate Sampler with OpenNMS
    - Query nodes from 'package' associate with sampler (NOC side)
    - Add look up of agent credentials for snmp querying (NOC side - part of config)
    - Setup schedule for nodes in package (Store side using config from NOC)
    - Update agent to use 'nodeid' as agent identifier
    - implement 'updateConfig' on snmpAgentRepository (save config Store side)
 1. Create 'MATH' api for applying math to Cassandra repository results
    - 'bucketize' raw data (including missed data collection)
    - calculate rate for COUNTER data
    - create general framework for apply math to results 
    - define 'rollup' math
 1. Brainstorm javascript plugin architecture for graphing engines
 1. Finalize REST api for use by graphing
 1. Graphing WEB UI
    - add link to node page for launching 'new' graph ui
    - list graphable resources available on indicated node and allow user to select (query resources by 'node'/'endpoint')
    - show graphs that apply to indicated resources (query metrics by resource and find graphs that apply to metrics)
 1. Enable collection of metric groups not associated with a resource type. -- *Matt*
 1. Remote event collection.
 1. Remote polling
 1. Netflow
    - Data Collection
    - Data persistence
    - define graph/report requirements
    - model changes/improvements/alternatives?
 1. Improve routing code
    - move routes into actual bundle
    - provide a command or something that turns them routes back into configuration
 1. netmgt.api.sample: javadocs
 1. Good tests for api.sample implementations (then remove old from api.sample)
 1. Write tests for Cassandra sample repository
 1. How to apply Cassandra consistency levels in sample repo
 1. Merge base class with Cassandra sample repository
 1. SampleSet is not great; Externalizable? Other, more compact serialization?

Space Concerns
--------------
 1. Date/time-based sharding of rows in Cassandra sample repo (limit by column per row)
 1. Caching of resource attributes (Cassandra)


Performance Concerns
--------------------
 1. Provide persistence of 'bucket'/'rollup' math if latency indicates the need for it
 1. sampler.snmp.Column: don't recreate Metric from accessor


Completed
---------
 1. Create OSGi bundle of Cassandra driver.
 1. Get `JrrdSampleRespoitory` writing to onms-compatible paths (requires
    some refactor of `Resource`).
 1. Reorder / prioritize this list!
 1. Implement storage of resource attributes (Cassandra repository).
 1. Implement storage of resource attributes (Jrrd repository).
 1. Implement:
    - Query resource attributes (Cassandra)
    - Query resource attributes (Jrrd)
