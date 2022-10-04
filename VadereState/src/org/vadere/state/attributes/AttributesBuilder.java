package org.vadere.state.attributes;



import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.ClassFields;

import java.lang.reflect.Field;

/**
 *
 * @author Benedikt Zoennchen
 */
public class AttributesBuilder<T extends Attributes> {

    private static Logger logger = Logger.getLogger(AttributesBuilder.class);
    private final T attributes;

    @SuppressWarnings("unchecked")
    public AttributesBuilder(T attributes) {
        this.attributes = (T) attributes.clone();
    }

    public void setField(String name, Object value) {
        Field field;
        try {
            field = ClassFields.getSuperDeclaredFields(attributes.getClass(),name).get();
            field.setAccessible(true);
            field.set(attributes, value);
        } catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
            logger.error(e);
        }

    }

    @SuppressWarnings("unchecked")
    public T build() {
        return (T) attributes.clone();
    }
}
