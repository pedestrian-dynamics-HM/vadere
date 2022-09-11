package org.vadere.gui.topographycreator.control.attribtable;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;
import org.vadere.gui.topographycreator.control.attribtable.util.Layouts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class JAttributeTable extends JPanel {

    private final List<JComponent> renderOrderModel;
    private final GridBagConstraints gbc = Layouts.initGridBagConstraint(1.0);
    Styler rowDelegate;

    public JAttributeTable(AbstractModel model, Styler rowDelegateStyler) {
        super(new GridBagLayout());
        this.renderOrderModel = new ArrayList<>();
        this.rowDelegate = rowDelegateStyler;
        this.setVisible(true);
        setModel(model);
    }

    public void setModel(AbstractModel model) {
        this.removeAll();
        this.renderOrderModel.clear();
        var editors = model.getEditors();
        var panels = model.getContentPanels();
        var keySet = editors.keySet();

        for (var key : keySet) {
            var editor = (AttributeEditor) editors.get(key);
            var panel = (JPanel) panels.get(key);
            var delegate = this.rowDelegate.rowDelegateStyle((String) key, editor);
            renderOrderModel.add(delegate);
            if (panel.getComponentCount() > 0) {
                renderOrderModel.add(panel);
            }
        }
        addTablesToView();
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
