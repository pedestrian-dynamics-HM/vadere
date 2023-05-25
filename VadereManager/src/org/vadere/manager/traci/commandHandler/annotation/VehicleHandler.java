package org.vadere.manager.traci.commandHandler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.VehicleVar;

/**
 * Experimental annotation interface which removes long manually create switch statements by
 * creating a dynamic HashMap connecting commands(using variableIDs) to the corresponding handler
 * methods.
 *
 * <p>Reflection is minimized to a single startup routine at object creation. At runtime only
 * HashMap access is performed.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(VehicleHandlers.class)
public @interface VehicleHandler {
  TraCICmd cmd();

  VehicleVar var();

  String name(); // name of client function.
}
