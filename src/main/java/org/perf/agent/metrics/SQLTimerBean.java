package org.perf.agent.metrics;

public class SQLTimerBean {

  private String name;
  private SQLTimer sqlTimer;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public SQLTimer getSqlTimer() {
    return sqlTimer;
  }

  public void setSqlTimer(SQLTimer sqlTimer) {
    this.sqlTimer = sqlTimer;
  }

}
