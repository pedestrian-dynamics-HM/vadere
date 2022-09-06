package org.vadere.gui.topographycreator.view;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.control.JCollapsablePanel;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.VadereAttributeClass;
import org.vadere.util.Attributes;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.AttributesAttached;
import org.vadere.util.reflection.VadereAttribute;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.vadere.gui.topographycreator.utils.Layouts.initGridBagConstraint;

public class AttributeTablePage extends JPanel{
    TopographyCreatorModel panelModel;

    private List<JAttributeTable> tablesListeners;

    public AttributeTablePage(final Class clazz,final TopographyCreatorModel defaultModel){
        super(new BorderLayout());
        this.setBackground(Color.white);
        this.panelModel = defaultModel;

        this.tablesListeners = new ArrayList<>();

        var panel = new JPanel(new GridBagLayout());
        var gbc = initGridBagConstraint(1.0);

        getSuperClassHierarchy(clazz)
                .stream()
                .forEach(c ->{
                    var pnl = createPanel(c,null,defaultModel);
                    if(pnl != null) {
                        panel.add(pnl, gbc);
                    }
                });
        var parentPane = new JCollapsablePanel(generateHeaderName(clazz),false);
        parentPane.add(panel);
        this.add(new JScrollPane(parentPane));
    }

    public void updateView(Object parent,Field attachedObject){
        for(var table : tablesListeners){
            table.updateView(parent,attachedObject);
        }
    }
/*
    @Override
    public void selectionChange(ScenarioElement element) {
        if(element != null) {
            System.out.println(element);
            System.out.println(element.getAttributes());
            Class elementClass = element.getAttributes().getClass();
            if(!editorPages.containsKey(elementClass)){
                var attributePage = buildPage(element.getAttributes(),panelModel);
                this.editorPages.put(elementClass,attributePage);
            }
            var attributePage = editorPages.get(elementClass);
            attributePage.updateFields(element);
            this.removeAll();
            this.add(attributePage,BorderLayout.NORTH);
        }else{
            this.removeAll();
        }
        this.revalidate();
        this.repaint();
    }
*/
    /*
    public void attributeSelectionChanged(Attributes optionalElement) {
        if(optionalElement != null) {
            if(!editorPages.containsKey(optionalElement.getClass())){
                var attributePage = buildPage(optionalElement,panelModel);
                this.editorPages.put(optionalElement.getClass(),attributePage);
            }
            var attributePage = editorPages.get(optionalElement.getClass());
            this.removeAll();
            this.add(attributePage,BorderLayout.NORTH);
        }else{
            this.removeAll();
        }
        this.revalidate();
        this.repaint();
    }
*/
    /*
    public static JAttributePage buildPage(Attributes object, TopographyCreatorModel model){
        var clazz = object.getClass();
        var panel = new JPanel(new GridBagLayout());
        var gbc = initGridBagConstraint(1.0);

        getSuperClassHierarchy(clazz)
                .stream()
                .forEach(c ->{
                    var pnl = createPanel(c,object,model);
                    if(pnl != null) {
                        panel.add(pnl, gbc);
                    }
                });
        var parentPane = new JAttributePage2(generateHeaderName(object.getClass()));
        parentPane.addScrollContent(panel);
        return parentPane;
    }
*/
    @NotNull
    public static String generateHeaderName(Class clazz) {
        return clazz.getSimpleName().replaceFirst("Attributes", "");
    }

    private JPanel createPanel(Class baseClass, Attributes object, TopographyCreatorModel model) {
        var gbc = initGridBagConstraint(1.0);

        var classPanel = new JPanel(new GridBagLayout()); //generateClassPanelFromRule(baseClass, noHeader);


        var fieldsGroupedBySuperClass = getFieldsGroupedBySuperClass(baseClass);

        if (!fieldsGroupedBySuperClass.isEmpty()) {
            var semanticList = getFieldsGroupedBySemanticMeaning( fieldsGroupedBySuperClass.get(baseClass));
            var groups = semanticList.keySet();

            for (var group : groups) {
                var tableModel = new AttributeTableModel(semanticList.get(group));
                var table = new JAttributeTable(tableModel,model,object);
                this.tablesListeners.add(table);
                if (groupIsUnNamed(group)) {
                    classPanel.add(table, gbc);
                } else {//groupHasName
                    var groupPanel = new JCollapsablePanel(group, true);
                    groupPanel.add(table);
                    classPanel.add(groupPanel, gbc);
                }
                model.addObserver(table);
            }
            return classPanel;
        }
        return null;
    }

    private static JPanel generateClassPanelFromRule(Class baseClass, boolean noHeader) {
        JPanel classPanel = null;
        var simpleClassName = baseClass.getSimpleName();
        if(noHeader){
            classPanel = new JPanel(new GridBagLayout());
        }else{
            classPanel = new JCollapsablePanel(simpleClassName, false);
        }
        return classPanel;
    }

    private static boolean groupIsUnNamed(String group) {
        return group.equals("");
    }
    private static Map<Class<?>, java.util.List<Field>> getFieldsGroupedBySuperClass(Class clazz){
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(VadereAttribute.class)!= null)
                .collect(Collectors.groupingBy(field -> field.getDeclaringClass(),Collectors.toList()));
    }

    private static java.util.List<Field> generateFieldModelFromRule(Class clazz){
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field ->{
                    var optAnnot = Optional.ofNullable(field.getDeclaringClass().getAnnotation(VadereAttributeClass.class));
                    if(optAnnot.isPresent() && optAnnot.get().includeAll()){
                        return true;
                    }
                    else if (Optional.ofNullable(field.getAnnotation(VadereAttribute.class)).isPresent()){
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    private static Map<String, java.util.List<Field>> getFieldsGroupedBySemanticMeaning(java.util.List<Field> fieldList){
        return fieldList.stream().collect(Collectors.groupingBy(field->field.getAnnotation(VadereAttribute.class).group(),Collectors.toList()));
    }
    private static Map<Class<?>, Map<String, List<Field>>> extractGroupedFields(Class clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .collect(Collectors.groupingBy(field -> field.getDeclaringClass(),Collectors.groupingBy(field->field.getAnnotation(VadereAttribute.class).group(),Collectors.toList())));
    }

    private static Vector<Class> getSuperClassHierarchy(Class clazz) {
        Vector<Class> classOrder = new Vector<>();
        do{
            classOrder.add(0,clazz);
            clazz = clazz.getSuperclass();
        }while(clazz.getSuperclass() != null);
        return classOrder;
    }

}
