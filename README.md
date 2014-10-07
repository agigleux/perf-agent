## Objectives

This JVM Agent is built with the aim to quickly identify:

* TOP X SQL Queries: list the first X sql queries where their Max execution time is one of the TOP Xx highest
* TOP X Costly SQL Queries: list the first X sql queries where the sum of their execution time (# Count x Mean Execution Time) is one of the TOP X highest
* TOP X Called SQL Queries: list the first X sql queries the mostly called since the JVM has started up
* TOP X Methods: list of X methods costing the must in term of execution time

## How To Activate?

In order to activate it you have to update your command with:

JVM_OPTIONS="-javaagent:/tmp/perf-agent-0.2-SNAPSHOT.jar=rank=5,reportFrequencyInSeconds=30,customPackageNamePrefix=com.sonarsource,methodsMeasurementActivated=false ${JVM_OPTIONS}"

