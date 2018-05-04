package org.vadere.annotation.factories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.ANNOTATION_TYPE)
public @interface FactoryType {
	String factoryType();
	String[] factoryImports();
	String factoryName();
	String factoryPackage();
}
