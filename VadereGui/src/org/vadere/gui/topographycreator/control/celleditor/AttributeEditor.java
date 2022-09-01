package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {
    private boolean externUpdate = false;
    private Object oldValue = null;

    private Field field;
    protected TopographyCreatorModel model;

    private Object attached;

    public  AttributeEditor(Object attached, Field field, TopographyCreatorModel model){
        super(new BorderLayout());
        this.attached = attached;
        this.field = field;
        this.model = model;
    }
    public void updateValueFromModel(Object value){
        this.externUpdate  = true;
        this.oldValue = value;
    }

    protected void updateModelFromValue(Object newValue){
        /*if (externUpdate){
            this.externUpdate = false;
            return;
        }*/
        if (oldValue == newValue){
            return;
        }
        try {
            field.setAccessible(true);
            this.field.set(this.attached, newValue);
            field.setAccessible(false);
            model.getScenario().updateCurrentStateSerialized();
            model.notifyObservers();
            System.out.println(attached);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        externUpdate = false;
        oldValue = newValue;
    }

}