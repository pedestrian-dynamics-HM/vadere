package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.view.AttributeView;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class AttributeClassSelector extends AttributeEditor {

    private final JButton button;
    private GridBagConstraints gbc;

    private final Class clazz;

    private TopographyCreatorModel model;

    private boolean contentPaneVisible = true;

    private final JPanel contentPanel;

    public AttributeClassSelector(Attributes attached, Field field, TopographyCreatorModel model, Class clazz, JPanel contentReceiver) {
        super(attached, field, model);
        this.contentPanel = contentReceiver;
        this.clazz = clazz;
        this.button = new JButton();
        this.add(button);
        Attributes attrs = null;
        try {
            attrs = this.constructNewObject();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        this.createInternalPropertyPane(attrs,model);

        this.button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
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
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(2, 2, 2, 2);

    }

    public void updateValueFromModel(Object value) {
        super.updateValueFromModel(value);
    }

    private void createInternalPropertyPane(Attributes newObject, TopographyCreatorModel model) {
        var proppane = AttributeView.buildPage(newObject, model);
        contentPanel.add(proppane, gbc);
    }

    private Attributes constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Attributes newObject =(Attributes) clazz.getDeclaredConstructor(null).newInstance(null);
        return newObject;
    }

}
