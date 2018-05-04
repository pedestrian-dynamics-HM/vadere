package org.vadere.annotation.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@FactoryType(
		factoryType = "DataProcessor<?, ?>",
		factoryImports = {
				"org.vadere.simulator.projects.dataprocessing.processor.DataProcessor"
		},
		factoryName = "DataProcessorFactoryXX",
		factoryPackage = "org.vadere.simulator.projects.dataprocessing.processor"
)
public @interface DataProcessorClass {
	String label() default "";
	String description() default "";
}
