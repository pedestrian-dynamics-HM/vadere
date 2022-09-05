package org.vadere.gui.topographycreator.control;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.celleditor.*;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
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
    private List<JComponent> tableComponents;

    /**
     * this attribute is used as the editor registry
     */
    private HashMap<String, Constructor> editorConstructors;

    /**
     * this attribute is used as the model for attaching fields with the TableCellEditors
     */
    private HashMap<String, Field> nameFields;

    /**
     * this attribute is used as the model for rendering the TableCellEditors
     */
    private HashMap<String,JComponent> editorObjects;

    private FieldNameRenderer fieldNameRenderer;
    private FieldValueRenderer fieldValueRendere;
    private FieldValueEditor fieldValueEditor;

    private GridBagConstraints gbc;
    private List<AttributeListener> attributeListeners;

    private TopographyCreatorModel topmodel;
    public JAttributeTable(){
        super(new GridBagLayout());
        this.registerDefaultEditors();

        initializeGridBagConstraint();
        initCellRenderer();

        this.setVisible(true);
    }

    public JAttributeTable(AttributeTableModel attrmodel, TopographyCreatorModel topmodel, Attributes object){
        this();
        this.attached = object;
        setModel(attrmodel,topmodel);
        updateView();
        this.fieldValueRendere.setEditors(this.editorObjects);
        this.fieldValueEditor.set(this.editorObjects);
        this.attributeListeners = new ArrayList<>();

    }
    private void registerDefaultEditors() {
        this.editorConstructors = new HashMap<>();
        this.editorObjects = new HashMap<>();
        this.nameFields = new HashMap<>();
        this.tableComponents = new ArrayList<>();

        addTypeEditor(String.class, AttributeTextEditor.class);
        addTypeEditor(Integer.class, AttributeSpinner.class);
        addTypeEditor(Double.class, AttributeDoubleSpinner.class);
        addTypeEditor(Boolean.class, AttributeCheckBox.class);

    }

    private void initCellRenderer() {
        this.fieldNameRenderer = new FieldNameRenderer();
        this.fieldValueRendere = new FieldValueRenderer();
        this.fieldValueEditor = new FieldValueEditor();
    }

    private void initializeGridBagConstraint() {
        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
    }

    public void setModel(AttributeTableModel fieldModel,TopographyCreatorModel topoModel) {
        this.topmodel = topoModel;
        var activeTable = initializeNewTableSection();
        var activeModel = intitializeNewTableModel();

        for(int row = 0; row < fieldModel.getRowCount();row++){
            var field = (Field) fieldModel.getValueAt(row,AttributeTableModel.PropertiesIndex);
            var type = (Class) fieldModel.getValueAt(row,AttributeTableModel.ValuesIndex);
            var typeName = type.getName();
            checkTypeRegisterIfEnum(type);

            if(this.editorConstructors.containsKey(typeName)){
                createTableEntryFromRegisteredClass(activeModel, field, type,panel);
            }else{
                // finish current table add it to tableComponents list
                // so that now other components can be inserted between
                activeModel.addRow(field);
                if(activeModel.getRowCount()>0) {
                    activeTable.setModel(activeModel);
                    this.tableComponents.add(activeTable);

                    activeTable = initializeNewTableSection();
                    activeModel = intitializeNewTableModel();
                }
                if(Modifier.isAbstract(type.getModifiers())) {
                    //sideffect here will insert a component into tableComponents
                    //which is not of type JTable
                    createTableEditorFromAbstractType(field, type,topoModel);
                }else{
                    createTableEditorFromType(field,type,topoModel);
                }
            }
        }
        activeTable.setModel(activeModel);
        this.tableComponents.add(activeTable);

        addTablesToView();
    }

    @NotNull
    private static AttributeTableModel intitializeNewTableModel() {
        return new AttributeTableModel();
    }

    private void createTableEditorFromType(Field field, Class type,TopographyCreatorModel model) {
        this.addTypeEditor(type, AttributeClassSelector.class);
        var constructor = this.editorConstructors.get(type.getName());
        var panel = new JPanel(new GridBagLayout());

        panel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        this.tableComponents.add(panel);
        try {
            this.nameFields.put(field.getName(), field);
            var component = (JComponent) constructor.newInstance(attached,field,model,type,panel);
            insertComponentIntoMap(field, component);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void addTablesToView() {
        for (var component : this.tableComponents){
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

    private JTable initializeNewTableSection() {
        var activeTable = new JTable();
        activeTable.setRowHeight(28);
        activeTable.setIntercellSpacing(new Dimension(0,4));
        activeTable.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activeTable.setBackground(UIManager.getColor("Panel.background"));
        activeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                //super.mouseMoved(e);
                SwingUtilities.invokeLater(()->
                AttributeHelpView.getInstance().loadHelpFromField(
                        (Field) activeTable.getModel().getValueAt(activeTable.rowAtPoint(e.getPoint()),0)
                )
                );
            }
        });
        return activeTable;
    }

    private void createTableEditorFromAbstractType(Field field, Class type,TopographyCreatorModel model) {



        this.addTypeEditor(type, AttributeSubClassSelector.class);

        var constructor = this.editorConstructors.get(type.getName());
        var panel = new JPanel(new GridBagLayout());

        panel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        this.tableComponents.add(panel);
        try {
            this.nameFields.put(field.getName(), field);
            var component = (JComponent) constructor.newInstance(attached,field,model,panel);
            insertComponentIntoMap(field, component);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    private void checkTypeRegisterIfEnum(Class type) {
        if(type.isEnum()){
            this.addTypeEditor(type, AttributeComboBox.class);
        }
    }

    private void createTableEntryFromRegisteredClass(AttributeTableModel activeModel, Field field, Class type,JPanel panel) {
        activeModel.addRow(field);
        Constructor constructor = this.editorConstructors.get(type.getName());
        this.nameFields.put(field.getName(), field);
        initializeNewComponent(field, constructor,panel);
        if(type.isEnum()){
            initializeEnum(field, type.getEnumConstants());
        }
    }

    private void initializeNewComponent(Field field, Constructor constrClass,JPanel panel) {
        insertComponentIntoMap(field, createNewInstanceOf(constrClass,attached,field,topmodel,panel));
    }

    private void insertComponentIntoMap(Field field, JComponent component) {
        this.editorObjects.put(field.getName(), component);
    }


    private void initializeEnum(Field field, Object[] values) {
        ((JComboBox)this.editorObjects.get(field.getName())).setModel(new DefaultComboBoxModel(values));
    }

    private static AttributeEditor createNewInstanceOf(Constructor constructor, Attributes attached, Field field, TopographyCreatorModel model,JPanel panel) {
        AttributeEditor component = null;
        try {
            component = (AttributeEditor) constructor.newInstance(attached,field,model,panel);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        return component;
    }

    public void addTypeEditor(Class type,Class<? extends AttributeEditor> clazz) {
        if(!this.editorConstructors.containsKey(type)){
            Constructor<? extends AttributeEditor> constructor = null;
            try {
                constructor = clazz.getDeclaredConstructor(Attributes.class, Field.class, TopographyCreatorModel.class,Class.class, JPanel.class);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            this.editorConstructors.put(type.getName(),constructor);
        }
    }

    public void updateView(){
        for( var fielName : nameFields.keySet()) {
            var component = (AttributeEditor) editorObjects.get(fielName);
            var field = nameFields.get(fielName);
            field.setAccessible(true);
            try {
                if(field.get(attached) != null) {
                    component.updateView(field.get(attached));
                }
            } catch (IllegalAccessException e) {
            }
            field.setAccessible(false);
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

}
