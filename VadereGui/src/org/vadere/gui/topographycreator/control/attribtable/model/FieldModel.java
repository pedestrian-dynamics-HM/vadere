package org.vadere.gui.topographycreator.control.attribtable.model;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.function.Function;

public class FieldModel extends AbstractModel<Field> {

    public FieldModel(ArrayList<Field> model, Object ownerRef, ViewListener listener) {
        super(model, ownerRef, listener);
    }

    @Override
    protected Function<? super Object, String> keyMapper() {
        return (c) -> ((Field) c).getName();
    }

    @Override
    protected AttributeEditor createEditor(Field object, JPanel contentPanel) {
        return registry.create(object.getType(), this, object.getName(), contentPanel);
    }


    @Override
    public void updateModel(String index, Object obj) {
        //update the requested field
        Field field = getElement(index);
        field.setAccessible(true);
        try {
            field.set(ownerRef, obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        field.setAccessible(false);
        // return the updated object back up the hierachy
        listener.updateModel(ownerRef);
    }

    @Override
    public void updateView(Object obj) {
        this.ownerRef = obj;
        var fields = this.getElements();
        var editors = this.getEditors();
        for (var fieldName : fields.keySet()) {
            var field = fields.get(fieldName);
            var editor = editors.get(fieldName);
            Object val = null;
            try {
                field.setAccessible(true);
                val = field.get(obj);
                field.setAccessible(false);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            editor.updateView(val);
        }
    }


}
