package org.vadere.gui.topographycreator.control.attribtable.tree;

import org.vadere.gui.topographycreator.control.attribtable.util.ClassFields;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class ObjectNode extends AttributeTree.TreeNode {

    private final HashMap<String, Field> fields;

    public ObjectNode(AttributeTree.TreeNode parent, String fieldName, Class clazz) {
        super(parent, fieldName, clazz);
        fields = (HashMap<String, Field>) Arrays.stream(ClassFields.getSuperDeclaredFields(clazz)).collect(Collectors.toMap(field -> field.getName(), field -> field));
    }

    @Override
    public void set(String field, AttributeTree.TreeNode object) {
        object.setParent(this);
        getChildren().put(field, object);
    }

    @Override
    public Object get(String field) {
        return getChildren().get(field);
    }

    @Override
    public void setParentField(String fieldName, Object object) throws NoSuchFieldException, IllegalAccessException {
        var field = fields.get(fieldName);
        field.setAccessible(true);
        field.set(getReference(), object);
        field.setAccessible(false);
        getParent().setParentField(getFieldName(), getReference());
    }

}
