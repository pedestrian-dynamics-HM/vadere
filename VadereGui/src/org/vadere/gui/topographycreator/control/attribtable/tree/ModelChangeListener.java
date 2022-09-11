package org.vadere.gui.topographycreator.control.attribtable.tree;

public abstract class ModelChangeListener {
    String path;
    AttributeTree tree;

    ModelChangeListener(String nodePath) {
        this.path = nodePath;
    }

    public void setTree(AttributeTree tree) {
        this.tree = tree;
    }

    public abstract void modelChanged(Object obj);
}