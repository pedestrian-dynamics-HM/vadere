package org.vadere.annotation.factories.processor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
public @interface DataProcessorClass {
	String name();
	String label() default "";
	String description() default "";
}
