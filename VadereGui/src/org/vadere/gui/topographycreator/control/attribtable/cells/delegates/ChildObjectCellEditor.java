package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTablePage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;

/**
 * ChildObjectCellEditor is an editor used for any Object not registered and not abstract.
 * It displays all fields of the model inside a AttributeTablePage  displayed inside the contentPanel
 * inherited by AttributeEditor. If the user clicks on the button displaying the object class type the contentPanel will
 * be displayed/hidden.
 */
public class ChildObjectCellEditor extends AttributeEditor  {

    private JButton button;
    private GridBagConstraints gbc;

    private final Class clazz;

    private boolean contentPaneVisible = false;

    private AttributeTablePage page;

    protected Object objectInstance;

    @Override
    public java.util.List<Component> getInputComponent() {
        return Collections.singletonList(button);
    }

    public ChildObjectCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel,initialValue);
        this.clazz = model.getFieldType();
        initializeButton(contentPanel);
    }
    @Override
    protected void initialize(Object initialValue) {
        this.contentPanel.setLayout(new BorderLayout());
        this.contentPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        this.page = createInternalPropertyPane(objectInstance);
        this.contentPanel.setVisible(contentPaneVisible);
        contentPanel.add(page, BorderLayout.CENTER);
        initializeGridBagConstraint();
    }
    private void initializeButton(JPanel contentPanel) {
        this.button = new JButton(AttributeTablePage.generateHeaderName(clazz));
        this.button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //super.mouseClicked(e);
                    contentPaneVisible = !contentPaneVisible;
                    contentPanel.setVisible(contentPaneVisible);
            }
        });
        this.add(button);
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
    protected void onModelChanged(Object object) {
        // since this editor is just a container ... do nothing
    }

    protected AttributeTablePage createInternalPropertyPane(Object newObject) {
        return new AttributeTablePage(model,AttributeTablePage.generateHeaderName(model.getFieldType()),new AttributeTablePage.TableStyler(model));
    }
}
