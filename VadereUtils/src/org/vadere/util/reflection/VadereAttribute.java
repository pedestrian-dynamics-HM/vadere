package org.vadere.util.reflection;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface VadereAttribute {
    String name() default "";

    String group() default "";

    boolean exclude() default false;

    String descr() default "";
}
