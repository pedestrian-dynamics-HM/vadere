package org.vadere.gui.topographycreator.control.celleditor;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.gui.topographycreator.view.AttributeView;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint
public class AttributeSubClassSelector extends AttributeEditor {
    public static final String STRING_NULL = "[null]";
    private JComboBox<Object> comboBox;
    private JPanel contentPanel;
    private AttributeSubClassSelector self;
    private RunnableRegistry runnableRegistry;
    private Map<Class<?>,Constructor<?>> classConstructorRegistry;
    private GridBagConstraints gbc;
    private String selected;

    private Object previousObject;
    public AttributeSubClassSelector(
            Attributes attached,
            Field field,
            TopographyCreatorModel model,
            ArrayList<Class<?>> subObjectModel,
            JPanel contentPanel
    ) {
        super(attached, field, model);
        initializeGridBagConstraint();
        initializeRunnableRegistry(model);
        try {
            initializeComboBox(subObjectModel, contentPanel);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        initializeSelfReference();

    }

    private void initializeSelfReference() {
        this.self = this;
    }
    private void initializeRunnableRegistry(TopographyCreatorModel model) {
        this.runnableRegistry = new RunnableRegistry();
        this.runnableRegistry.registerAction("[null]",()->{
            nullifyFieldValueOfAttached();
            clearContentPanel();
        });
        this.runnableRegistry.registerDefault(()->{
            try {
                ifNotUpdatedFromOutside(()-> {
                    try {
                        constructNewInternalPropertyPane(selected,model);
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                System.out.println(e);
            }
        });
    }

    private void initializeComboBox(ArrayList<Class<?>> subObjectModel, JPanel contentReceiver) throws NoSuchMethodException {
        this.comboBox = new JComboBox<>();
        this.comboBox.setModel(initializeComboBoxModel(subObjectModel));
        this.contentPanel = contentReceiver;
        this.comboBox.addItemListener(e -> SwingUtilities.invokeLater(() -> {
            ifNotUpdatedFromOutside(()->{
                this.runnableRegistry.apply(comboBox.getSelectedItem());
            });

        }));
        this.add(comboBox);
    }

    private void nullifyFieldValueOfAttached() {
    }
    @NotNull
    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class<?>> classesModel) throws NoSuchMethodException {
        this.classConstructorRegistry = new HashMap<>();
        var comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(STRING_NULL);
        for (var clazz : classesModel) {
            classConstructorRegistry.put(clazz, clazz.getDeclaredConstructor());
            comboBoxModel.addElement(clazz);
        }
        return comboBoxModel;
    }

    @Override
    public void updateValueFromModel(Object value) {
        if(value != previousObject) {
            super.updateValueFromModel(value);
            this.comboBox.getModel().setSelectedItem(value);
            updateInternalPropertyPane((Attributes) value,getModel());
        }
        this.previousObject = value;
    }
    private void updateInternalPropertyPane(Attributes newObject, TopographyCreatorModel model) {
        clearContentPanel();
        var proppane = AttributeView.buildPage(newObject, model);
        contentPanel.add(proppane, gbc);
    }
    private void clearContentPanel() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private Attributes constructNewInternalPropertyPane(String selected, TopographyCreatorModel model) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        var newObject = (Attributes) classConstructorRegistry.get(selected).newInstance();
        updateInternalPropertyPane(newObject, model);
        updateModelFromValue(newObject);
        return newObject;
    }


    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(2, 2, 2, 2);

    }
}
