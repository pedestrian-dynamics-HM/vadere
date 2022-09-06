package org.vadere.gui.topographycreator.control.celleditor;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.gui.topographycreator.view.AttributeTableView;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint
public class AttributeSubClassSelector extends AttributeEditor {
    public static final String STRING_NULL = "[null]";
    private JComboBox<Object> comboBox;
    private AttributeSubClassSelector self;
    private RunnableRegistry runnableRegistry;
    private Map<Class<?>,Constructor<?>> classConstructorRegistry;
    private Map<String,Class<?>> classNameRegistry;
    private GridBagConstraints gbc;
    private String selected;

    private AttributeTableView attributeTableView;
    private Object previousObject;
    public AttributeSubClassSelector(
            Field attached,
            Field field,
            TopographyCreatorModel model,
            JPanel contentPanel
    ) {
        super(attached, field, model,contentPanel);

    }
    @Override
    protected void initialize() {
        this.attributeTableView = new AttributeTableView(getModel());
        initializeGridBagConstraint();
        initializeRunnableRegistry();
        initializeComboBox();
        initializeSelfReference();
        this.contentPanel.add(attributeTableView,gbc);
    }
    @Override
    public void modelChanged(Object value) {
        if(value.getClass() != previousObject) {
            this.previousObject = value;
            this.selected = getSimpleName(value.getClass());
            this.comboBox.getModel().setSelectedItem(this.selected);
            this.attributeTableView.updateView(parent,this.fieldOwner);
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
            this.attributeTableView.selectionChange(null,null);
        });
        this.runnableRegistry.registerDefault(()->{
            selected = getSelectedItem();
            Attributes newAttributeInstance = null;

            try {
                newAttributeInstance = (Attributes) classConstructorRegistry.get(classNameRegistry.get(selected)).newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
            this.attributeTableView.selectionChange(parent,fieldOwner);
            updateModel(previousObject);
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
                classConstructorRegistry.put(clazz, clazz.getDeclaredConstructor());
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
            var simpleName = getSimpleName(clazz);
            if(Attributes.class.isAssignableFrom(clazz)){
                classNameRegistry.put(simpleName,clazz);
            }
            comboBoxModel.addElement(simpleName);
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
}
