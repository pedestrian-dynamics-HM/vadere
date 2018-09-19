package org.vadere.annotation.factories.attributes;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation interface defines the ModelAttributeFactory. The main logic of the factory
 * resides in the  AttributeBaseFactory (util-Module). The generated java source file only
 * provides the tedious getter and Map creations.
 */
@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "ModelAttributeFactory",
		extendedClassName = "AttributeBaseFactory",
		genericFactoryTypes = "Attributes",
		factoryImports = {
				"org.vadere.state.attributes.Attributes",
				"org.vadere.util.factory.attributes.AttributeBaseFactory"
		},
		factoryPackage = "org.vadere.state.attributes"

)
public @interface ModelAttributeClass {
}
