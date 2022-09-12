package org.vadere.gui.topographycreator.control.attribtable;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.util.Layouts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JAttributeTable extends JPanel implements ModelListener {

    private final List<JComponent> renderOrderModel;
    private final GridBagConstraints gbc = Layouts.initGridBagConstraint(1.0);
    Styler rowDelegate;

    private final EditorRegistry registry = EditorRegistry.getInstance();
    AttributeTree.TreeNode model;

    public JAttributeTable(AttributeTree.TreeNode model, Styler rowDelegateStyler) {
        super(new GridBagLayout());
        this.renderOrderModel = new ArrayList<>();
        this.rowDelegate = rowDelegateStyler;
        this.model = model;
        model.addChangeListener(this);
        this.setVisible(true);
        setModel(model);
    }

    public void setModel(AttributeTree.TreeNode model) {
        this.removeAll();
        if (model != null) {
            this.renderOrderModel.clear();
            var children = model.getChildren();
            for (var key : children.keySet()) {
                var clazz = children.get(key).getFieldClass();
                var subModel = children.get(key);

                var subPanel = new JPanel(new GridBagLayout());
                subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());

                var editor = registry.create(clazz, subModel, subPanel);
                var delegate = this.rowDelegate.rowDelegateStyle(key, editor);

                renderOrderModel.add(delegate);
                if (subPanel.getComponentCount() > 0) {
                    renderOrderModel.add(subPanel);
                }
            }
            addTablesToView();
            revalidate();
            repaint();
        }
    }

    @Override
    public void modelChanged(Object obj) {
        revalidate();
        repaint();
    }

    public static abstract class Styler {
        public abstract JTable rowDelegateStyle(String id, AttributeEditor editor);
    }

    private void addTablesToView() {
        for (var component : this.renderOrderModel) {
            this.add(component, gbc);
        }
    }
}
