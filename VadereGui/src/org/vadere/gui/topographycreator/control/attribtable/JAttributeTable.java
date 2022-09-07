package org.vadere.gui.topographycreator.control.attribtable;

import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.*;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.FieldValueEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.attribtable.cells.renderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.control.attribtable.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JAttributeTable extends JPanel {
    /**
     *
     */
    private final AttributeTablePage parent;
    private Field ownerField;
    private Object fieldOwner;
    private final List<JComponent> renderOrderModel;

    /**
     * this attribute is used as the editor registry
     */
    private final HashMap<String, Constructor<? extends AttributeEditor>> editorConstructors;

    /**
     * this attribute is used as the model for attaching fields with the TableCellEditors
     */
    private final HashMap<String, Field> nameFields;

    /**
     * this attribute is used as the model for rendering the TableCellEditors
     */
    private final HashMap<String,JComponent> editorInstances;
    private FieldNameRenderer fieldNameRenderer;
    private FieldValueRenderer fieldValueRendere;
    private FieldValueEditor fieldValueEditor;
    private final GridBagConstraints gbc = initializeGridBagConstraint();

    private TopographyCreatorModel topmodel;

    public JAttributeTable(
            AttributeTablePage parent,
            AttributeTableModel attrmodel,
            TopographyCreatorModel topmodel,
            Object object)
    {
        super(new GridBagLayout());
        this.editorConstructors = new HashMap<>();
        this.editorInstances = new HashMap<>();
        this.nameFields = new HashMap<>();
        this.renderOrderModel = new ArrayList<>();
        this.registerDefaultEditors();
        initCellRenderer();
        this.setVisible(true);
        this.parent = parent;
        this.fieldOwner = object;
        setModel(attrmodel,topmodel);
        updateView(object);
        this.fieldValueRendere.setEditors(this.editorInstances);
        this.fieldValueEditor.set(this.editorInstances);
    }
    private void initCellRenderer() {
        this.fieldNameRenderer = new FieldNameRenderer();
        this.fieldValueRendere = new FieldValueRenderer();
        this.fieldValueEditor = new FieldValueEditor();
    }

    private static GridBagConstraints initializeGridBagConstraint() {
        var gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        return gbc;
    }

    public void addTypeEditor(Class type,Class<? extends AttributeEditor> clazz) {
        if(!this.editorConstructors.containsKey(type)){
            Constructor<? extends AttributeEditor> constructor = null;
            try {
                constructor = clazz.getDeclaredConstructor(JAttributeTable.class, Object.class, Field.class, TopographyCreatorModel.class, JPanel.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(type.getName(),constructor);
        }
    }

    private void registerDefaultEditors() {
        addTypeEditor(String.class, TextEditCellEditor.class);
        addTypeEditor(Integer.class, SpinnerCellEditor.class);
        addTypeEditor(Double.class, DoubleSpinnerCellEditor.class);
        addTypeEditor(Boolean.class, CheckBoxCellEditor.class);
        addTypeEditor(ArrayList.class, ListCellEditor.class);
    }

    public void setModel(AttributeTableModel fieldModel,TopographyCreatorModel topoModel) {
        this.topmodel = topoModel;
        var activeTable = initializeNewTableSection();
        var activeModel = initializeNewTableModel();

        for(int row = 0; row < fieldModel.getRowCount();row++){
            var field = (Field) fieldModel.getValueAt(row,AttributeTableModel.PropertiesIndex);
            var type = (Class) fieldModel.getValueAt(row,AttributeTableModel.ValuesIndex);
            var typeName = type.getName();
            JPanel subPanel = new JPanel(new GridBagLayout());
            subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());

            if(!this.editorConstructors.containsKey(typeName)){
                if(type.isEnum())
                    this.addTypeEditor(type, ComboBoxCellEditor.class);
                else if(Modifier.isAbstract(type.getModifiers()))
                    this.addTypeEditor(type, AbstractTypeCellEditor.class);
                else
                    this.addTypeEditor(type, ChildObjectCellEditor.class);
            }

            Constructor constructor = this.editorConstructors.get(type.getName());
            AttributeEditor component = null;

            try {
                component = (AttributeEditor) constructor.newInstance(this,fieldOwner, field, topmodel, subPanel);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }

            this.editorInstances.put(field.getName(), component);
            this.nameFields.put(field.getName(), field);
            if(type.isEnum()){
                ((JComboBox) this.editorInstances.get(field.getName())).setModel(new DefaultComboBoxModel(type.getEnumConstants()));
            }
            //create new Table if subPanel got populated
            activeModel.addRow(field);
            if(subPanel.getComponentCount()>0) {
                activeTable.setModel(activeModel);
                this.renderOrderModel.add(activeTable);
                this.renderOrderModel.add(subPanel);
                activeTable = initializeNewTableSection();
                activeModel = initializeNewTableModel();
            }
        }
        if(activeModel.getRowCount()>0) {
            activeTable.setModel(activeModel);
            this.renderOrderModel.add(activeTable);
        }

        addTablesToView();
    }

    private static AttributeTableModel initializeNewTableModel() {
        return new AttributeTableModel();
    }

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

    private void addTablesToView() {
        for (var component : this.renderOrderModel){
            if(component instanceof JTable){
                JTable table = (JTable) component;
                table.getColumn(AttributeTableModel.PropertyString).setCellRenderer(fieldNameRenderer);
                table.getColumn(AttributeTableModel.ValuesString).setCellRenderer(fieldValueRendere);
                table.getColumn(AttributeTableModel.ValuesString).setCellEditor(fieldValueEditor);
                table.setEditingColumn(AttributeTableModel.ValuesIndex);
            }
            this.add(component,gbc);
        }
    }

    public void updateView(){
        for( var fielName : nameFields.keySet()) {
            var component = (AttributeEditor) editorInstances.get(fielName);
            var field = nameFields.get(fielName);
            field.setAccessible(true);
            if(fieldOwner != null) {
                component.updateView(fieldOwner);
            }
            field.setAccessible(false);
        }
    }

    public void updateView(Object object) {
        if (object != null) {
            for (var fieldName : editorInstances.keySet()) {
                var field = nameFields.get(fieldName);
                var editor = (AttributeEditor) editorInstances.get(fieldName);
                try {
                    field.setAccessible(true);
                    editor.setFieldOwner(object);
                    editor.updateView(field.get(object));
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(fieldName + " " + field + " " + editor);
                }
            }
        }
    }

    /*
        @Override
        public void update(Observable o, Object arg) {
            var model = (TopographyCreatorModel)o;
            if(arg instanceof NotifyContext){
                var ctx = (NotifyContext)arg;
                if(!ctx.getNotifyContext().equals(AttributeEditor.class)){
                    updateView();
                }
            }

        }
    */
    public void setFieldOwner(Attributes element) {
        this.fieldOwner = element;
    }

    public void updateModel(Field field, Object object) {
        parent.updateModel(field,object);
        this.revalidate();
        this.repaint();
    }
}
