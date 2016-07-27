package org.perf.agent.db;

import java.util.ArrayList;
import java.util.List;

import javassist.CannotCompileException;
import javassist.CtClass;
import javassist.CtMethod;

import org.perf.agent.metrics.MetricReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JtdsInstrumentation implements DatabaseInstrumentation {

  private static final Logger LOGGER = LoggerFactory.getLogger(JtdsInstrumentation.class);

  private static final List<String> DB_STMT_CLASSES = new ArrayList<>();
  private static final List<String> DB_STMT_EXECUTE_METHODS = new ArrayList<>();

  static {
    DB_STMT_CLASSES.add("net.sourceforge.jtds.jdbc.JtdsPreparedStatement");
    DB_STMT_CLASSES.add("net.sourceforge.jtds.jdbc.JtdsCallableStatement");
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

    method.addLocalVariable("__metricStartTimeExecuteJtds", CtClass.longType);
    method.insertBefore("__metricStartTimeExecuteJtds = System.currentTimeMillis();");
    method.insertAfter(MetricReporter.PATH_TO_REPORTTIME_METHOD + "(this.sql, System.currentTimeMillis() - __metricStartTimeExecuteJtds);");
  }

}
