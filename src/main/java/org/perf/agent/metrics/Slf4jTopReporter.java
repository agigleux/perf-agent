package org.perf.agent.metrics;

import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Counter;
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * A reporter class for logging metrics values to a SLF4J {@link Logger} periodically, similar to {@link ConsoleReporter} or {@link CsvReporter}, but
 * using the SLF4J framework instead. It also
 * supports specifying a {@link Marker} instance that can be used by custom appenders and filters
 * for the bound logging toolkit to further process metrics reports.
 */
public class Slf4jTopReporter extends ScheduledReporter {

  private final Logger logger; // NOSONAR
  private final Marker marker;
  private final int rank;

  /**
   * Returns a new {@link Builder} for {@link Slf4jReporter}.
   * 
   * @param registry the registry to report
   * @return a {@link Builder} instance for a {@link Slf4jReporter}
   */
  public static Builder forRegistry(MetricRegistry registry) {
    return new Builder(registry);
  }

  /**
   * A builder for {@link CsvReporter} instances. Defaults to logging to {@code metrics}, not
   * using a marker, converting rates to events/second, converting durations to milliseconds, and
   * not filtering metrics.
   */
  public static class Builder {
    private final MetricRegistry registry;
    private Logger logger; // NOSONAR
    private Marker marker;
    private TimeUnit rateUnit;
    private TimeUnit durationUnit;
    private MetricFilter filter;
    private int rank;

    private Builder(MetricRegistry registry) {
      this.registry = registry;
      this.logger = LoggerFactory.getLogger("metrics");
      this.marker = null;
      this.rateUnit = TimeUnit.SECONDS;
      this.durationUnit = TimeUnit.MILLISECONDS;
      this.filter = MetricFilter.ALL;
    }

    /**
     * Log metrics to the given logger.
     * 
     * @param logger an SLF4J {@link Logger}
     * @return {@code this}
     */
    public Builder outputTo(Logger logger) { // NOSONAR
      this.logger = logger;
      return this;
    }

    /**
     * Mark all logged metrics with the given marker.
     * 
     * @param marker an SLF4J {@link Marker}
     * @return {@code this}
     */
    public Builder markWith(Marker marker) {
      this.marker = marker;
      return this;
    }

    /**
     * Convert rates to the given time unit.
     * 
     * @param rateUnit a unit of time
     * @return {@code this}
     */
    public Builder convertRatesTo(TimeUnit rateUnit) {
      this.rateUnit = rateUnit;
      return this;
    }

    /**
     * Convert durations to the given time unit.
     * 
     * @param durationUnit a unit of time
     * @return {@code this}
     */
    public Builder convertDurationsTo(TimeUnit durationUnit) {
      this.durationUnit = durationUnit;
      return this;
    }

    /**
     * Only report metrics which match the given filter.
     * 
     * @param filter a {@link MetricFilter}
     * @return {@code this}
     */
    public Builder filter(MetricFilter filter) {
      this.filter = filter;
      return this;
    }

    public Builder withRank(int rank) {
      this.rank = rank;
      return this;
    }

    /**
     * Builds a {@link Slf4jTopReporter} with the given properties.
     * 
     * @return a {@link Slf4jTopReporter}
     */
    public Slf4jTopReporter build() {
      return new Slf4jTopReporter(registry, logger, marker, rateUnit, durationUnit, filter, rank);
    }
  }

  private Slf4jTopReporter(MetricRegistry registry, Logger logger, Marker marker, TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter,
    int rank) {
    super(registry, "logger-reporter", filter, rateUnit, durationUnit);
    this.logger = logger;
    this.marker = marker;
    this.rank = rank;
  }

