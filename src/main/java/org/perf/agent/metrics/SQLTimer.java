package org.perf.agent.metrics;

import com.codahale.metrics.Timer;

public class SQLTimer extends Timer {

  private String sql;

  public SQLTimer(String sql) {
    this.sql = sql;
  }

  public String getSql() {
    return sql;
  }

}
