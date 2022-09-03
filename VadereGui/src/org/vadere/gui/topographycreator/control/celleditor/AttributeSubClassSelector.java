package org.vadere.gui.topographycreator.control.celleditor;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.gui.topographycreator.view.AttributeView;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint
public class AttributeSubClassSelector extends AttributeEditor {
    private JComboBox<Object> comboBox;
    private JPanel contentPanel;
    private AttributeSubClassSelector self;
    private RunnableRegistry runnableRegistry;
    private Map<Object,Class<?>> subObjectClasses;
    private GridBagConstraints gbc;
    private Attributes subAttached;
    private String selected;

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
        initializeComboBox(subObjectModel, contentPanel);
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
                var newSubObject = constructNewSubObject(selected);
                clearContentPanel();
                setOwnerFieldTo(newSubObject);
                createInternalPropertyPane(newSubObject, model);
            } catch (Exception ignored) {
            }
        });
    }

    private void initializeComboBox(ArrayList<Class<?>> subObjectModel, JPanel contentReceiver) {
        this.comboBox = new JComboBox<>();
        this.comboBox.setModel(initializeComboBoxModel(subObjectModel));
        this.contentPanel = contentReceiver;
        this.comboBox.addItemListener(e -> SwingUtilities.invokeLater(() -> {
            this.selected = (String) comboBox.getSelectedItem();
            this.runnableRegistry.apply(selected);
        }));
        this.add(comboBox);
    }

    private void setOwnerFieldTo(Attributes newObject) {
        updateModelFromValue(newObject);
        this.subAttached = newObject;
    }
    private void nullifyFieldValueOfAttached() {
        setOwnerFieldTo(null);
        self.setSubAttached(null);
    }
    @NotNull
    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class<?>> classesModel) {
        this.subObjectClasses = classesModel
                .stream()
                .collect(Collectors.toMap(aClass -> (aClass).getSimpleName(), aClass -> aClass));
        var comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement("[null]");
        classesModel.forEach(c -> comboModel.addElement(c.getSimpleName()));
        return comboModel;
    }
    @Override
    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.comboBox.setSelectedItem(value.getClass().getSimpleName());
        nullifyFieldValueOfAttached();
        clearContentPanel();
        var newObject = (Attributes) value;
        setOwnerFieldTo(newObject);
        clearContentPanel();
        createInternalPropertyPane(newObject, getModel());
    }
    private void createInternalPropertyPane(Attributes newObject, TopographyCreatorModel model) {
        var proppane = AttributeView.buildPage(newObject, model);
        contentPanel.add(proppane, gbc);
    }
    private void clearContentPanel() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private Attributes constructNewSubObject(String selected) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class<? extends Attributes> clazz = (Class<? extends Attributes>) subObjectClasses.get(selected);
        Attributes newObject = clazz.getDeclaredConstructor((Class<?>) null).newInstance();
        setSubAttached(newObject);
        return newObject;
    }

    private void setSubAttached(Attributes attached){
        this.subAttached = attached;
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
