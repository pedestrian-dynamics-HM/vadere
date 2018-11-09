package org.vadere.annotation.factories.migrationassistant;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "JoltTransformationFactory",
		extendedClassName = "JoltTransformationBaseFactory",
		factoryImports = {"org.vadere.simulator.projects.migration.jolttranformation.JoltTransformationBaseFactory"},
		factoryPackage = "org.vadere.simulator.projects.migration.jolttranformation"

)
public @interface MigrationTransformation {
	String targetVersionLabel();
}
