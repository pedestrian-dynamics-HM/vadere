package org.vadere.gui.topographycreator.control.attribtable.util;

import org.apache.commons.lang3.ArrayUtils;
import org.vadere.util.reflection.VadereAttribute;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

public final class ClassFields {
    public static Vector<Class> getSuperClassHierarchy(Class clazz) {
        Vector<Class> classHierarchy = new Vector<>();
        do {
            classHierarchy.add(0, clazz);
            clazz = clazz.getSuperclass();
        } while (clazz.getSuperclass() != null);
        return classHierarchy;
    }

    public static Field[] getSuperDeclaredFields(Class clazz) {
        return getSuperClassHierarchy(clazz).stream()
                .map(c -> checkVadereAttributes(c))
                .reduce((c1, c2) -> ArrayUtils.addAll(c1, c2)).get();
    }

    public static Field[] checkVadereAttributes(Class clazz) {
        if (clazz.getAnnotation(VadereAttribute.class) != null) {
            return Arrays.stream(clazz.getDeclaredFields()).filter(f -> {
                if (f.getAnnotation(VadereAttribute.class) != null) {
                    return !f.getAnnotation(VadereAttribute.class).exclude();
                }
                return true;
            }).collect(Collectors.toList()).toArray(Field[]::new);
        }
        return new Field[0];
    }

    public static Map<String, List<Field>> getFieldsGroupedBySemanticMeaning(java.util.List<Field> fieldList) {
        return fieldList.stream()
                .collect(
                        Collectors.groupingBy(
                                field -> field.getAnnotation(VadereAttribute.class).group(),
                                Collectors.toList()
                        ));
    }

    public static Map<Class<?>, java.util.List<Field>> getFieldsGroupedBySuperClass(Class clazz){
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(VadereAttribute.class)!= null)
                .collect(Collectors.groupingBy(
                        field -> field.getDeclaringClass(),
                        Collectors.toList()
                ));
    }
}
