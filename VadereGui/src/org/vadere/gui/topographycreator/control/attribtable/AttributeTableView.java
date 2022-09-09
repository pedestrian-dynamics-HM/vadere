package org.vadere.gui.topographycreator.control.attribtable;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class AttributeTableView extends JPanel implements ViewListener {

    HashMap<Class, AttributeTablePage> editorPages;

    private Object selectedAttributesInstance;

    AttributeTablePage activePage;

    private final ViewListener parent;

    public AttributeTableView(ViewListener parent) {
        super(new BorderLayout());
        this.parent = parent;
        this.editorPages = new HashMap<>();
    }

    public void selectionChange(Object object) {
        this.selectedAttributesInstance = object;
        this.removeAll();
        if (object != null) {

            if (!editorPages.containsKey(object.getClass())) {
                var archetype = object.getClass();
                var attributePage = new AttributeTablePage(this, archetype);
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
        parent.updateModel(selectedAttributesInstance);
        this.revalidate();
        this.repaint();
    }
}
