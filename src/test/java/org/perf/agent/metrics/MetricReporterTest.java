package org.perf.agent.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MetricReporterTest {

  @Test
  public void shouldRemoveWhitespaceFromString() {
    String sql = "SELECT NULL AS table_cat,       o.owner AS table_schem,       o.object_name AS table_name";
    String escapedString = MetricReporter.escapeNewLine(sql);

    assertThat(escapedString).isEqualTo("SELECT NULL AS table_cat, o.owner AS table_schem, o.object_name AS table_name");
  }

}
