package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.vadere.gui.topographycreator.control.attribtable.AttributeTableView;
import org.vadere.gui.topographycreator.control.attribtable.AttributeTranslator;
import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint
public class AbstractTypeCellEditor extends AttributeEditor implements AttributeTranslator {
    public static final String STRING_NULL = "[null]";
    private JComboBox<Object> comboBox;
    private AbstractTypeCellEditor self;
    private RunnableRegistry runnableRegistry;
    private Map<String,Constructor<?>> classConstructorRegistry;
    private Map<String,Class<?>> classNameRegistry;
    private GridBagConstraints gbc;
    private String selected;

    private AttributeTableView attributeTableView;
    private Attributes instanceOfSelected;
    public AbstractTypeCellEditor(
            JAttributeTable parent,
            Object fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel contentPanel
    ) {
        super(parent,fieldOwner, field, model,contentPanel);

    }
    @Override
    protected void initialize() {
        this.attributeTableView = new AttributeTableView(this,getModel());
        initializeGridBagConstraint();
        initializeRunnableRegistry();
        initializeComboBox();
        initializeSelfReference();
        this.contentPanel.add(attributeTableView,gbc);
    }
    @Override
    public void modelChanged(Object value) {
        if(value != instanceOfSelected) {
            if(value == null){
                this.selected = "[null]";
                this.contentPanel.setVisible(false);
            }else{
                this.selected = getSimpleName(value.getClass());
            }
            this.instanceOfSelected = (Attributes) value;
            this.comboBox.getModel().setSelectedItem(this.selected);
            this.attributeTableView.selectionChange(value);
            this.contentPanel.setVisible(true);
            this.contentPanel.revalidate();
            this.contentPanel.repaint();
        }
    }

    private void initializeSelfReference() {
        this.self = this;
    }
    private void initializeRunnableRegistry() {
        var model = getModel();
        this.runnableRegistry = new RunnableRegistry();
        this.runnableRegistry.registerAction("[null]",()->{
            updateModel(null);
            instanceOfSelected = null;
            this.attributeTableView.selectionChange(instanceOfSelected);
            contentPanel.setVisible(false);
        });
        this.runnableRegistry.registerDefault(()->{
            selected = getSelectedItem();
            try {
                instanceOfSelected = (Attributes) classConstructorRegistry.get(selected).newInstance();
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            updateModel(instanceOfSelected);
            this.attributeTableView.selectionChange(instanceOfSelected);
            contentPanel.setVisible(true);
        });
    }

    private String getSelectedItem() {
        return (String) comboBox.getModel().getSelectedItem();
    }

    private void initializeComboBox(){
        var reflections = new Reflections("org.vadere");
        var subClassModel = new ArrayList(reflections.getSubTypesOf(this.field.getType()));

        this.comboBox = new JComboBox<>();
        this.comboBox.setModel(initializeComboBoxModel(subClassModel));
        this.comboBox.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if( value instanceof Attributes) {
                try {
                    var splitPath = value.toString().split(".Attributes");
                    return new JLabel(splitPath[splitPath.length-1]);
                } catch (Exception e) {
                    throw new RuntimeException("This should not happen registered distribution not found"+value);
                }
            }
            return new JLabel(value.toString());
        });
        this.comboBox.addItemListener(e ->  {
                this.runnableRegistry.apply(comboBox.getModel().getSelectedItem());
        });
        this.add(comboBox);
    }

    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class<?>> classesModel){
        this.classConstructorRegistry = new HashMap<>();
        this.classNameRegistry = new HashMap<>();
        var comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(STRING_NULL);
        classesModel.sort(Comparator.comparing(Class::getSimpleName));
        for (var clazz : classesModel) {
            try {
                classConstructorRegistry.put(getSimpleName(clazz), clazz.getDeclaredConstructor());
                var simpleName = getSimpleName(clazz);
                if (Attributes.class.isAssignableFrom(clazz)) {
                    classNameRegistry.put(simpleName, clazz);
                }
                comboBoxModel.addElement(simpleName);
            } catch (NoSuchMethodException e) {
                System.out.println(getSimpleName(clazz) + " does not implement a default constructor. will be skipped");
            }
        }
        return comboBoxModel;
    }

    @NotNull
    private static String getSimpleName(Class<?> clazz) {
        return clazz.getSimpleName().replace("Attributes", "");
    }

    private void initializeGridBagConstraint() {
        this.gbc = new GridBagConstraints();
        this.gbc.gridwidth = GridBagConstraints.REMAINDER;
        this.gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        this.gbc.fill = GridBagConstraints.HORIZONTAL;
        this.gbc.weightx = 1;
        this.gbc.insets = new Insets(1, 1, 1, 1);

    }

    @Override
    public void updateModel(Object attributes) {
        parentTranslator.updateModel(field, attributes);
        this.contentPanel.revalidate();
        this.contentPanel.repaint();
    }
}
