package org.vadere.gui.topographycreator.control.attribtable;

import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;
import org.vadere.gui.topographycreator.control.attribtable.util.Layouts;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class JAttributeTable extends JPanel {

    private final List<JComponent> renderOrderModel;
    private final GridBagConstraints gbc = Layouts.initGridBagConstraint(1.0);
    BiFunction<String, AttributeEditor, JTable> rowDelegate;

    public JAttributeTable(AbstractModel model, BiFunction<String, AttributeEditor, JTable> rowDelegate) {
        super(new GridBagLayout());
        this.renderOrderModel = new ArrayList<>();
        this.rowDelegate = rowDelegate;
        this.setVisible(true);
        setModel(model);
    }

    public void setModel(AbstractModel model) {
        var editors = model.getEditors();
        var panels = model.getContentPanels();
        var keySet = editors.keySet();

        for (var key : keySet) {
            var editor = (AttributeEditor) editors.get(key);
            var panel = (JPanel) panels.get(key);
            var delegate = this.rowDelegate.apply((String) key, editor);
            renderOrderModel.add(delegate);
            if (panel.getComponentCount() > 0) {
                renderOrderModel.add(panel);
            }
        }
        addTablesToView();
    }

    private void addTablesToView() {
        for (var component : this.renderOrderModel) {
            this.add(component, gbc);
        }
    }
/*
    private JTable initializeNewTableSection() {
        var activeTable = new JTable();
        activeTable.setRowHeight(28);
        activeTable.setIntercellSpacing(new Dimension(0,4));
        activeTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activeTable.setBackground(UIManager.getColor("Panel.background"));
        activeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                SwingUtilities.invokeLater(()->
                        AttributeHelpView.getInstance().loadHelpFromField(
                                (Field) activeTable.getModel().getValueAt(activeTable.rowAtPoint(e.getPoint()),0)
                        )
                );
            }
        });
        return activeTable;
    }
*/
}
