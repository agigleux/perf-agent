package org.perf.agent;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;

import org.perf.agent.metrics.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * References:
 * - http://chimpler.wordpress.com/2013/11/05/implementing-a-java-agent-to-instrument-code/
 * - http://www.javabeat.net/introduction-to-java-agents/
 */
public class MethodTimerAgent {

  private static final Logger LOGGER = LoggerFactory.getLogger(MethodTimerAgent.class);

  private static final int DEFAULT_RANK = 5;
  private static final int REPORT_FREQUENCY_SECONDS = 30;

  public static final String PARAM_RANK = "rank";
  public static final String PARAM_REPORT_FREQUENCY_SECONDS = "reportFrequencyInSeconds";
  public static final String PARAM_CUSTOM_PACKAGE_PREFIX = "customPackageNamePrefix";
  public static final String PARAM_METHODS_MEASUREMENT = "methodsMeasurementActivated";

  private MethodTimerAgent() {
  }

  public static int getRank(Map<String, String> properties) {
    int r = DEFAULT_RANK;
    try {
      String v = properties.get(PARAM_RANK);
      if (v != null && v.length() > 0) {
        r = Integer.valueOf(v);
      }
    } catch (NumberFormatException e) {
      LOGGER
        .info("Value given for param '" + PARAM_RANK + "' is not an Integer. Default value '" + DEFAULT_RANK + "' will be applied instead.");
    }
    return r;
  }

  private static int getReportFrequencyInSeconds(Map<String, String> properties) {
    int r = REPORT_FREQUENCY_SECONDS;
    try {
      String v = properties.get(PARAM_REPORT_FREQUENCY_SECONDS);
      if (v != null && v.length() > 0) {
        r = Integer.valueOf(v);
      }
    } catch (NumberFormatException e) {
      LOGGER.info("Value given for param '" + PARAM_REPORT_FREQUENCY_SECONDS + "' is not an Integer. Default value '"
        + REPORT_FREQUENCY_SECONDS + "' will be applied instead.");
    }
    return r;
  }

  /**
   * Supported Parameters:
   * - rank: number of entries to report on the Slf4jTopReporter (default: 5)
   * - reportFrequencyInSeconds: frequency of reporting (default: 30 seconds)
   * - methodsMeasurementActivated: true|false: whether or not to measure the processing time of each method belonging to 'customPackageNamePrefix'
   * - customPackageNamePrefix: the package a class must belong to, to be taken into account by Slf4jTopReporter
   * - csvDirectoryPath: where to generate the CSV file containing all timings
   * agentArguments must follow this pattern: rank=10,reportFrequencyInSeconds=30,customPackageNamePrefix=com.mypackage,csvDirectoryPath=/tmp/agent
   */
  public static void premain(String agentArguments, Instrumentation instrumentation) {
    LOGGER.info("Starting MethodTimer Agent");
    LOGGER.info("Agent Arguments: " + agentArguments);

    Map<String, String> properties = new HashMap<>();

    if (agentArguments != null) {
      for (String propertyAndValue : agentArguments.split(",")) {
        String[] tokens = propertyAndValue.split("=", 2);
        if (tokens.length != 2) {
          continue;
        }
        properties.put(tokens[0], tokens[1]);
      }
    }

    // MetricReporter.enableConsoleReporter();
    MetricReporter.enableLoggerReporter();
    MetricReporter.enableCSVReporter();
    MetricReporter.enableTopRankingLoggerReporter(getRank(properties), getReportFrequencyInSeconds(properties));

    instrumentation.addTransformer(new TimedClassTransformer(properties));
  }

}
