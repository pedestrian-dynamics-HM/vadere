package org.vadere.gui.topographycreator.view;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class AttributeTableView extends JPanel{

    private final TopographyCreatorModel defaultModel;
    HashMap<Class, AttributeTablePage> editorPages;

    private Object selectedAttributesInstance;

    AttributeTablePage activePage;

    private final AttributeTranslator parentModelTranslator;

    public AttributeTableView(AttributeTranslator parent,final TopographyCreatorModel defaultModel){
        super(new BorderLayout());
        this.parentModelTranslator = parent;
        this.defaultModel = defaultModel;
        this.editorPages = new HashMap<>();
    }

    public void selectionChange(Object object) {
        this.selectedAttributesInstance = object;
        this.removeAll();
        if (object != null) {

            if (!editorPages.containsKey(object.getClass())) {
                var archetype = object.getClass();
                var attributePage = new AttributeTablePage(this, archetype, defaultModel);
                this.editorPages.put(object.getClass(), attributePage);
            }
            activePage = editorPages.get(object.getClass());
            activePage.updateView(object);
            this.add(activePage, BorderLayout.NORTH);

        } else {
            this.removeAll();
        }
        revalidate();
        repaint();
    }

    public void updateView(Object object) {
        if (activePage != null)
            activePage.updateView(object);
    }

    public void updateModel(Object selectedAttributesInstance) {
        parentModelTranslator.updateModel(selectedAttributesInstance);
        this.revalidate();
        this.repaint();
    }
}
