package org.vadere.gui.topographycreator.control;

import org.reflections.Reflections;
import org.vadere.gui.topographycreator.control.celleditor.*;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldNameRenderer;
import org.vadere.gui.topographycreator.control.cellrenderer.FieldValueRenderer;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.List;

public class JAttributeTable extends JPanel implements Observer {

    /**
     *
     */
    private Object attached;
    private List<JComponent> tableComponents;

    /**
     * this attribute is used as the editor registry
     */
    private HashMap<String,Class> typeEditors;

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

    public JAttributeTable(AttributeTableModel attrmodel,TopographyCreatorModel topmodel, Object object){
        this();
        this.attached = object;
        setModel(attrmodel,topmodel);
        updateView(object);
        this.attributeListeners = new ArrayList<>();
    }
    private void registerDefaultEditors() {
        this.typeEditors = new HashMap<>();
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

    public void setModel(AttributeTableModel dataModel,TopographyCreatorModel topoModel) {
        this.topmodel = topoModel;
        JTable activeTable = initializeNewTableSection();
        var activeModel = new AttributeTableModel();

        for(int row = 0; row < dataModel.getRowCount();row++){
            var field = (Field) dataModel.getValueAt(row,AttributeTableModel.PropertiesIndex);
            var type = (Class) dataModel.getValueAt(row,AttributeTableModel.ValuesIndex);
            var typeName = type.getName();

            checkTypeRegisterIfEnum(type);

            if(this.typeEditors.containsKey(typeName)){
                createTableEntryFromRegisteredClass(activeModel, field, type);
            }else{
                // finish current table add it to tableComponents list
                // so that now other components can be inserted between
                activeModel.addRow(field);
                if(activeModel.getRowCount()>0) {
                    activeTable.setModel(activeModel);
                    this.tableComponents.add(activeTable);

                    activeTable = initializeNewTableSection();
                    activeModel = new AttributeTableModel();
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

    private void createTableEditorFromType(Field field, Class type,TopographyCreatorModel model) {
        this.addTypeEditor(type, AttributeClassSelector.class);
        var constrClass = this.typeEditors.get(type.getName());
        var panel = new JPanel(new GridBagLayout());

        panel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        this.tableComponents.add(panel);
        try {
            this.nameFields.put(field.getName(), field);
            var component = (JComponent) constrClass.getDeclaredConstructor(Class.class,JPanel.class, TopographyCreatorModel.class).newInstance(type,panel,model);
            insertComponentIntoMap(field, component);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
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
        return activeTable;
    }

    private void createTableEditorFromAbstractType(Field field, Class type,TopographyCreatorModel model) {
        var reflections = new Reflections("org.vadere");
        var subClass = new ArrayList(reflections.getSubTypesOf(type));

        this.addTypeEditor(type, AttributeSubClassSelector.class);

        var constrClass = this.typeEditors.get(type.getName());
        var panel = new JPanel(new GridBagLayout());

        panel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        this.tableComponents.add(panel);
        try {
            this.nameFields.put(field.getName(), field);
            var component = (JComponent) constrClass.getDeclaredConstructor(Object.class,Field.class, TopographyCreatorModel.class,ArrayList.class,JPanel.class).newInstance(attached,field,model,subClass,panel);
            insertComponentIntoMap(field, component);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }


    private void checkTypeRegisterIfEnum(Class type) {
        if(type.isEnum()){
            this.addTypeEditor(type, AttributeComboBox.class);
        }
    }

    private void createTableEntryFromRegisteredClass(AttributeTableModel activeModel, Field field, Class type) {
        activeModel.addRow(field);

        Class constrClass = this.typeEditors.get(type.getName());
        try {
            this.nameFields.put(field.getName(), field);
            initializeNewComponent(field, constrClass);
            if(type.isEnum()){
                initializeEnum(field, type.getEnumConstants());
            }

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeNewComponent(Field field, Class constrClass) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        var component = createNewInstanceOf(constrClass,attached,field,topmodel);
        insertComponentIntoMap(field, (JComponent) component);
    }

    private void insertComponentIntoMap(Field field, JComponent component) {
        this.editorObjects.put(field.getName(), component);
    }


    private void initializeEnum(Field field, Object[] values) {
        ((JComboBox)this.editorObjects.get(field.getName())).setModel(new DefaultComboBoxModel(values));
    }

    private static AttributeEditor createNewInstanceOf(Class constrClass,Object attached,Field field, TopographyCreatorModel model) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        AttributeEditor component = (AttributeEditor) constrClass.getDeclaredConstructor(Object.class,Field.class,TopographyCreatorModel.class).newInstance(attached,field,model);
        return component;
    }

    public void addTypeEditor(Class type,Class clazz) {
        try {
            clazz.getDeclaredMethod("updateValueFromModel",new Class[]{Object.class});
        }catch (NoSuchMethodException e){
            throw new IllegalArgumentException();
        }
        if(!this.typeEditors.containsKey(type)){
            this.typeEditors.put(type.getName(),clazz);
        }
    }

    public void updateView(Object attached){
        this.attached = attached;
        this.fieldValueRendere.setEditors(this.editorObjects);
        this.fieldValueEditor.set(this.editorObjects,attached);
        for( var fielName : nameFields.keySet()) {
            var component = (AttributeEditor) editorObjects.get(fielName);
            var field = nameFields.get(fielName);
            field.setAccessible(true);
            try {
                if(field.get(attached) != null) {
                    component.updateValueFromModel(field.get(attached));
                }
            } catch (IllegalAccessException e) {
            }
            field.setAccessible(false);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        var model = (TopographyCreatorModel)o;
        updateView(model.getSelectedElement().getAttributes());

    }

}
