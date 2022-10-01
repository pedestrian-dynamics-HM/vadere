package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.vadere.gui.topographycreator.control.attribtable.tree.AbstrNode;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.ui.AttributeTableView;
import org.vadere.gui.topographycreator.utils.RunnableRegistry;
import org.vadere.state.attributes.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This AttributeEditor Class is used by JAttributeTable as the default fallback for
 * any type which has no preregistered editor and is an abstract type.
 * It lists all subclasses as items in the combobox and creates a new JPropertyPane for the
 * selected type right under the table
 */

//TODO: add jtable to constructor for revalidation & repaint

public class AbstractTypeCellEditor extends AttributeEditor{
    public static final String STRING_NULL = "[null]";
    protected JComboBox<Object> comboBox;
    private AbstractTypeCellEditor self;
    private RunnableRegistry runnableRegistry;
    private Map<String, Constructor<?>> classConstructorRegistry;
    private Map<String, Class<?>> classNameRegistry;
    private GridBagConstraints gbc;
    private String selected;

    protected AttributeTableView view;
    private Object instanceOfSelected;

    @Override
    public List<Component> getInputComponent() {
        return Collections.singletonList(comboBox);
    }

    public AbstractTypeCellEditor(AttributeTreeModel.TreeNode model, JPanel contentPanel, Object initialValue) {
        super(model, contentPanel,initialValue);
    }


    @Override
    protected void initialize(Object initialValue) {
        view = new AttributeTableView(null);
        buildSubPages();
        initializeGridBagConstraint();
        initializeRunnableRegistry();
        initializeComboBox();
        if(initialValue == null){
            this.contentPanel.setVisible(false);
        }else{
            this.contentPanel.setVisible(true);
            onModelChanged(initialValue);
        }

        initializeSelfReference();
        this.contentPanel.add(view, gbc);
    }

    protected void buildSubPages() {
        var models = ((AbstrNode)model).getSubClassModels();
        for(var model : models.values()){
            view.buildPageFor(model);
        }
    }

    @Override
    public void onModelChanged(Object value) {
        if (value != instanceOfSelected) {
            this.instanceOfSelected = value;
            if (value == null) {
                this.selected = "[null]";
            } else {
                this.selected = getSimpleName(value.getClass());
            }
            this.comboBox.getModel().setSelectedItem(this.selected);
        }
    }

    private void initializeSelfReference() {
        this.self = this;
    }
    private void initializeRunnableRegistry() {
        this.runnableRegistry = new RunnableRegistry();
        this.runnableRegistry.registerAction("[null]",()->{
            instanceOfSelected = null;
            contentPanel.setVisible(false);
            view.clear();
            try {
                (model).getValueNode().setValue(null);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        this.runnableRegistry.registerDefault(()-> {
                selected = getSelectedItem();
                if(canUpdate()) {
                    try {
                        instanceOfSelected = classConstructorRegistry.get(selected).newInstance();
                        try {
                            (model).getValueNode().setValue(instanceOfSelected);
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                view.clear();

                view.selectionChange(instanceOfSelected);
                contentPanel.setVisible(true);
                contentPanel.revalidate();
                contentPanel.repaint();
        });
    }

    protected String getSelectedItem() {
        return (String) comboBox.getModel().getSelectedItem();
    }

    private void initializeComboBox(){
        ArrayList subClassModel = getReflectionModel();

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
        this.comboBox.addItemListener(e -> {
            this.runnableRegistry.apply(comboBox.getModel().getSelectedItem());
            revalidate();
            repaint();
        });
        this.add(comboBox);
    }

    @NotNull
    protected ArrayList getReflectionModel() {
        return (ArrayList) ((AbstrNode)model).getSubClassModels().values().stream().map(n -> n.getFieldType()).collect(Collectors.toList());
    }

    private DefaultComboBoxModel<Object> initializeComboBoxModel(ArrayList<Class<?>> classesModel){
        this.classConstructorRegistry = new HashMap<>();
        this.classNameRegistry = new HashMap<>();
        var comboBoxModel = new DefaultComboBoxModel<>();
        comboBoxModel.addElement(STRING_NULL);
        classesModel.sort(Comparator.comparing(Class::getSimpleName));
        for (var clazz : classesModel) {
            try {
                var simpleName = getSimpleName(clazz);
                classConstructorRegistry.put(getSimpleName(clazz), clazz.getDeclaredConstructor());
                classNameRegistry.put(simpleName, clazz);
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

}
