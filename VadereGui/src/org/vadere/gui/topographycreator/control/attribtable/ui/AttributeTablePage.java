package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.control.attribtable.JCollapsablePanel;
import org.vadere.gui.topographycreator.control.attribtable.ValueListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.CellNameDelegateWrapper;
import org.vadere.gui.topographycreator.control.attribtable.cells.CellValueDelegateWarpper;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;


/** This class is supposed to be used as the gui container for on single class archetype **/
public class AttributeTablePage extends JPanel implements ValueListener {

    /** container is used as a parent container for the class attribute table.
     * All attributes can be hidden when the user clicks on the header of the panel.
     * It displays the classes simple name in the header.
    **/
    private final JCollapsablePanel container;
    /**
     * view is the view in the MVC pattern
     */
    private final JAttributeTable view;
    AttributeTree.TreeNode model;

    public AttributeTablePage(AttributeTree.TreeNode model) {
        super(new BorderLayout());
        this.setBackground(Color.white);

        this.model = model;

        container = new JCollapsablePanel(
                generateHeaderName(model.getFieldClass()),
                JCollapsablePanel.Style.HEADER
        );
        view = new JAttributeTable(model, new TableStyler(model));
        container.add(view);

        this.add(container);
        model.addChangeListener(this);
    }


    @NotNull
    public static String generateHeaderName(Class clazz) {
        return clazz.getSimpleName().replaceFirst("Attributes", "");
    }

    public void updateModel(Object object) throws TreeException, IllegalAccessException {
        model.updateStructure(object);
        model.updateValues(object);
    }

    @Override
    public void modelChanged(Object obj) {
        container.revalidate();
        container.repaint();
    }

    public AttributeTree.TreeNode getModel() {
        return model;
    }


    /**
     * TableStyler is an implementation of a JAttributeTable to display every
     * field in the model in a  | name | editor | structure.
     *                          ----------------
     *                          | name | editor |
     */
    public static class TableStyler extends JAttributeTable.Styler {
        private final AttributeTree.TreeNode model;

        public TableStyler(AttributeTree.TreeNode model) {
            this.model = model;
        }

        @Override
        public JTable rowDelegateStyle(String id, AttributeEditor editor) {
            JTable style = new JTable();

            /** custom table model wich disallows the editing of the name column **/
            DefaultTableModel tableModel = new DefaultTableModel(new Object[]{"id", "attr"}, 1){
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 0;
                }
            };

            /** JAttributeTable does not use fields this field. The field is only used for the
             *  AttributeHelpView so that we are aware of the field that correspons to the cell
             *  clicked on **/
            tableModel.setValueAt(model.get(id).getField(), 0, 0);

            /** some styling */
            style.setModel(tableModel);
            style.setRowHeight(28);
            style.setIntercellSpacing(new Dimension(0, 4));
            style.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            style.setBackground(UIManager.getColor("Panel.background"));

            /** initialization of the cell delegates **/
            var idDelegate = new CellNameDelegateWrapper(id);
            var attrEdlegate = new CellValueDelegateWarpper(editor);

            style.getColumn("id").setCellRenderer(idDelegate);
            style.getColumn("id").setCellRenderer(idDelegate);
            style.getColumn("attr").setCellRenderer(attrEdlegate);
            style.getColumn("attr").setCellEditor(attrEdlegate);
            style.setEditingColumn(1);

            /** initialize the listener to update the AttributeHelpView**/
            style.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    SwingUtilities.invokeLater(() -> {
                        var field = (Field) style.getModel().getValueAt(style.rowAtPoint(e.getPoint()), 0);
                        if(field!=null)
                            AttributeHelpView.getInstance().loadHelpFromField(field);
                    });
                }
            });

            /** this section is supposed enable the resizing of the editor cells**/
            style.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    super.componentResized(e);
                    editor.setSize(style.getWidth() / 2, style.getRowHeight());
                }
            });


            return style;
        }
    }
}
