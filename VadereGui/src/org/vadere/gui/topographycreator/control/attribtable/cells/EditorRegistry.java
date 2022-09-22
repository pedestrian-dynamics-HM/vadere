package org.vadere.gui.topographycreator.control.attribtable.cells;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.*;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.state.util.JacksonObjectMapper;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.geometry.shapes.VShape;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;

/**
 * @author Ludwig Jaeck
 * EditorRegistry is modeled after the singleton model to have a central manager for all AttributeEditor types used
 * while constructing the gui. This is needed since there is no monolithic gui builder used which would build the whole
 * attribute table widget hierarchy but in every level of the attribute tree model, UI delegates are able to add sub elements
 * themselve.
 */
public class EditorRegistry {
    /**
     * The singleton instance of the registry
     */
    private static EditorRegistry registry;
    /**
     * editorConstructors holds all constructors of the registered AttributeEditors
     */
    private final HashMap<String, Constructor<? extends AttributeEditor>> editorConstructors;

    private EditorRegistry() {
        this.editorConstructors = new HashMap<>();
        addTypeEditor(String.class, TextEditCellEditor.class);
        addTypeEditor(Integer.class, SpinnerCellEditor.class);
        addTypeEditor(Double.class, DoubleSpinnerCellEditor.class);
        addTypeEditor(Boolean.class, CheckBoxCellEditor.class);
        addTypeEditor(VPoint.class, VPointCellEditor.class);
        addTypeEditor(VShape.class, VShapeCellEditor.class);
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
                constructor = editorClass.getDeclaredConstructor(AttributeTreeModel.TreeNode.class, JPanel.class,Object.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(typeClass.getName(), constructor);
        }
    }

    public AttributeEditor create(@NotNull Class type, @NotNull AttributeTreeModel.TreeNode model, @NotNull JPanel contentPanel, Object initialValue) {
        Constructor constructor;
        AttributeEditor component;
        try {
            if (!contains(type)) {
                if (type.isEnum())
                    constructor = ComboBoxCellEditor.class.getDeclaredConstructor(AttributeTreeModel.TreeNode.class, JPanel.class, Object.class);
                else if (type.isAssignableFrom(List.class)) {
                    constructor = ListCellEditor.class.getDeclaredConstructor(AttributeTreeModel.TreeNode.class, JPanel.class, Object.class);
                } else if (Modifier.isAbstract(type.getModifiers()))
                    constructor = AbstractTypeCellEditor.class.getDeclaredConstructor(AttributeTreeModel.TreeNode.class, JPanel.class, Object.class);
                else
                    constructor = ChildObjectCellEditor.class.getDeclaredConstructor(AttributeTreeModel.TreeNode.class, JPanel.class,Object.class);
            } else {
                constructor = editorConstructors.get(type.getName());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(type.getName() + ": no editor registered for such type");
        }
        try {
            component = (AttributeEditor) constructor.newInstance(model, contentPanel,initialValue);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e + " could not create new instance of type " + constructor);
        }

        return component;
    }

    public boolean contains(Class type) {
        return editorConstructors.containsKey(type.getName());
    }
}
