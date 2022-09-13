package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTableView;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.state.attributes.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint

public class AbstractTypeCellEditor extends AttributeEditor implements Revalidatable {
    public static final String STRING_NULL = "[null]";
    private JComboBox<Object> comboBox;
    private AbstractTypeCellEditor self;
    private RunnableRegistry runnableRegistry;
    private Map<String, Constructor<?>> classConstructorRegistry;
    private Map<String, Class<?>> classNameRegistry;
    private GridBagConstraints gbc;
    private String selected;

    private AttributeTableView view;
    private Object instanceOfSelected;

    public AbstractTypeCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }


    @Override
    protected void initialize() {
        view = new AttributeTableView(this);
        initializeGridBagConstraint();
        initializeRunnableRegistry();
        initializeComboBox();
        initializeSelfReference();
        this.contentPanel.setVisible(false);
        this.contentPanel.add(view, gbc);
    }

    @Override
    public void onModelChanged(Object value) {
        if (value != instanceOfSelected) {
            if (value == null) {
                this.selected = "[null]";
                this.contentPanel.setVisible(false);
            } else {
                this.selected = getSimpleName(value.getClass());
            }
            this.instanceOfSelected = value;
            this.comboBox.getModel().setSelectedItem(this.selected);
            view.selectionChange(value);
            this.contentPanel.setVisible(true);
            this.contentPanel.revalidate();
            this.contentPanel.repaint();
        }
    }

    private void initializeSelfReference() {
        this.self = this;
    }
    private void initializeRunnableRegistry() {
        this.runnableRegistry = new RunnableRegistry();
        this.runnableRegistry.registerAction("[null]",()->{
            view.selectionChange(null);
            instanceOfSelected = null;
            contentPanel.setVisible(false);
            view.clear();
        });
        this.runnableRegistry.registerDefault(()-> {
            SwingUtilities.invokeLater(() -> {
                selected = getSelectedItem();
                try {
                    instanceOfSelected = classConstructorRegistry.get(selected).newInstance();
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
                view.clear();
                view.selectionChange(instanceOfSelected);
                contentPanel.setVisible(true);
                contentPanel.revalidate();
                contentPanel.repaint();
            });
        });
    }

    private String getSelectedItem() {
        return (String) comboBox.getModel().getSelectedItem();
    }

    private void initializeComboBox(){
        var reflections = new Reflections("org.vadere");
        var subClassModel = new ArrayList(reflections.getSubTypesOf(model.getFieldClass()));

        this.comboBox = new JComboBox<>();
        this.comboBox.setModel(initializeComboBoxModel(subClassModel));
        this.comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if( value instanceof Attributes) {
                try {
                    var splitPath = value.toString().split(".Attributes");
                    return new JLabel(splitPath[splitPath.length-1]);
                } catch (Exception e) {
                    throw new RuntimeException("This should not happen registered distribution not found"+value);
                }
            }
            return new JLabel(value.toString());
        });
        this.comboBox.addItemListener(e -> {
            this.runnableRegistry.apply(comboBox.getModel().getSelectedItem());
            revalidate();
            repaint();
        });
        this.add(comboBox);
    }

    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class<?>> classesModel){
        this.classConstructorRegistry = new HashMap<>();
        this.classNameRegistry = new HashMap<>();
        var comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(STRING_NULL);
        classesModel.sort(Comparator.comparing(Class::getSimpleName));
        for (var clazz : classesModel) {
            try {
                classConstructorRegistry.put(getSimpleName(clazz), clazz.getDeclaredConstructor());
                var simpleName = getSimpleName(clazz);
                if (Attributes.class.isAssignableFrom(clazz)) {
                    classNameRegistry.put(simpleName, clazz);
                }
                comboBoxModel.addElement(simpleName);
            } catch (NoSuchMethodException e) {
                System.out.println(getSimpleName(clazz) + " does not implement a default constructor. will be skipped");
            }
        }
        return comboBoxModel;
    }

    @NotNull
    private static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName().replace("Attributes", "");
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(1, 1, 1, 1);
    }

    @Override
    public void revalidateObjectStructure(Object object) {
        try {
            model.getParent().updateParentsFieldValue(model.getFieldName(), object);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
