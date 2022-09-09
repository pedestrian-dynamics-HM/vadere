package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;
import java.awt.*;

public abstract class AttributeEditor extends JPanel implements ViewListener {

    protected final AbstractModel model;
    protected final String id;
    protected JPanel contentPanel;

    Object oldValue;
    private boolean locked = false;


    public AttributeEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(new BorderLayout());
        this.model = parent;
        this.id = id;
        this.contentPanel = contentPanel;
        disableNotify();
        initialize();
        enableNotify();
    }

    protected abstract void initialize();

    protected abstract void modelChanged(Object value);

    public void updateView(Object fieldValue){
        if(oldValue!=fieldValue) {
            oldValue = fieldValue;
            disableNotify();
            modelChanged(fieldValue);
            enableNotify();
        }
    }
    private void disableNotify(){
        this.locked = true;
    }

    private void enableNotify(){
        this.locked = false;
    }

    private boolean canUpdate(){
        return !locked;
    }

    public void updateModel(Object value) {
        if (canUpdate()) updateModelFromValue(value);
    }

    private void updateModelFromValue(Object newValue) {
        this.model.updateModel(this.id, newValue);
    }

}