package org.vadere.gui.topographycreator.control.attribtable;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.FieldValueEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.control.attribtable.model.FieldModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributTreeException;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.state.attributes.Attributes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.vadere.gui.topographycreator.control.attribtable.util.ClassFields.*;
import static org.vadere.gui.topographycreator.control.attribtable.util.Layouts.initGridBagConstraint;

public class AttributeTablePage extends JPanel implements ViewListener, ModelListener {
    private final List<FieldModel> tablesListeners;
    Object selectedAttributesInstance;
    ViewListener parentView;
    AttributeTree tree;
    public AttributeTablePage(ViewListener parentView, final Class<? extends Object> clazz) {
        super(new BorderLayout());

        this.setBackground(Color.white);
        this.parentView = parentView;

        this.tablesListeners = new ArrayList<>();

        var panel = new JPanel(new GridBagLayout());
        var gbc = initGridBagConstraint(1.0);

        getSuperClassHierarchy(clazz).stream()
                .forEach(c -> buildClassPanel(panel, gbc, c));

        var parentPane = new JCollapsablePanel(generateHeaderName(clazz), JCollapsablePanel.Style.HEADER, this);
        parentPane.add(panel);
        this.add(new JScrollPane(parentPane));

        tree = new AttributeTree(null, clazz);
    }

    private void buildClassPanel(JPanel panel, GridBagConstraints gbc, Class c) {
        var pnl = createPanel(c, null);
        if (pnl != null) {
            panel.add(pnl, gbc);
        }
    }

    public void updateView(Object object) {
        try {
            tree.updateModel(object);
        } catch (AttributTreeException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        this.selectedAttributesInstance = object;
        for (var table : tablesListeners) {
            table.updateView(object);
        }
    }

    private JPanel createPanel(Class baseClass, Attributes object) {
        var gbc = initGridBagConstraint(1.0);
        var classPanel = new JPanel(new GridBagLayout());

        var fieldsGroupedBySuperClass = getFieldsGroupedBySuperClass(baseClass);

        if (!fieldsGroupedBySuperClass.isEmpty()) {
            var semanticList = getFieldsGroupedBySemanticMeaning(fieldsGroupedBySuperClass.get(baseClass));
            var groups = semanticList.keySet();
            for (var group : groups) {
                var tableModel = new FieldModel((ArrayList<Field>) semanticList.get(group), selectedAttributesInstance, this);
                var table = new JAttributeTable(tableModel, new MyStyler(tableModel));
                this.tablesListeners.add(tableModel);
                if (groupIsUnNamed(group)) {
                    classPanel.add(table, gbc);
                } else {//groupHasName
                    var groupPanel = new JCollapsablePanel(group, JCollapsablePanel.Style.GROUP, classPanel);
                    groupPanel.add(table);
                    classPanel.add(groupPanel, gbc);
                }
            }
            return classPanel;
        }
        return null;
    }

    @NotNull
    public static String generateHeaderName(Class clazz) {
        return clazz.getSimpleName().replaceFirst("Attributes", "");
    }

    private static boolean groupIsUnNamed(String group) {
        return group.equals("");
    }

    public void updateModel(Field field, Object value) {
        if(selectedAttributesInstance!=null) {
            try {
                field.setAccessible(true);
                field.set(selectedAttributesInstance, value);
                field.setAccessible(false);
                parentView.updateModel(selectedAttributesInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Meh");
            }
        }
        this.revalidate();
        this.repaint();
    }

    @Override
    public void updateModel(Object attributes) {
        parentView.updateModel(attributes);
    }

    public static class MyStyler extends JAttributeTable.Styler {

        private final FieldModel tableModel;

        public MyStyler(FieldModel tableModel) {
            this.tableModel = tableModel;
        }

        @Override
        public JTable rowDelegateStyle(String id, AttributeEditor editor) {
            JTable style = new JTable();
            DefaultTableModel model = new DefaultTableModel(new Object[]{"id", "attr"}, 1);
            model.setValueAt(tableModel.getElement(id), 0, 0);
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
            //table.setVisible(true);
            return style;
        }
    }
}
