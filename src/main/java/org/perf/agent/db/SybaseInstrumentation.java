package org.perf.agent.db;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

import org.perf.agent.metrics.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SybaseInstrumentation implements DatabaseInstrumentation {

  private static final Logger LOGGER = LoggerFactory.getLogger(SybaseInstrumentation.class);

  private static List<String> DB_STMT_CLASSES = new ArrayList<String>();
  private static List<String> DB_STMT_EXECUTE_METHODS = new ArrayList<String>();

  static {
    DB_STMT_CLASSES.add("com.sybase.jdbc2.jdbc.SybPreparedStatement");
    DB_STMT_CLASSES.add("com.sybase.jdbc2.jdbc.SybCallableStatement");

    DB_STMT_CLASSES.add("com.sybase.jdbc3.jdbc.SybPreparedStatement");
    DB_STMT_CLASSES.add("com.sybase.jdbc3.jdbc.SybCallableStatement");
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
    method.insertBefore("__metricStartTimeExecuteSybase = System.currentTimeMillis();");
    method.insertAfter(MetricReporter.PATH_TO_REPORTTIME_METHOD + "(this._query, System.currentTimeMillis() - __metricStartTimeExecuteSybase);");
  }

}
