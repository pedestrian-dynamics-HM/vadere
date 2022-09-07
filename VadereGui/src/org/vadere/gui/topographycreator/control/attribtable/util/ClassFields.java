package org.vadere.gui.topographycreator.control.attribtable.util;

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
        do{
            classHierarchy.add(0,clazz);
            clazz = clazz.getSuperclass();
        }while(clazz.getSuperclass() != null);
        return classHierarchy;
    }

    public static Map<String, List<Field>> getFieldsGroupedBySemanticMeaning(java.util.List<Field> fieldList){
        return fieldList.stream()
                .collect(
                        Collectors.groupingBy(
                                field->field.getAnnotation(VadereAttribute.class).group(),
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
