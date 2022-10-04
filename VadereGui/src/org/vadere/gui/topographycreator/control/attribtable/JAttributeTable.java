package org.vadere.gui.topographycreator.control.attribtable;

import org.apache.commons.math3.util.Pair;
import org.vadere.gui.topographycreator.control.attribtable.cells.EditorRegistry;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.tree.*;
import org.vadere.gui.topographycreator.control.attribtable.util.Layouts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JAttributeTable extends JPanel implements AttributeTreeModel.ValueListener, StructureListener {

    private final List<JComponent> renderOrderModel;
    private final HashMap<AttributeTreeModel.TreeNode, Pair<AttributeEditor,JComponent>> editors;
    private final GridBagConstraints gbc = Layouts.initGridBagConstraint(1.0);
    private final ArrayList<Component> focusOrder;
    Styler rowDelegate;

    private final EditorRegistry registry = EditorRegistry.getInstance();
    AttributeTreeModel.TreeNode model;

    public JAttributeTable(AttributeTreeModel.TreeNode model, Styler rowDelegateStyler) {
        super(new GridBagLayout());
        this.renderOrderModel = new ArrayList<>();
        this.rowDelegate = rowDelegateStyler;
        this.model = model;
        this.focusOrder = new ArrayList<>();
        editors =  new HashMap<>();

        model.addChangeListener(this);
        model.addStructureListener(this);

        this.setVisible(true);
        refreshUI(model);
    }

    public List<Component> getFocusOrder(){
        return focusOrder;
    }

    public void refreshUI(AttributeTreeModel.TreeNode model) {
        this.removeAll();
        if (model != null) {
            this.renderOrderModel.clear();
            var children = model.getChildren();
            for (var key : children.keySet()) {
                var subModel = children.get(key).getSecond();
                var clazz = subModel.getFieldType();
                if(!editors.containsKey(subModel)){
                    var subPanel = new JPanel(new GridBagLayout());
                    subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
                    var editor = registry.create(clazz, subModel, subPanel,subModel.getValueNode().getReference());
                    editors.put(subModel,new Pair(editor,subPanel));
                }
                var delegates = editors.get(subModel);
                var editor = delegates.getFirst();
                focusOrder.addAll(editor.getInputComponent());
                var subPanel = delegates.getSecond();
                renderOrderModel.add(this.rowDelegate.rowDelegateStyle(key, editor));
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

    @Override
    public void structureChanged(AttributeTreeModel.TreeNode node) {
        this.removeAll();
        this.renderOrderModel.clear();
        this.refreshUI(node);
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
