package org.vadere.simulator.projects.dataprocessing.datakey;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OutputFileMap {
  public Class<? extends OutputFile> outputFileClass();
}
