JVM Perf Agent [![Build Status](https://travis-ci.org/agigleux/perf-agent.svg?branch=master)](https://travis-ci.org/agigleux/perf-agent) [![SonarQube](https://sonarqube.com/api/badges/gate?key=org.perf:perf-agent)](https://sonarqube.com/overview?id=org.perf%3Aperf-agent) [![Dependency Status](https://dependencyci.com/github/agigleux/perf-agent/badge)](https://dependencyci.com/github/agigleux/perf-agent)
==========

## Objectives

This JVM Agent is built with the aim to quickly identify:

* TOP X SQL Queries: list the first X sql queries having their max execution time part of the TOP X highest
* TOP X Costly SQL Queries: list the first X sql queries where the sum of their execution time (#Count x Mean Execution Time) is one of the TOP X highest
* TOP X Called SQL Queries: list the first X sql queries mostly called since the JVM has been started
* TOP X Methods: list of X methods costing the must in term of execution time

## DB Supported

* Oracle
* Microsoft SQL Server
* H2
* PostgreSQL
* Sybase

## How To Activate?

Add this on your JVM command line:

-javaagent:/tmp/perf-agent-1.0.jar=rank=5,reportFrequencyInSeconds=30,customPackageNamePrefix=org.perf,methodsMeasurementActivated=true

## Parameters
* rank: configure the value of the X, in TOP X ...
* reportFrequencyInSeconds: delay in seconds to report collected Metrics
* customPackageNamePrefix: used only by TOP X Methods: look for methods execution time only on Classes belonging to a package starting with this parameter
* methodsMeasurementActivated: activate or not the methods execution time

## perf-agent.log

The JVM Agent produces a file named perf-agent.log in /tmp containing all Metrics
Override src/main/resources/log4j.xml to change the directory where this file is produced.