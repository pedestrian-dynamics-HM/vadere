package org.vadere.annotation.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation interface is used to annotate specific factory annotations. The members in this
 * annotation provides information about the new factory java source file.
 */
@Target(ElementType.ANNOTATION_TYPE)
public @interface FactoryType {
	/**
	 * @return Nam of the new Factory class. This is mandatory.
	 */
	String factoryClassName();

	/**
	 * @return Name of the super class if one exists.
	 */
	String extendedClassName() default "";

	/**
	 * @return Generic information for super class if one exists.
	 */
	String genericFactoryTypes() default "";

	/**
	 * @return List of imports needed for the new Factory class.
	 */
	String[] factoryImports() default {};

	/**
	 * @return Package name of the new Factory class. This is mandatory.
	 */
	String factoryPackage();
}
