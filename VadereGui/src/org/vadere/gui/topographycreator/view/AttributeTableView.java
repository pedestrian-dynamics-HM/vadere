package org.vadere.gui.topographycreator.view;

import org.vadere.gui.components.view.ISelectScenarioElementListener;
import org.vadere.gui.topographycreator.control.AttributeHelpView;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.Attributes;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.HashMap;

import static org.vadere.gui.topographycreator.utils.Layouts.initGridBagConstraint;

public class AttributeTableView extends JPanel{

    private final TopographyCreatorModel defaultModel;
    HashMap<Class, AttributeTablePage> editorPages;

    AttributeTablePage activePage;

    public AttributeTableView(final TopographyCreatorModel defaultModel){
        super(new BorderLayout());
        this.defaultModel = defaultModel;
        this.editorPages = new HashMap<>();
    }

    public void selectionChange(Object parent,Field field) {
        this.removeAll();
        if(parent != null) {

            if (!editorPages.containsKey(parent.getClass())) {
                var attributePage = new AttributeTablePage(field.getType(), defaultModel);
                this.editorPages.put(parent.getClass(), attributePage);
            }
            activePage = editorPages.get(parent.getClass());
            activePage.updateView(parent,field);
            this.add(activePage,BorderLayout.NORTH);
            revalidate();
            repaint();
        }
    }
    public  void updateView(Object parent,Field attributes){
        if(activePage!= null)
            activePage.updateView(parent,attributes);
    }
}
