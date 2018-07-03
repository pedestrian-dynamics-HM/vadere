package org.vadere.annotation.factories.models;

import org.vadere.annotation.factories.FactoryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation interface define a new ModelHelper class. This class is not a Factory but
 * the generation of the source file is similar enough to use it.
 */
@Target(ElementType.TYPE)
@FactoryType(
		factoryClassName = "ModelHelper",
		extendedClassName = "org.vadere.util.factory.model.BaseModelHelper",
		factoryPackage = "org.vadere.simulator.models"
)
public @interface ModelClass {
	/**
	 * @return Used to mark MainModels. All other models/subModels do not have to explicitly state this.
	 */
	boolean isMainModel() default false;
}
