package org.vadere.util.test;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation can be used to mark statistical JUnit test methods that use a
 * random generator and can fail with a certain probability. This is only for
 * documentation.
 *
 */
@Retention(SOURCE)
@Target(METHOD)
public @interface StatisticalTestCase {

	double probabilityOfFailure() default Double.NaN;

}
