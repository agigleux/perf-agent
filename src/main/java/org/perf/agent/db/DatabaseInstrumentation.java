package org.perf.agent.db;

import javassist.CannotCompileException;
import javassist.CtMethod;

public interface DatabaseInstrumentation {

  boolean isExecuteMethodsOnStatmentClasses(String className, CtMethod method);

  void doInstrumentMethod(CtMethod method) throws CannotCompileException;

}