  public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters, SortedMap<String, Histogram> histograms,
    SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {

    SQLTimerBean[] sqlTOPMaxRanking = new SQLTimerBean[rank];
    SQLTimerBean[] sqlTOPCostlyRanking = new SQLTimerBean[rank];
    SQLTimerBean[] sqlTOPCalledRanking = new SQLTimerBean[rank];
    TimerBean[] methodsRanking = new TimerBean[rank];

    for (Entry<String, Timer> entry : timers.entrySet()) {
      if (entry.getValue() instanceof SQLTimer) {
        findTOPSQLMaxRanking(sqlTOPMaxRanking, entry);
        findTOPCostlySQLRanking(sqlTOPCostlyRanking, entry);
        findTOPCalledSQLRanking(sqlTOPCalledRanking, entry);
      } else {
        if (entry.getValue() instanceof Timer) {
          findMethodsRanking(methodsRanking, entry);
        }
      }
    }

    reportTOPSQLMaxRanking(sqlTOPMaxRanking);
    reportTOPCostlySQLRanking(sqlTOPCostlyRanking);
    reportTOPCalledSQLRanking(sqlTOPCalledRanking);

    reportMethodsRanking(methodsRanking);
  }

  private void findTOPSQLMaxRanking(SQLTimerBean[] ranking, Entry<String, Timer> entry) {
    final String name = entry.getKey();
    final SQLTimer sqlTimer = (SQLTimer) entry.getValue();
    final Snapshot snapshot = sqlTimer.getSnapshot();

    for (int i = 0; i < rank; i++) {
      SQLTimerBean bean = ranking[i];
      if (bean != null) {
        if (snapshot.getMax() >= bean.getSqlTimer().getSnapshot().getMax()) {
          bean.setName(name);
          bean.setSqlTimer(sqlTimer);
          ranking[i] = bean;
          break;
        }
      } else {
        bean = new SQLTimerBean();
        bean.setName(name);
        bean.setSqlTimer(sqlTimer);
        ranking[i] = bean;
        break;
      }
    }
  }

  private void findTOPCostlySQLRanking(SQLTimerBean[] ranking, Entry<String, Timer> entry) {

    final String name = entry.getKey();
    final SQLTimer sqlTimer = (SQLTimer) entry.getValue();

    for (int i = 0; i < rank; i++) {
      SQLTimerBean bean = ranking[i];
      if (bean != null) {
        double thisTimeCost = sqlTimer.getCount() * sqlTimer.getSnapshot().getMean();
        double beanTimeCost = bean.getSqlTimer().getCount() * bean.getSqlTimer().getSnapshot().getMean();

        if (thisTimeCost >= beanTimeCost) {
          bean.setName(name);
          bean.setSqlTimer(sqlTimer);
          ranking[i] = bean;
          break;
        }
      } else {
        bean = new SQLTimerBean();
        bean.setName(name);
        bean.setSqlTimer(sqlTimer);
        ranking[i] = bean;
        break;
      }
    }
  }

  private void findTOPCalledSQLRanking(SQLTimerBean[] ranking, Entry<String, Timer> entry) {
    final String name = entry.getKey();
    final SQLTimer sqlTimer = (SQLTimer) entry.getValue();

    for (int i = 0; i < rank; i++) {
      SQLTimerBean bean = ranking[i];
      if (bean != null) {
        if (sqlTimer.getCount() >= bean.getSqlTimer().getCount()) {
          bean.setName(name);
          bean.setSqlTimer(sqlTimer);
          ranking[i] = bean;
          break;
        }
      } else {
        bean = new SQLTimerBean();
        bean.setName(name);
        bean.setSqlTimer(sqlTimer);
        ranking[i] = bean;
        break;
      }
    }
  }

  private TimerBean[] findMethodsRanking(TimerBean[] ranking, Entry<String, Timer> entry) {
    final String name = entry.getKey();
    final Timer timer = (Timer) entry.getValue();
    final Snapshot snapshot = timer.getSnapshot();

    for (int i = 0; i < rank; i++) {
      TimerBean bean = ranking[i];
      if (bean != null) {
        if (snapshot.getMax() >= bean.getTimer().getSnapshot().getMax()) {
          bean.setName(name);
          bean.setTimer(timer);
          ranking[i] = bean;
          break;
        }
      } else {
        bean = new TimerBean();
        bean.setName(name);
        bean.setTimer(timer);
        ranking[i] = bean;
        break;
      }
    }
    return ranking;
  }

  private void reportTOPSQLMaxRanking(SQLTimerBean[] sqlRanking) {
    logger.info(marker, "TOP {} SQL Queries (Max Exec Time)", rank);
    reportSQLRanking(sqlRanking);
  }

