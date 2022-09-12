package org.vadere.gui.topographycreator.control.attribtable.cells.editors;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.*;
import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public class EditorRegistry {
    private static EditorRegistry registry;
    private final HashMap<String, Constructor<? extends AttributeEditor>> editorConstructors;

    private EditorRegistry() {
        this.editorConstructors = new HashMap<>();
        addTypeEditor(String.class, TextEditCellEditor.class);
        addTypeEditor(Integer.class, SpinnerCellEditor.class);
        addTypeEditor(Double.class, DoubleSpinnerCellEditor.class);
        addTypeEditor(Boolean.class, CheckBoxCellEditor.class);
        //addTypeEditor(ArrayList.class, ListCellEditor.class);
        //addTypeEditor(Object.class, EmptyEditor.class);
    }

    public static EditorRegistry getInstance() {
        if (registry == null) {
            registry = new EditorRegistry();
        }
        return registry;
    }

    public void addTypeEditor(Class typeClass, Class<? extends AttributeEditor> editorClass) {
        if (!this.editorConstructors.containsKey(typeClass)) {
            Constructor<? extends AttributeEditor> constructor = null;
            try {
                constructor = editorClass.getDeclaredConstructor(AbstractModel.class, String.class, JPanel.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(typeClass.getName(), constructor);
        }
    }

    public AttributeEditor create(Class type, AbstractModel model, String id, JPanel contentPanel) {
        Constructor constructor;
        AttributeEditor component;
        try {
            if (!contains(type)) {
                if (type.isEnum())
                    constructor = ComboBoxCellEditor.class.getDeclaredConstructor(AbstractModel.class, String.class, JPanel.class);
                    //addTypeEditor(type, ComboBoxCellEditor.class);
                else if (Modifier.isAbstract(type.getModifiers()))
                    constructor = AbstractTypeCellEditor.class.getDeclaredConstructor(AbstractModel.class, String.class, JPanel.class);
                else
                    constructor = ChildObjectCellEditor.class.getDeclaredConstructor(AbstractModel.class, String.class, JPanel.class);
            } else {
                constructor = editorConstructors.get(type.getName());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(type.getName() + ": no editor registered for such type");
        }
        try {
            component = (AttributeEditor) constructor.newInstance(model, id, contentPanel);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        return component;
    }

    public boolean contains(Class type) {
        return editorConstructors.containsKey(type.getName());
    }
}
