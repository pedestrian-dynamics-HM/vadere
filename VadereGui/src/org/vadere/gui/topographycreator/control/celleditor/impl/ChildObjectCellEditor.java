package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.AttributeTablePage;
import org.vadere.gui.topographycreator.view.AttributeTableView;
import org.vadere.gui.topographycreator.view.AttributeTranslator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ChildObjectCellEditor extends AttributeEditor implements AttributeTranslator {

    private final JButton button;
    private GridBagConstraints gbc;

    private final Class clazz;

    private TopographyCreatorModel model;

    private boolean contentPaneVisible = false;

    private AttributeTableView view;

    protected Object objectInstance;

    public ChildObjectCellEditor(
            JAttributeTable parent,
            Object fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel contentPanel
    ) {
        super(parent, fieldOwner, field, model, contentPanel);
        this.contentPanel.setLayout(new BorderLayout());
        this.contentPanel.setBorder(new EmptyBorder(2,2,2,2));
        this.contentPanel.setVisible(contentPaneVisible);
        this.clazz = field.getType();
        this.button = new JButton(AttributeTablePage.generateHeaderName(clazz));
        this.add(button);
        try {
            this.constructNewObject();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        this.createInternalPropertyPane(objectInstance, model);

        this.button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //super.mouseClicked(e);
                SwingUtilities.invokeLater(() -> {
                    contentPaneVisible = !contentPaneVisible;
                    contentPanel.setVisible(contentPaneVisible);
                });
            }
        });
        initializeGridBagConstraint();
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.BOTH;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(2, 2, 2, 2);

    }

    @Override
    protected void initialize() {

    }

    public void modelChanged(Object value) {
        view.updateView(value);
    }

    protected void createInternalPropertyPane(Object newObject, TopographyCreatorModel model) {
        view = new AttributeTableView(this, model);
        view.selectionChange(newObject);
        contentPanel.add(view, BorderLayout.CENTER);
    }

    protected void constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        objectInstance = clazz.getDeclaredConstructor(null).newInstance(null);
    }

    @Override
    public void updateModel(Object attributes) {
        parentTranslator.updateModel(field, attributes);
    }
}
