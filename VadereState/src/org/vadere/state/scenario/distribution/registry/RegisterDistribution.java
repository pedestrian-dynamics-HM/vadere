package org.vadere.state.scenario.distribution.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Aleksandar Ivanov(ivanov0@hm.edu)
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterDistribution {

	String name();

	Class<?> parameter();

}
