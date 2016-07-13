## Objectives

This JVM Agent is built with the aim to quickly identify:

* TOP X SQL Queries: list the first X sql queries where their max execution time is one of the TOP X highest
* TOP X Costly SQL Queries: list the first X sql queries where the sum of their execution time (#Count x Mean Execution Time) is one of the TOP X highest
* TOP X Called SQL Queries: list the first X sql queries the mostly called since the JVM has started up
* TOP X Methods: list of X methods costing the must in term of execution time

## DB Supported

* Sybase
* Oracle
* Microsoft SQL Server
* H2
* Postgresql

## How To Activate?

In order to activate it you have to update your command line with:

-javaagent:/tmp/perf-agent-1.0.jar=rank=5,reportFrequencyInSeconds=30,customPackageNamePrefix=org.perf,methodsMeasurementActivated=true

## Parameters
* rank: configure the value of the X, in TOP X ...
* reportFrequencyInSeconds: delay in seconds to report collected Metrics
* customPackageNamePrefix: used only by TOP X Methods: look for methods execution time only on Classes belonging to a package starting with thie parameter
* methodsMeasurementActivated: activate or not the methods execution time

## perf-agent.log

The JVM Agent produces a file named perf-agent.log in /tmp containing all Metrics
Override src/main/resources/log4j.xml to change the directory where this file is produced.