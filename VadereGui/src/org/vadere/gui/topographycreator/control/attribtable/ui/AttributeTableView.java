package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.attribtable.Revalidatable;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.ObjectNode;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeAdapter;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class AttributeTableView extends JPanel implements Revalidatable {
    HashMap<Class, AttributeTablePage> pages;
    AttributeTablePage activePage;
    Revalidatable revalidatable;

    public AttributeTableView(Revalidatable revalidatable) {
        super(new BorderLayout());
        this.pages = new HashMap<>();
        this.revalidatable = revalidatable;

    }
    public void selectionChange(@NotNull Object object) {
        this.clear();
        this.activePage = this.pages.get(object.getClass());
        if(this.activePage == null){
            throw new RuntimeException("AttributeTableView did not have a valid page for " + object.getClass()+". The method buildPageFor("+object.getClass()+") probably has not been called before this.");
        }
        this.add(activePage, BorderLayout.NORTH);
        repaintPage();
    }

    private void repaintPage() {
        this.revalidate();
        this.repaint();
    }

    public void updateModel(Object object) throws TreeException, IllegalAccessException {
        activePage.updateModel(object);
    }

    public void revalidateObjectStructure(Object object) {
        if (revalidatable != null)
            revalidatable.revalidateObjectStructure(object);
    }

    public void clear() {
        this.removeAll();
        repaintPage();
    }

    /**
     * creates an ui for the given class with and attaches view model listeners to the model
     * @param clazz
     */
    public void buildPageFor(Class clazz){
        var tree = AttributeTreeModel.parseClassTree(new TreeAdapter(this), null, clazz);
        var page = new AttributeTablePage((ObjectNode) tree,AttributeTablePage.generateHeaderName(clazz),new AttributeTablePage.TableStyler(tree));
        this.pages.put(clazz, page);
    }

    public void buildPageFor(AttributeTreeModel.TreeNode tree){
        var page = new AttributeTablePage((ObjectNode) tree,AttributeTablePage.generateHeaderName(tree.getFieldType()),new AttributeTablePage.TableStyler(tree));
        this.pages.put(tree.getFieldType(), page);
    }
}
