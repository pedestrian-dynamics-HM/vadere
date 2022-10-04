package org.vadere.gui.topographycreator.control.attribtable.ui;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTreeModel;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeAdapter;
import org.vadere.gui.topographycreator.control.attribtable.tree.TreeException;
import org.vadere.gui.topographycreator.control.attribtable.util.ManualAttributeTableFocus;
import org.vadere.simulator.context.Context;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;

public class AttributeTableView extends JPanel implements ViewListener {
    HashMap<Class, AttributeTablePage> pages;
    private HashMap<Class, ManualAttributeTableFocus> pageFocus;
    AttributeTablePage activePage;
    ViewListener viewListener;

    public AttributeTableView(ViewListener viewListener) {
        super(new BorderLayout());
        this.pages = new HashMap<>();
        this.pageFocus = new HashMap<>();
        this.viewListener = viewListener;

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

    public void viewChanged(Object object) {
        if (viewListener != null)
            viewListener.viewChanged(object);
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
        var page = new AttributeTablePage(tree,AttributeTablePage.generateHeaderName(clazz),new AttributeTablePage.TableStyler(tree));
        var f = new ManualAttributeTableFocus();
        page.applyFocusPolicy(f);
        this.pages.put(clazz, page);
        this.pageFocus.put(clazz, f);
    }

    public void buildPageFor(AttributeTreeModel.TreeNode tree){
        var page = new AttributeTablePage(tree,AttributeTablePage.generateHeaderName(tree.getFieldType()),new AttributeTablePage.TableStyler(tree));
        var f = new ManualAttributeTableFocus();
        page.applyFocusPolicy(f);
        this.pages.put(tree.getFieldType(), page);
        this.pageFocus.put(tree.getFieldType(), f);
    }
}
