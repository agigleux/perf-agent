package org.perf.agent.db;

import javassist.CannotCompileException;
import javassist.CtMethod;

public interface DatabaseInstrumentation {

  public boolean isExecuteMethodsOnStatmentClasses(String className, CtMethod method);

  public void doInstrumentMethod(CtMethod method) throws CannotCompileException;

}
