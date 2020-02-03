package org.vadere.manager.traci.commandHandler.annotation;

import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.PolygonVar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Experimental annotation interface which removes long manually create switch statements by
 * creating a dynamic HashMap connecting commands(using variableIDs) to the corresponding handler
 * methods.
 *
 * Reflection is minimized to a single startup routine at object creation. At runtime only HashMap
 * access is performed.
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PolygonHandlers.class)
public @interface PolygonHandler {
	TraCICmd cmd();

	PolygonVar var();

	String name(); // name of client function.

	boolean ignoreElementId() default false;

	String dataTypeStr() default "";
}
