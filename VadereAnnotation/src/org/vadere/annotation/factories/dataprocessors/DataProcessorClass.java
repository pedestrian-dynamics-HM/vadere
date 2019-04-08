package org.vadere.annotation.factories.dataprocessors;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation interface defines the DataProcessorFactory. The main logic of the factory
 * resides in the  ProcessorBaseFactory (util-Module). The generated java source file only
 * provides the tedious getter and Map creations.
 */
@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "DataProcessorFactory",
		extendedClassName = "ProcessorBaseFactory",
		genericFactoryTypes = "DataProcessor<?, ?>",
		factoryImports = {
				"org.vadere.simulator.projects.dataprocessing.processor.DataProcessor",
				"org.vadere.util.factory.processors.ProcessorBaseFactory"
		},
		factoryPackage = "org.vadere.simulator.projects.dataprocessing.processor"
)
public @interface DataProcessorClass {
	String label() default "";

	String description() default "";

	String[] processorFlags() default {};
}
