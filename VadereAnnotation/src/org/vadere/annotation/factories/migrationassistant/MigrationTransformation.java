package org.vadere.annotation.factories.migrationassistant;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "JsonTransformationFactory",
		extendedClassName = "JsonTransformationBaseFactory",
		factoryImports = {"org.vadere.simulator.projects.migration.jsontranformation.JsonTransformationBaseFactory"},
		factoryPackage = "org.vadere.simulator.projects.migration.jsontranformation"

)
public @interface MigrationTransformation {
	String targetVersionLabel();
}
