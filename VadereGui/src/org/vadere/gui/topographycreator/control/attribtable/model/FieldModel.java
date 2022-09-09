package org.vadere.gui.topographycreator.control.attribtable.model;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    protected void createEditors(Map<String, Field> model, HashMap<String, AttributeEditor> attributeEditors, HashMap<String, JPanel> editorContentPanels) {
        var registry = EditorRegistry.getInstance();
        for (var field : model.keySet()) {
            var subPanel = new JPanel(new GridBagLayout());
            var editor = registry.create(model.get(field).getType(), this, model.get(field).getName(), subPanel);
            editorContentPanels.put(field, subPanel);
            attributeEditors.put(field, editor);
            subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        }
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
