package org.vadere.manager.traci.commandHandler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.vadere.manager.traci.TraCICmd;
import org.vadere.manager.traci.commandHandler.variables.ControlVar;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ControlHandlers.class)
public @interface ControlHandler {
  TraCICmd cmd();

  ControlVar var() default ControlVar.NONE;

  String name(); // name of client function.
}