  private void reportTOPCostlySQLRanking(SQLTimerBean[] sqlRanking) {
    logger.info(marker, "TOP {} Costly SQL Queries (# Count Exec x Mean)", rank);
    for (int i = 0; i < rank; i++) {
      SQLTimerBean bean = sqlRanking[i];

      if (bean != null) {
        final SQLTimer sqlTimer = bean.getSqlTimer();

        logger.info(marker, "type=SQLTIMER, sql={}, count={}, totalCost={}, mean={}, duration_unit={}",
          MetricReporter.escapeNewLine(sqlTimer.getSql()), sqlTimer.getCount(), sqlTimer.getCount()
            * convertDuration(sqlTimer.getSnapshot().getMean()), convertDuration(sqlTimer.getSnapshot().getMean()), getDurationUnit());
      }
    }
  }

  private void reportTOPCalledSQLRanking(SQLTimerBean[] sqlRanking) {
    logger.info(marker, "TOP {} Called SQL Queries", rank);
    reportSQLRanking(sqlRanking);
  }

  private void reportSQLRanking(SQLTimerBean[] sqlRanking) {
    for (int i = 0; i < rank; i++) {
      SQLTimerBean bean = sqlRanking[i];

      if (bean != null) {
        final SQLTimer sqlTimer = bean.getSqlTimer();
        final Snapshot snapshot = sqlTimer.getSnapshot();

        logger.info(marker, "type=SQLTIMER, sql={}, count={}, min={}, max={}, mean={}, stddev={}, median={}, "
          + "p75={}, p95={}, p98={}, p99={}, p999={}, mean_rate={}, m1={}, m5={}, " + "m15={}, rate_unit={}, duration_unit={}",
          MetricReporter.escapeNewLine(sqlTimer.getSql()), sqlTimer.getCount(), convertDuration(snapshot.getMin()),
          convertDuration(snapshot.getMax()), convertDuration(snapshot.getMean()), convertDuration(snapshot.getStdDev()),
          convertDuration(snapshot.getMedian()), convertDuration(snapshot.get75thPercentile()),
          convertDuration(snapshot.get95thPercentile()), convertDuration(snapshot.get98thPercentile()),
          convertDuration(snapshot.get99thPercentile()), convertDuration(snapshot.get999thPercentile()),
          convertRate(sqlTimer.getMeanRate()), convertRate(sqlTimer.getOneMinuteRate()), convertRate(sqlTimer.getFiveMinuteRate()),
          convertRate(sqlTimer.getFifteenMinuteRate()), getRateUnit(), getDurationUnit());
      }
    }
  }

  private void reportMethodsRanking(TimerBean[] methodsRanking) {
    logger.info(marker, "TOP {} Methods (Max Exec Time)", rank);
    for (int i = 0; i < rank; i++) {
      TimerBean bean = methodsRanking[i];

      if (bean != null) {
        final Timer timer = bean.getTimer();
        final Snapshot snapshot = timer.getSnapshot();

        logger.info(marker, "type=TIMER, methodName={}, count={}, min={}, max={}, mean={}, stddev={}, median={}, "
          + "p75={}, p95={}, p98={}, p99={}, p999={}, mean_rate={}, m1={}, m5={}, " + "m15={}, rate_unit={}, duration_unit={}",
          bean.getName(), timer.getCount(), convertDuration(snapshot.getMin()), convertDuration(snapshot.getMax()),
          convertDuration(snapshot.getMean()), convertDuration(snapshot.getStdDev()), convertDuration(snapshot.getMedian()),
          convertDuration(snapshot.get75thPercentile()), convertDuration(snapshot.get95thPercentile()),
          convertDuration(snapshot.get98thPercentile()), convertDuration(snapshot.get99thPercentile()),
          convertDuration(snapshot.get999thPercentile()), convertRate(timer.getMeanRate()), convertRate(timer.getOneMinuteRate()),
          convertRate(timer.getFiveMinuteRate()), convertRate(timer.getFifteenMinuteRate()), getRateUnit(), getDurationUnit());
      }
    }
  }

  @Override
  protected String getRateUnit() {
    return "events/" + super.getRateUnit();
  }
}
