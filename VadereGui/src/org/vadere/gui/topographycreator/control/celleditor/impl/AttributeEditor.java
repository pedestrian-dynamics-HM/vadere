package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.scenario.AttributesSource;
import org.vadere.state.attributes.spawner.AttributesSpawner;
import org.vadere.util.Attributes;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;

public abstract class AttributeEditor extends JPanel {

    JPanel contentPanel;

    protected final Field field;
    protected Attributes fieldOwner;
    private final TopographyCreatorModel model;
    private boolean locked = false;


    protected JAttributeTable parentTranslator;

    Object oldValue;
    public AttributeEditor(JAttributeTable parentTranslator, Attributes fieldOwner, Field field, TopographyCreatorModel model, JPanel contentPanel){
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
            printDebug(newValue);
            parentTranslator.updateModel(field,newValue);
            ///?????
            //model.getScenario().updateCurrentStateSerialized();
            //model.setElementHasChanged(element);
            //model.notifyObservers(ctx);


            ///????

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        });
    }

    private void printDebug(Object newValue) {
        var element = model.getSelectedElement();
        var elementClassName = element.getClass().getSimpleName();
        var attribs = element.getAttributes();
        var attribsName = attribs.getClass().getSimpleName();
        System.out.print("Model: "+elementClassName+"@"+element.hashCode()+"->");
        System.out.print(attribsName+"@"+attribs.hashCode()+"->");
        if(newValue instanceof AttributesSpawner) {
            var spawnAttribs = ((AttributesSource) attribs).getSpawnerAttributes();
            if(spawnAttribs!=null) {
                var spawnAttribsName = spawnAttribs.getClass().getName();
                System.out.print(spawnAttribsName + "@" + spawnAttribs + "->");
                var distAttribs = spawnAttribs.getDistributionAttributes();
                if(distAttribs!=null) {
                    var distAttribName = distAttribs.getClass().getName();
                    System.out.println(distAttribName + "@" + distAttribs.hashCode());
                }else{
                    System.out.println(distAttribs.hashCode());
                }
            }else{
                System.out.println(spawnAttribs);
            }

        }
        if(newValue != null)
            System.out.print("Table: "+newValue.getClass().getSimpleName()+"@"+newValue.hashCode()+"->");
        /*if(newValue!=null)
            System.out.println(this.fieldOwner.getClass().getSimpleName()+"@"+this.fieldOwner.hashCode()+""+this.field.getName()+" " + newValue.getClass().getSimpleName()+ "@" + newValue.hashCode());
        else
            System.out.println(this.fieldOwner.getClass().getSimpleName()+"@"+this.fieldOwner.hashCode()+""+this.field.getName()+" " + newValue);
        */
    }

    protected TopographyCreatorModel getModel(){
        return this.model;
    }

    public void setFieldOwner(Attributes fieldOwner) {
        this.fieldOwner = fieldOwner;
    }
}