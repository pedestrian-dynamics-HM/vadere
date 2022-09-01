package org.vadere.gui.topographycreator.control.celleditor;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.AttributeView;
import org.vadere.state.attributes.AttributesAttached;
import org.vadere.state.scenario.ScenarioElement;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
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
public class AttributeSubClassSelector extends AttributeEditor {
    private Map<String, Class> classMap;
    private JComboBox comboBox;
    private GridBagConstraints gbc;
    private JPanel contentPanel;

    public AttributeSubClassSelector(AttributesAttached attached, Field field, TopographyCreatorModel model, ArrayList<Class> classesModel, JPanel contentReceiver) {
        super(attached, field, model);
        initializeGridBagConstraint();
        this.comboBox = new JComboBox();
        this.comboBox.setModel(initializeComboBoxModel(classesModel));
        this.contentPanel = contentReceiver;
        this.add(comboBox);
        this.comboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String selected = (String) comboBox.getSelectedItem();
                    if (isNoClass(selected)) {
                        clearFieldValue();
                        clearContentPanel();
                        refreshContentPanel();
                    } else {
                        try {
                            AttributesAttached newObject = constructNewObject(selected);
                            setFieldValue(newObject);
                            clearContentPanel();
                            createInternalPropertyPane(newObject, model);
                            refreshContentPanel();
                        } catch (Exception exp) {

                        }
                    }

                });

            }

            private void setFieldValue(Object newObject) {
                updateModelFromValue(newObject);
            }

            private boolean isNoClass(String selected) {
                return selected.equals("[null]");
            }

            private void clearFieldValue() {
                setFieldValue(null);
            }
        });

        //this.setBorder(new FlatTextBorder());
    }

    @NotNull
    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class> classesModel) {
        this.classMap = classesModel.stream()
                .collect(Collectors.toMap(aClass -> ((Class) aClass).getSimpleName(), aClass -> aClass));
        var comboModel = new DefaultComboBoxModel<>();

        comboModel.addElement("[null]");
        classesModel.forEach(c -> comboModel.addElement(c.getSimpleName()));
        return comboModel;
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(2, 2, 2, 2);

    }

    @Override
    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        this.comboBox.setSelectedItem(value);
    }

    private void createInternalPropertyPane(AttributesAttached newObject, TopographyCreatorModel model) {
        var proppane = AttributeView.buildPage(newObject, model);
        proppane.selectionChange((ScenarioElement) newObject);
        contentPanel.add(proppane, gbc);
    }

    private void refreshContentPanel() {
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void clearContentPanel() {
        contentPanel.removeAll();
    }

    private AttributesAttached constructNewObject(String selected) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Class clazz = classMap.get(selected);
        AttributesAttached newObject = (AttributesAttached) clazz.getDeclaredConstructor(null).newInstance(null);
        return newObject;
    }


}
