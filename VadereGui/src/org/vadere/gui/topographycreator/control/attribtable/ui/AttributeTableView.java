package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeAdapter;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class AttributeTableView extends JPanel implements Revalidatable {
    HashMap<Class, AttributeTablePage> attributePages;
    AttributeTablePage activePage;
    Revalidatable revalidatable;

    public AttributeTableView(Revalidatable revalidatable) {
        super(new BorderLayout());
        this.attributePages = new HashMap<>();
        this.revalidatable = revalidatable;
    }

    public void selectionChange(Object object) {
        if (object == null) {
            return;
        }
        setVisible(true);
        var objectClass = object.getClass();
        createClassPageIfNotExisting(objectClass);
        setClassPageActive(objectClass);
        updateClassPageModel(object);
        repaintPage();
    }

    private void repaintPage() {
        this.add(activePage, BorderLayout.NORTH);
        this.revalidate();
        this.repaint();
    }

    private void updateClassPageModel(Object object) {
        try {
            activePage.updateModel(object);
        } catch (TreeException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void createClassPageIfNotExisting(Class clazz) {
        if (!attributePages.containsKey(clazz)) {
            var tree = AttributeTree.parseClassTree(null, null, clazz);
            tree.setParent(new TreeAdapter(this));
            var attributePage = new AttributeTablePage(tree);
            this.attributePages.put(clazz, attributePage);
        }
    }

    public void setClassPageActive(Class clazz) {
        activePage = attributePages.get(clazz);
    }

    public void updateModel(Object object) throws TreeException, IllegalAccessException {
        activePage.updateModel(object);
    }

    public void revalidateObjectStructure(Object object) {
        if (revalidatable != null)
            revalidatable.revalidateObjectStructure(object);
    }

    public AttributeTablePage getActivePage() {
        return activePage;
    }

    public void clear() {
        this.removeAll();
    }
}
