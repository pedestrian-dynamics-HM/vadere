package org.vadere.gui.topographycreator.control.celleditor;

import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.util.Attributes;
import org.vadere.util.observer.NotifyContext;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {
    protected final Field field;
    protected Attributes fieldOwner;
    private final TopographyCreatorModel model;
    private boolean locked = false;
    protected final NotifyContext ctx = new NotifyContext(this.getClass());

    public  AttributeEditor(Attributes fieldOwner, Field field, TopographyCreatorModel model){
        super(new BorderLayout());
        this.fieldOwner = fieldOwner;
        this.field = field;
        this.model = model;
    }
    public void updateValueFromModel(Object value){this.locked = true;};

    protected void updateModelFromValue(Object newValue){
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
    protected void ifNotUpdatedFromOutside(Runnable runner){
        if(!this.locked){
            runner.run();
        }
        this.locked = false;
    }

    protected TopographyCreatorModel getModel(){
        return this.model;
    }
}