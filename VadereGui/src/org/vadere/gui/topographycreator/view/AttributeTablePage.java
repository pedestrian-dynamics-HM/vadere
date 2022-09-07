package org.vadere.gui.topographycreator.view;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.control.JCollapsablePanel;
import org.vadere.gui.topographycreator.model.AttributeTableModel;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.vadere.gui.topographycreator.utils.ClassFields.*;
import static org.vadere.gui.topographycreator.utils.Layouts.initGridBagConstraint;

public class AttributeTablePage extends JPanel{
    TopographyCreatorModel panelModel;
    AttributeTableView parentView;
    Attributes selectedAttributesInstance;
    private final List<JAttributeTable> tablesListeners;

    public AttributeTablePage(AttributeTableView parentView,final Class<? extends Attributes> clazz,final TopographyCreatorModel defaultModel){
        super(new BorderLayout());

        this.setBackground(Color.white);
        this.parentView = parentView;
        this.panelModel = defaultModel;

        this.tablesListeners = new ArrayList<>();

        var panel = new JPanel(new GridBagLayout());
        var gbc = initGridBagConstraint(1.0);

        getSuperClassHierarchy(clazz).stream()
                .forEach(c ->buildClassPanel(defaultModel, panel, gbc, c));

        var parentPane = new JCollapsablePanel(generateHeaderName(clazz), JCollapsablePanel.Style.HEADER);
        parentPane.add(panel);
        this.add(new JScrollPane(parentPane));
    }

    private void buildClassPanel(TopographyCreatorModel defaultModel, JPanel panel, GridBagConstraints gbc, Class c) {
        var pnl = createPanel(c,null, defaultModel);
        if(pnl != null) {
            panel.add(pnl, gbc);
        }
    }

    public void updateView(Attributes object){
        this.selectedAttributesInstance = object;
        for(var table : tablesListeners){
            table.updateView(object);
        }
    }

    private JPanel createPanel(Class baseClass, Attributes object, TopographyCreatorModel model) {
        var gbc = initGridBagConstraint(1.0);
        var classPanel = new JPanel(new GridBagLayout());

        var fieldsGroupedBySuperClass = getFieldsGroupedBySuperClass(baseClass);

        if (!fieldsGroupedBySuperClass.isEmpty()) {
            var semanticList = getFieldsGroupedBySemanticMeaning( fieldsGroupedBySuperClass.get(baseClass));
            var groups = semanticList.keySet();
            for (var group : groups) {
                var tableModel = new AttributeTableModel(semanticList.get(group));
                var table = new JAttributeTable(this,tableModel,model,object);
                this.tablesListeners.add(table);
                if (groupIsUnNamed(group)) {
                    classPanel.add(table, gbc);
                } else {//groupHasName
                    var groupPanel = new JCollapsablePanel(group, JCollapsablePanel.Style.GROUP);
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
                field.set(selectedAttributesInstance,value);
                field.setAccessible(false);
                parentView.updateModel(selectedAttributesInstance);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        this.revalidate();
        this.repaint();
    }
}
