package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.ListValueEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.ButtonColumnRenderer;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.ListValueRenderer;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ListCellEditor extends ChildObjectCellEditor {
    private JAttributeTable table;


    private Object instanceOfSelected;

    public ListCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }


    @Override
    protected void createInternalPropertyPane(Object newObject) {
        table = new JAttributeTable(model, new JAttributeTable.Styler() {
            @Override
            public JTable rowDelegateStyle(String id, AttributeEditor editor) {
                JTable style = new JTable();
                DefaultTableModel modelt = new DefaultTableModel(new Object[]{"attr", "btn"}, 1);
                //modelt.setValueAt(valueModel.getElement(id), 0, 0);
                style.setModel(modelt);
                style.setRowHeight(28);
                style.setIntercellSpacing(new Dimension(0, 4));
                style.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                style.setBackground(UIManager.getColor("Panel.background"));
                style.getColumn("attr").setCellRenderer(new ListValueRenderer(editor, id));
                style.getColumn("attr").setCellEditor(new ListValueEditor(editor, id));
                style.getColumn("btn").setCellRenderer(new ButtonColumnRenderer());
                style.getColumn("btn").setPreferredWidth(10);
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
                style.addComponentListener(new ComponentAdapter() {
                    @Override
                    public void componentResized(ComponentEvent e) {
                        super.componentResized(e);
                    }
                });
                style.setEditingColumn(1);
                //table.setVisible(true);
                return style;
            }
        });
        this.contentPanel.add(new JLabel("hi"));
        this.contentPanel.add(table, BorderLayout.CENTER);
    }


    @Override
    protected void constructNewObject() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        //super.constructNewObject();
    }
}
