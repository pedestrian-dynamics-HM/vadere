package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {



    @FunctionalInterface interface ValueProvider{
        Object value();
    }

    JPanel contentPanel;

    protected final Field field;
    protected Field fieldOwner;
    private final TopographyCreatorModel model;
    private boolean locked = false;
    protected final NotifyContext ctx = new NotifyContext(this.getClass());

    protected   Object parent;
    Object oldValue;
    public AttributeEditor(Field fieldOwner, Field field, TopographyCreatorModel model,JPanel contentPanel){
        super(new BorderLayout());
        this.fieldOwner = fieldOwner;
        this.field = field;
        this.model = model;
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

    public void setParent(Object parent) {
        this.parent = parent;
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

    protected void updateModel(Object value){
        if(canUpdate())updateModelFromValue(value);
    }
    private void updateModelFromValue(Object newValue){
        try {

            var element = model.getSelectedElement();

            field.setAccessible(true);
            this.field.set(this.fieldOwner.get(parent), newValue);
            updateParent(this.fieldOwner);
            //this.field.set(element.getAttributes(),newValue);
            field.setAccessible(false);
            printDebug(newValue);
            model.getScenario().updateCurrentStateSerialized();
            model.setElementHasChanged(element);
            model.notifyObservers(ctx);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void updateParent(Field fieldOwner) {
        //parent.
    }

    private void printDebug(Object newValue) {
        System.out.println(model.getSelectedElement());
        System.out.println(model.getSelectedElement().getAttributes());
        if(newValue!=null)
            System.out.println(this.fieldOwner.getClass().getSimpleName()+"@"+this.fieldOwner.hashCode()+""+this.field.getName()+" " + newValue.getClass().getSimpleName()+ "@" + newValue.hashCode());
        else
            System.out.println(this.fieldOwner.getClass().getSimpleName()+"@"+this.fieldOwner.hashCode()+""+this.field.getName()+" " + newValue);
    }

    public void setAttached(Field attachedObject) {
        this.fieldOwner = attachedObject;
    }
    protected TopographyCreatorModel getModel(){
        return this.model;
    }
}