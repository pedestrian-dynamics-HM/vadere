package org.vadere.util.reflection;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target(ElementType.FIELD)
public @interface VadereAttribute {
    String name() default "";
    String group() default "";
    String descr() default "";
}
