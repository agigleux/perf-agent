package org.perf.agent.metrics;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;

public class MetricReporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricReporter.class);

  private static final MetricRegistry METRIC_REGISTRY = new MetricRegistry();

  private static MessageDigest MESSAGE_DIGEST;

  public static final String PATH_TO_REPORTTIME_METHOD = "org.perf.agent.metrics.MetricReporter.reportSQLTime";

  static {
    try {
      MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      LOGGER.error("not able to find MessageDigest MD5,  WTF in your install?", e);
      System.exit(-1); // NOSONAR
    }
  }

  public static void enableConsoleReporter() {
    LOGGER.info("Init Console Reporter");

    final ConsoleReporter reporter = ConsoleReporter.forRegistry(METRIC_REGISTRY).convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS).build();
    reporter.start(1, TimeUnit.MINUTES);
  }

  public static void enableLoggerReporter() {
    LOGGER.info("Init Logger Reporter");

    final Slf4jReporter reporter = Slf4jReporter.forRegistry(METRIC_REGISTRY).outputTo(LoggerFactory.getLogger("org.metrics"))
      .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
    reporter.start(1, TimeUnit.MINUTES);
  }

  public static void enableCSVReporter() {
    LOGGER.info("Init CSV Reporter");

    String tempDir = System.getProperty("java.io.tmpdir");
    File tempFile = new File(tempDir);
    File outputDir = new File(tempFile, "/agent/");
    if (!outputDir.exists()) {
      outputDir.mkdirs();
    }

    final CsvReporter reporter = CsvReporter.forRegistry(METRIC_REGISTRY).formatFor(Locale.US).convertRatesTo(TimeUnit.SECONDS)
      .convertDurationsTo(TimeUnit.MILLISECONDS).build(outputDir);
    reporter.start(1, TimeUnit.MINUTES);

    LOGGER.info("CSV File generated on:" + outputDir.getAbsolutePath());
  }

  public static void enableTopRankingLoggerReporter(int rank, int reportFrequencyInSeconds) {
    LOGGER.info("Init Top Ranking Logger Reporter with rank:{} and reportFrequencyInSeconds:{}", rank, reportFrequencyInSeconds);

    final Slf4jTopReporter reporter = Slf4jTopReporter.forRegistry(METRIC_REGISTRY).withRank(rank)
      .outputTo(LoggerFactory.getLogger("org.metrics")).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).build();
    reporter.start(reportFrequencyInSeconds, TimeUnit.SECONDS);
  }

  public static void reportTime(String name, long timeInMs) {
    Timer timer = METRIC_REGISTRY.timer(name);
    timer.update(timeInMs, TimeUnit.MILLISECONDS);
  }

  public static void reportSQLTime(String sql, long timeInMs) {
    String hash = getHashFromSQL(sql);

    SQLTimer timer = METRIC_REGISTRY.sqlTimer(hash, sql);

    timer.update(timeInMs, TimeUnit.MILLISECONDS);
    LOGGER.debug(escapeNewLine(sql) + "|timeInMs: " + timeInMs);
  }

  private static String getHashFromSQL(String sql) {
    byte[] bytesOfSQL;
    try {
      bytesOfSQL = sql.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOGGER.warn("given sql can't be encoded as UTF-8", e);
      bytesOfSQL = sql.getBytes();
    }
    byte[] hash = MESSAGE_DIGEST.digest(bytesOfSQL);
    return new String(hash);
  }

  static String escapeNewLine(String text) {
    return text.replace("\n", "").replace("\r", "").replaceAll("\\s+", " ");
  }
}
