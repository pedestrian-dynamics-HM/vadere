package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.ObjectNode;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTablePage;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTableView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.InvocationTargetException;

public class ChildObjectCellEditor extends AttributeEditor implements Revalidatable {

    private final JButton button;
    private GridBagConstraints gbc;

    private final Class clazz;

    private boolean contentPaneVisible = false;

    private AttributeTableView view;

    protected Object objectInstance;

    public ChildObjectCellEditor(AttributeTree.TreeNode model, JPanel contentPanel,Object initialValue) {
        super(model, contentPanel,initialValue);
        this.contentPanel.setLayout(new BorderLayout());
        this.contentPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.contentPanel.setVisible(contentPaneVisible);
        this.clazz = model.getFieldType();
        this.button = new JButton(AttributeTablePage.generateHeaderName(clazz));
        this.add(button);
        /*try {
            this.constructNewObject();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }*/
        this.createInternalPropertyPane(objectInstance);

        this.button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //super.mouseClicked(e);
                    contentPaneVisible = !contentPaneVisible;
                    contentPanel.setVisible(contentPaneVisible);
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
    protected void initialize(Object initialValue) {
        view = new AttributeTableView(this);
        view.selectionChange(initialValue);
    }

    public void onModelChanged(Object value) {
        view.selectionChange(value);
    }

    protected void createInternalPropertyPane(Object newObject) {
        view.selectionChange(newObject);
        contentPanel.add(view, BorderLayout.CENTER);
    }

    protected void constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        objectInstance = clazz.getDeclaredConstructor(null).newInstance(null);
    }

    @Override
    public void revalidateObjectStructure(Object object) {
        try {
            ((ObjectNode) model).getValueNode().setValue(object);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
