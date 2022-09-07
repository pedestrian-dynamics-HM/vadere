package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {

    JPanel contentPanel;

    protected final Field field;
    protected Object fieldOwner;
    private final TopographyCreatorModel model;
    private boolean locked = false;


    protected JAttributeTable parentTranslator;

    Object oldValue;

    public AttributeEditor(JAttributeTable parentTranslator, Object fieldOwner, Field field, TopographyCreatorModel model, JPanel contentPanel) {
        super(new BorderLayout());
        this.parentTranslator = parentTranslator;
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
        SwingUtilities.invokeLater(()->{
        try {

            var element = model.getSelectedElement();
            field.setAccessible(true);
            this.field.set(this.fieldOwner, newValue);
            field.setAccessible(false);
            parentTranslator.updateModel(field,newValue);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        });
    }

    protected TopographyCreatorModel getModel(){
        return this.model;
    }

    public void setFieldOwner(Object fieldOwner) {
        this.fieldOwner = fieldOwner;
    }
}