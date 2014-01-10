
-- NOTES ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
-- This schema, as of yet, lacks the means to keep rows from growing
-- unbounded.  One of sharding (by row key, table, etc), or column TTLs,
-- or both, will need to be added.
--
-- This schema does not yet support storage of rollups. 
-- ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

DROP KEYSPACE seriesdb;

CREATE KEYSPACE seriesdb
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};

USE seriesdb;

CREATE TABLE samples (
    resource      text,
    collected_at  timestamp,
    metric        text,
    value         blob,
    PRIMARY KEY(resource, collected_at, metric)
);

-- Name/value pairs associated w/ resource
CREATE TABLE resources (
    resource   text,
    attribute  text,
    value      text,
    PRIMARY KEY(resource, attribute)
);

-- Metrics indexed by resource
CREATE TABLE metrics_x_resource (
    resource   text,
    group      text,
    metric     text,
    PRIMARY KEY(resource, group, metric)
);

-- Resources indexed by endpoint
CREATE TABLE resources_x_endpoint (
    endpoint   text,
    resource   text,
    PRIMARY KEY(endpoint, resource)
);
