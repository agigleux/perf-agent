package org.perf.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javassist.ByteArrayClassPath;
import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import org.perf.agent.db.DatabaseInstrumentation;
import org.perf.agent.db.H2Instrumentation;
import org.perf.agent.db.JDBCStatementInstrumentation;
import org.perf.agent.db.JtdsInstrumentation;
import org.perf.agent.db.OracleInstrumentation;
import org.perf.agent.db.PostgresqlInstrumentation;
import org.perf.agent.db.SybaseInstrumentation;
import org.perf.agent.metrics.Measured;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimedClassTransformer implements ClassFileTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(TimedClassTransformer.class);

  private static final List<String> PACKAGES_TO_IGNORE = new ArrayList<String>();
  private static final List<DatabaseInstrumentation> DB_INSTRUMENTATION_CLASSES = new ArrayList<DatabaseInstrumentation>();

  static {
    DB_INSTRUMENTATION_CLASSES.add(new JDBCStatementInstrumentation());

    DB_INSTRUMENTATION_CLASSES.add(new H2Instrumentation());
    DB_INSTRUMENTATION_CLASSES.add(new JtdsInstrumentation());
    DB_INSTRUMENTATION_CLASSES.add(new OracleInstrumentation());
    DB_INSTRUMENTATION_CLASSES.add(new PostgresqlInstrumentation());
    DB_INSTRUMENTATION_CLASSES.add(new SybaseInstrumentation());
  }

  static {
    PACKAGES_TO_IGNORE.add("java");
    PACKAGES_TO_IGNORE.add("sun");
    PACKAGES_TO_IGNORE.add("com.sun");
    PACKAGES_TO_IGNORE.add("javax.management");

    PACKAGES_TO_IGNORE.add("shaded");
    PACKAGES_TO_IGNORE.add("org.perf.agent");
    PACKAGES_TO_IGNORE.add("com.codahale");
    PACKAGES_TO_IGNORE.add("org.jboss");
    PACKAGES_TO_IGNORE.add("org.codehaus");
    PACKAGES_TO_IGNORE.add("groovy");
    PACKAGES_TO_IGNORE.add("play");
    PACKAGES_TO_IGNORE.add("org.eclipse");

    PACKAGES_TO_IGNORE.add("oracle");
    PACKAGES_TO_IGNORE.add("org.h2");
    PACKAGES_TO_IGNORE.add("net.sourceforge.jtds");
    PACKAGES_TO_IGNORE.add("org.postgresql");
    PACKAGES_TO_IGNORE.add("com.sybase");
  }

  private ClassPool classPool;
  private Map<String, String> properties;

  public String getCustomPackagePrefix() {
    String r = properties.get(MethodTimerAgent.PARAM_CUSTOM_PACKAGE_PREFIX);
    if (r == null) {
      r = "";
    }
    return r;
  }

  private boolean getMethodsMeasurement() {
    String methodsMeasurement = properties.get(MethodTimerAgent.PARAM_METHODS_MEASUREMENT);
    return Boolean.valueOf(methodsMeasurement);
  }

  public TimedClassTransformer(Map<String, String> properties) {
    this.properties = properties;

    classPool = new ClassPool();
    classPool.appendSystemPath();
    try {
      classPool.appendPathList(System.getProperty("java.class.path"));

      // make sure that MetricReporter is loaded
      classPool.get("org.perf.agent.metrics.MetricReporter").getClass();
      classPool.appendClassPath(new LoaderClassPath(ClassLoader.getSystemClassLoader()));
    } catch (Exception e) {
      throw new RuntimeException(e); // NOSONAR
    }
  }

  public byte[] transform(ClassLoader loader, String fullyQualifiedClassName, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
    byte[] classBytes) throws IllegalClassFormatException {
    String className = fullyQualifiedClassName.replace("/", ".");

    classPool.appendClassPath(new ByteArrayClassPath(className, classBytes));

    boolean elligible4MethodsMeasurement = false;

    try {
      CtClass ctClass = classPool.get(className);
      if (ctClass.isFrozen()) {
        LOGGER.debug("Skip class {}: is frozen", className);
        return null;
      }

      if (ctClass.isPrimitive() || ctClass.isArray() || ctClass.isAnnotation() || ctClass.isEnum() || ctClass.isInterface()) {
        LOGGER.debug("Skip class {}: not a class", className);
        return null;
      }

      if (getMethodsMeasurement()) {
        if (className.startsWith(getCustomPackagePrefix()) && !isPackageToDiscard(className)) {
          elligible4MethodsMeasurement = true;
        }
      }

      boolean isClassModified = false;
      for (CtMethod method : ctClass.getDeclaredMethods()) {

        // if method is annotated then add the code to measure the time
        if (method.hasAnnotation(Measured.class) || elligible4MethodsMeasurement) {

          if (method.getMethodInfo().getCodeAttribute() == null) {
            LOGGER.debug("Skip method " + method.getLongName());
            continue;
          }

          instrumentMethodWithExecTime(ctClass, method);
          isClassModified = true;
        }
        for (DatabaseInstrumentation clazz : DB_INSTRUMENTATION_CLASSES) {
          if (clazz.isExecuteMethodsOnStatmentClasses(className, method)) {
            clazz.doInstrumentMethod(method);
            isClassModified = true;
          }
        }

      }
      if (!isClassModified) {
        return null;
      }
      return ctClass.toBytecode();
    } catch (Exception e) {
      LOGGER.debug("Skip class {}: " + className, e);
      return null;
    }
  }

  private boolean isPackageToDiscard(String className) {
    for (String packageName : PACKAGES_TO_IGNORE) {
      if (className.startsWith(packageName)) {
        return true;
      }
    }
    return false;
  }

  private void instrumentMethodWithExecTime(CtClass ctClass, CtMethod method) throws CannotCompileException {
    LOGGER.debug("Instrumenting method " + method.getLongName());
    method.addLocalVariable("__metricStartTimeMeasured", CtClass.longType);
    method.insertBefore("__metricStartTimeMeasured = System.currentTimeMillis();");
    String metricName = ctClass.getName() + "." + method.getName();
    method.insertAfter("org.perf.agent.metrics.MetricReporter.reportTime(\"" + metricName
      + "\", System.currentTimeMillis() - __metricStartTimeMeasured);");
  }

}
