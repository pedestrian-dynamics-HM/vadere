package org.vadere.annotation.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@FactoryType(
		factoryType = "OutputFile<? extends DataKey<?>>",
		factoryImports = {
				"org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile",
				"org.vadere.simulator.projects.dataprocessing.datakey.DataKey"
		},
		factoryName = "OutputFileFactoryXX",
		factoryPackage = "org.vadere.simulator.projects.dataprocessing.outputfile"

)
public @interface OutputFileClass {
	String label() default "";
	String description() default "";
}
