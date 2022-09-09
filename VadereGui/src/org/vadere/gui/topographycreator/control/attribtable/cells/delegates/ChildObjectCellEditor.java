package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.AttributeTablePage;
import org.vadere.gui.topographycreator.control.attribtable.AttributeTableView;
import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ChildObjectCellEditor extends AttributeEditor {

    private final JButton button;
    private GridBagConstraints gbc;

    private final Class clazz;

    private boolean contentPaneVisible = false;

    private AttributeTableView view;

    protected Object objectInstance;

    public ChildObjectCellEditor(
            AbstractModel parent,
            String id,
            JPanel contentPanel
    ) {
        super(parent, id, contentPanel);
        this.contentPanel.setLayout(new BorderLayout());
        this.contentPanel.setBorder(new EmptyBorder(2,2,2,2));
        this.contentPanel.setVisible(contentPaneVisible);
        this.clazz = ((Field) parent.getElement(id)).getType();
        this.button = new JButton(AttributeTablePage.generateHeaderName(clazz));
        this.add(button);
        try {
            this.constructNewObject();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        this.createInternalPropertyPane(objectInstance);

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

    protected void createInternalPropertyPane(Object newObject) {
        view = new AttributeTableView(this);
        view.selectionChange(newObject);
        contentPanel.add(view, BorderLayout.CENTER);
    }

    protected void constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        objectInstance = clazz.getDeclaredConstructor(null).newInstance(null);
    }

}
