package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.AttributeView;
import org.vadere.state.attributes.AttributesAttached;
import org.vadere.state.scenario.ScenarioElement;

import javax.swing.*;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class AttributeClassSelector extends AttributeEditor {

    private JButton button;
    private GridBagConstraints gbc;

    private Class clazz;

    private TopographyCreatorModel model;

    private JPanel contentPanel;

    public AttributeClassSelector(AttributesAttached attached, Field field, TopographyCreatorModel model, Class clazz, JPanel contentReceiver) {
        super(attached, field, model);
        this.button = new JButton();
        this.button.setText("[null]");
        this.add(button);
        this.button.addMouseListener(new MouseInputAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String selected = button.getText();
                    if (isNoClass(selected)) {
                        try {
                            AttributesAttached newObject = constructNewObject();
                            setFieldValue(newObject);
                            clearContentPanel();
                            button.setText(clazz.getSimpleName());
                            createInternalPropertyPane(newObject, model);
                            refreshContentPanel();
                        } catch (Exception exp) {

                        }
                    } else {
                        clearFieldValue();
                        button.setText("[null]");
                        clearContentPanel();
                        refreshContentPanel();
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
        this.contentPanel = contentReceiver;
        this.clazz = clazz;

        initializeGridBagConstraint();
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(2, 2, 2, 2);

    }

    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
        //this.setSelectedItem(value);
    }

    private void createInternalPropertyPane(AttributesAttached newObject, TopographyCreatorModel model) {
        var proppane = AttributeView.buildPage(newObject, model);
        //proppane.selectionChange((ScenarioElement) newObject);
        contentPanel.add(proppane, gbc);
    }

    private void refreshContentPanel() {
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void clearContentPanel() {
        contentPanel.removeAll();
    }

    private AttributesAttached constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AttributesAttached newObject =(AttributesAttached) clazz.getDeclaredConstructor(null).newInstance(null);
        return newObject;
    }


}
