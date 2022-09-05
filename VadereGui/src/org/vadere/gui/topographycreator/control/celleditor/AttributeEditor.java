package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
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
    protected Attributes fieldOwner;
    private final TopographyCreatorModel model;
    private boolean locked = false;
    protected final NotifyContext ctx = new NotifyContext(this.getClass());

    public AttributeEditor(Attributes fieldOwner, Field field, TopographyCreatorModel model,JPanel contentPanel){
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

    public void updateView(Object value){
        disableNotify();
        modelChanged(value);
        enableNotify();
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
            this.field.set(this.fieldOwner, newValue);
            field.setAccessible(false);

            model.getScenario().updateCurrentStateSerialized();
            model.setElementHasChanged(element);
            model.notifyObservers(ctx);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected TopographyCreatorModel getModel(){
        return this.model;
    }
}