package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.control.attribtable.JCollapsablePanel;
import org.vadere.gui.topographycreator.control.attribtable.ModelListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.FieldValueEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;

public class AttributeTablePage extends JPanel implements ModelListener {

    private final JCollapsablePanel collapsablePanel;
    private final JScrollPane scrollPane;
    private final JAttributeTable attributeTable;
    AttributeTree model;

    public AttributeTablePage(AttributeTree model) {
        super(new BorderLayout());
        this.setBackground(Color.white);

        this.model = model;

        collapsablePanel = new JCollapsablePanel(generateHeaderName(model.getRootClass()), JCollapsablePanel.Style.HEADER, this);
        attributeTable = new JAttributeTable(model.getRootNode(), new MyStyler());
        scrollPane = new JScrollPane(collapsablePanel);

        var rootNode = model.getRootNode();
        rootNode.addChangeListener(this);

        collapsablePanel.add(attributeTable);
        this.add(scrollPane);
    }


    @NotNull
    public static String generateHeaderName(Class clazz) {
        return clazz.getSimpleName().replaceFirst("Attributes", "");
    }

    public void updateModel(Object object) throws TreeException, IllegalAccessException {
        model.updateModel(object);
    }

    @Override
    public void modelChanged(Object obj) {
        scrollPane.revalidate();
        scrollPane.repaint();
        collapsablePanel.revalidate();
        collapsablePanel.repaint();
    }

    public AttributeTree getModel() {
        return model;
    }

    public static class MyStyler extends JAttributeTable.Styler {

        public MyStyler() {
        }

        @Override
        public JTable rowDelegateStyle(String id, AttributeEditor editor) {
            JTable style = new JTable();
            DefaultTableModel model = new DefaultTableModel(new Object[]{"id", "attr"}, 1);
            //model.setValueAt(tableModel.getElement(id), 0, 0);
            style.setModel(model);
            style.setRowHeight(28);
            style.setIntercellSpacing(new Dimension(0, 4));
            style.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            style.setBackground(UIManager.getColor("Panel.background"));
            style.getColumn("id").setCellRenderer(new FieldNameRenderer(id));
            style.getColumn("attr").setCellRenderer(new FieldValueRenderer(editor));
            style.getColumn("attr").setCellEditor(new FieldValueEditor(editor));
            style.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    SwingUtilities.invokeLater(() ->
                            AttributeHelpView.getInstance().loadHelpFromField(
                                    (Field) style.getModel().getValueAt(style.rowAtPoint(e.getPoint()), 0)
                            )
                    );
                }
            });
            style.setEditingColumn(1);
            return style;
        }
    }
}
