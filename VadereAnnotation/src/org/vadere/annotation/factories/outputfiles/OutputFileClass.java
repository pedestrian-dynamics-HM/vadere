package org.vadere.annotation.factories.outputfiles;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation interface defines the OutputFileFactory. The main logic of the factory
 * resides in the  OutputFileBaseFactory (util-Module). The generated java source file only
 * provides the tedious getter and Map creations. Also the mapping between OutputFile and DataKey
 * type is created.
 */
@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "OutputFileFactory",
		extendedClassName = "OutputFileBaseFactory",
		genericFactoryTypes = "OutputFile<? extends DataKey<?>>",
		factoryImports = {
				"org.vadere.simulator.projects.dataprocessing.outputfile.OutputFile",
				"org.vadere.simulator.projects.dataprocessing.datakey.DataKey",
				"org.vadere.util.factory.outputfiles.OutputFileBaseFactory"
		},
		factoryPackage = "org.vadere.simulator.projects.dataprocessing.outputfile"

)
public @interface OutputFileClass {
	String label() default "";

	String description() default "";

	Class dataKeyMapping();
}
