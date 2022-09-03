package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.AttributesAttached;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {
    private final Field field;
    private Attributes fieldOwner;
    private final TopographyCreatorModel model;


    protected final NotifyContext ctx = new NotifyContext(this.getClass());

    public  AttributeEditor(Attributes fieldOwner, Field field, TopographyCreatorModel model){
        super(new BorderLayout());
        this.fieldOwner = fieldOwner;
        this.field = field;
        this.model = model;
    }
    public void updateValueFromModel(Object value){
    }
    protected void updateModelFromValue(Object newValue){
        try {
            field.setAccessible(true);
            this.field.set(this.fieldOwner, newValue);
            field.setAccessible(false);

            model.getScenario().updateCurrentStateSerialized();
            model.notifyObservers(ctx);

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    protected TopographyCreatorModel getModel(){
        return this.model;
    }
}