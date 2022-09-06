package org.vadere.gui.topographycreator.control;

import org.vadere.gui.topographycreator.control.celleditor.*;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

public class JAttributeTable extends JPanel implements Observer {
    /**
     *
     */
    private Attributes attached;
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
    public JAttributeTable(){
        super(new GridBagLayout());
        this.editorConstructors = new HashMap<>();
        this.editorInstances = new HashMap<>();
        this.nameFields = new HashMap<>();
        this.renderOrderModel = new ArrayList<>();
        this.registerDefaultEditors();
        initCellRenderer();
        this.setVisible(true);
    }
    public JAttributeTable(AttributeTableModel attrmodel, TopographyCreatorModel topmodel, Attributes object){
        this();
        this.attached = object;
        setModel(attrmodel,topmodel);
        updateView();
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
                constructor = clazz.getDeclaredConstructor(Field.class, Field.class, TopographyCreatorModel.class,JPanel.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(type.getName(),constructor);
        }
    }

    private void registerDefaultEditors() {
        addTypeEditor(String.class, AttributeTextEditor.class);
        addTypeEditor(Integer.class, AttributeSpinner.class);
        addTypeEditor(Double.class, AttributeDoubleSpinner.class);
        addTypeEditor(Boolean.class, AttributeCheckBox.class);

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
                    this.addTypeEditor(type, AttributeComboBox.class);
                else if(Modifier.isAbstract(type.getModifiers()))
                    this.addTypeEditor(type, AttributeSubClassSelector.class);
                else
                    this.addTypeEditor(type, AttributeClassSelector.class);
            }

            Constructor constructor = this.editorConstructors.get(type.getName());
            AttributeEditor component = null;

            try {
                component = (AttributeEditor) constructor.newInstance(attached, field, topmodel, subPanel);
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
            if(attached != null) {
                component.updateView(attached);
            }
            field.setAccessible(false);
        }
    }

    public void updateView(Object parent,Field attachedObject){
        for(var fieldName : editorInstances.keySet()){
            var field = nameFields.get(fieldName);
            var editor = (AttributeEditor)editorInstances.get(fieldName);
            editor.setParent(parent);
            editor.setAttached(attachedObject);
            try {
                field.setAccessible(true);
                editor.updateView(field.get(attachedObject.get(((ScenarioElement)parent).getAttributes())));
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

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

    public void setAttached(Attributes element) {
        this.attached = element;
    }

}
