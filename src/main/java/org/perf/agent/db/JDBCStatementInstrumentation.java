package org.perf.agent.db;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

import org.perf.agent.metrics.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synthesis of all XXXStatementInstrumentation Classes (yet to be validated)
 */
public class JDBCStatementInstrumentation implements DatabaseInstrumentation {

  private static final Logger LOGGER = LoggerFactory.getLogger(JDBCStatementInstrumentation.class);

  private static List<String> DB_STMT_CLASSES = new ArrayList<String>();
  private static List<String> DB_STMT_EXECUTE_METHODS = new ArrayList<String>();

  static {
    DB_STMT_CLASSES.add("net.sourceforge.jtds.jdbc.JtdsStatement");

    DB_STMT_CLASSES.add("com.sybase.jdbc2.jdbc.SybStatement");
    DB_STMT_CLASSES.add("com.sybase.jdbc3.jdbc.SybStatement");

    DB_STMT_CLASSES.add("oracle.jdbc.driver.OracleStatement");

    DB_STMT_CLASSES.add("org.h2.jdbc.JdbcStatement");

    DB_STMT_CLASSES.add("org.postgresql.jdbc2.AbstractJdbc2Statement");
    DB_STMT_CLASSES.add("org.postgresql.jdbc3.AbstractJdbc3Statement");
    DB_STMT_CLASSES.add("org.postgresql.jdbc4.AbstractJdbc4Statement");

  }

  static {
    DB_STMT_EXECUTE_METHODS.add("execute");
    DB_STMT_EXECUTE_METHODS.add("executeQuery");
    DB_STMT_EXECUTE_METHODS.add("executeUpdate");
  }

  @Override
  public boolean isExecuteMethodsOnStatmentClasses(String className, CtMethod method) {
    return DB_STMT_CLASSES.contains(className) && DB_STMT_EXECUTE_METHODS.contains(method.getName());
  }

  @Override
  public void doInstrumentMethod(CtMethod method) throws CannotCompileException {
    LOGGER.debug("Instrumenting method " + method.getLongName());

    method.addLocalVariable("__metricStartTimeExecuteSybase", CtClass.longType);
    method.insertBefore("System.out.println( \"originalSql:\" + $args[0] );");
    method.insertBefore("__metricStartTimeExecuteSybase = System.currentTimeMillis();");
    method.insertAfter(MetricReporter.PATH_TO_REPORTTIME_METHOD + "($args[0], System.currentTimeMillis() - __metricStartTimeExecuteSybase);");
  }

}
