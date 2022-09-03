package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.control.JCollapsablePanel;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.reflection.VadereAttribute;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class AttributeView extends JPanel implements ISelectScenarioElementListener{

    JPanel pageView;
    JPanel helpView;
    HashMap<ScenarioElement,JScrollPane> editorPages;
    TopographyCreatorModel panelModel;


    AttributeView(final TopographyCreatorModel defaultModel){
        super(new GridBagLayout());

        this.panelModel = defaultModel;
        this.panelModel.addSelectScenarioElementListener(this);

        this.editorPages = new HashMap<>();

        pageView = new JPanel(new BorderLayout());
        helpView = new JPanel();

        var minimalHelpViewSize = new Dimension(1,Toolkit.getDefaultToolkit().getScreenSize().height/10);

        helpView.setMinimumSize(minimalHelpViewSize);
        helpView.setBackground(Color.red);

        var gbcPage = initGridBagConstraint(1.0);
        var gbcHelp = initGridBagConstraint(0.2);

        this.add(pageView,gbcPage);
        this.add(helpView,gbcHelp);

    }

    @Override
    public void selectionChange(ScenarioElement scenarioElement) {
        if (scenarioElement != null){
            if(!editorPages.containsKey(scenarioElement)){
                var attributePage = buildPage(scenarioElement.getAttributes(),panelModel);
                this.editorPages.put(scenarioElement,attributePage);
            }
            var attributePage = editorPages.get(scenarioElement);
            this.pageView.removeAll();
            this.pageView.add(attributePage,BorderLayout.NORTH);

        }
    }

    public static JScrollPane buildPage(Attributes object, TopographyCreatorModel model){
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
        return new JScrollPane(panel);
    }

    private static JPanel createPanel(Class baseClass, Attributes object, TopographyCreatorModel model) {
        var gbc = initGridBagConstraint(1.0);
        var fieldsGroupedBySuperClass = getFieldsGroupedBySuperClass(baseClass);
        if (!fieldsGroupedBySuperClass.isEmpty()) {
            var simpleClassName = baseClass.getSimpleName();
            var semanticList = getFieldsGroupedBySemanticMeaning(fieldsGroupedBySuperClass.get(baseClass));
            var classPanel = new JCollapsablePanel(simpleClassName, false);
            var groups = semanticList.keySet();

            for (var group : groups) {
                var tableModel = new AttributeTableModel(semanticList.get(group));
                var table = new JAttributeTable(tableModel,model,object);
                /*table.addAttributeListener(new AttributeListener() {
                    @Override
                    public void attributeSelected(Field field) {
                        //selectedField = field;
                        firePropertyChange("property",null,field.getAnnotation(VadereAttribute.class).descr());
                        System.out.println(field.getAnnotation(VadereAttribute.class).descr());
                    }
                });*/
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

    private static boolean groupIsUnNamed(String group) {
        return group.equals("");
    }
    private static Map<Class<?>, java.util.List<Field>> getFieldsGroupedBySuperClass(Class clazz){
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(VadereAttribute.class)!= null)
                .collect(Collectors.groupingBy(field -> field.getDeclaringClass(),Collectors.toList()));
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

    private static GridBagConstraints initGridBagConstraint(double weighty) {
        var gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.PAGE_START;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = weighty;
        return gbc;
    }
}