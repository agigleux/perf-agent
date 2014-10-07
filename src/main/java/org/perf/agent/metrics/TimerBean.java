package org.perf.agent.metrics;

import com.codahale.metrics.Timer;

public class TimerBean {

  private String name;
  private Timer timer;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Timer getTimer() {
    return timer;
  }

  public void setTimer(Timer timer) {
    this.timer = timer;
  }

}
