package org.perf.agent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.dbutils.DbUtils;
import org.perf.agent.metrics.Measured;

public class AgentIntegrationTestApp {

  private Random random = new Random();

  public static void main(String[] args) {
    System.out.println("Start AgentIntegrationTestApp");
    AgentIntegrationTestApp a = new AgentIntegrationTestApp();

    for (int i = 0; i < 10; i++) {
      // a.bizMethod1();
    }
    a.bizMethod2("String Parameter");

    try {
      a.simulateOracleActivities();
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }

  public void bizMethod1() {
    System.out.println("In bizMethod1");
    try {
      Thread.sleep(random.nextInt(1200));
    } catch (InterruptedException e) {
    }
  }

  @Measured
  private void bizMethod2(String param) {
    System.out.println("bizMethod2:" + param);
  }

  private void simulateOracleActivities() throws ClassNotFoundException, SQLException {
    final String dbDriver = "oracle.jdbc.OracleDriver";

    final String dbPort = "11521";
    final String dbSID = "ORCL";
    final String dbHost = "localhost";
    final String dbURL = "jdbc:oracle:thin:@" + dbHost + ":" + dbPort + "/" + dbSID;
    final String dbUser = "sonar";
    final String dbPass = "sonar";

    ResultSet rs = null;
    PreparedStatement ps = null;
    Connection conn = null;

    try {

      Class.forName(dbDriver);
      conn = DriverManager.getConnection(dbURL, dbUser, dbPass);

      List<TableItem> tableItems = new ArrayList<TableItem>();
      // ps =
      // conn.prepareStatement("SELECT TABLE_NAME, OWNER FROM ALL_TABLES WHERE OWNER NOT IN ('SYS', 'SYSTEM', 'OUTLN') AND TABLE_NAME LIKE 'A%' ORDER BY TABLE_NAME ASC");
      ps =
        conn.prepareStatement("SELECT TABLE_NAME, OWNER FROM ALL_TABLES WHERE OWNER NOT IN ('SYS', 'SYSTEM', 'OUTLN') ORDER BY TABLE_NAME ASC");
      // ps = conn.prepareStatement("SELECT TABLE_NAME, OWNER FROM ALL_TABLES WHERE OWNER  IN ('SONAR') ORDER BY TABLE_NAME ASC");

      rs = ps.executeQuery();
      while (rs.next()) {
        String tableName = rs.getString(1);
        String owner = rs.getString(2);

        TableItem ti = new TableItem();
        ti.tableName = tableName;
        ti.owner = owner;

        tableItems.add(ti);
      }

      for (int i = 0; i < 10; i++) {
        System.out.println("Run #" + i);

        for (TableItem ti : tableItems) {
          System.out.println("Rows of:" + ti.tableName);
          countRowsInTable(conn, ti);
        }
      }

    } finally {
      DbUtils.closeQuietly(rs);
      DbUtils.closeQuietly(ps);
      DbUtils.closeQuietly(conn);
    }

  }

  private void countRowsInTable(Connection conn, TableItem ti) {
    ResultSet rsTable = null;
    PreparedStatement pStatement = null;
    try {
      pStatement = conn.prepareStatement("SELECT COUNT(1) FROM " + ti.owner + "." + ti.tableName);
      rsTable = pStatement.executeQuery();

      while (rsTable.next()) {
        Long tableSize = rsTable.getLong(1);
        System.out.println(ti.tableName + ": " + tableSize);
      }
    } catch (java.sql.SQLException e) {
      System.err.println(e.getMessage());
    } finally {
      DbUtils.closeQuietly(rsTable);
      DbUtils.closeQuietly(pStatement);
    }
  }
}
