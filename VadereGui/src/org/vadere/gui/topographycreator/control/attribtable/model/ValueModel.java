package org.vadere.gui.topographycreator.control.attribtable.model;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;

import javax.swing.*;
import java.util.ArrayList;
import java.util.function.Function;

public class ValueModel<T> extends AbstractModel<T> {
    ArrayList<T> referenceObject;
    private int index = 0;

    public ValueModel(ArrayList<T> model, Object ownerRef, ViewListener listener) {
        super(model, ownerRef, listener);
    }

    @Override
    protected Function<? super Object, String> keyMapper() {
        return (c) -> String.valueOf(index);

    }

    @Override
    protected AttributeEditor createEditor(T object, JPanel contentPanel) {
        return registry.create(object.getClass(), this, String.valueOf(index), contentPanel);
    }


    @Override
    public void updateModel(String index, Object obj) {
        var idx = Integer.valueOf(index);
        referenceObject.remove(idx - 1);
        referenceObject.add(idx - 1, (T) obj);
        listener.updateModel(referenceObject);

    }

    @Override
    public void updateView(Object obj) {
        referenceObject = (ArrayList<T>) obj;
        var modelSize = getElements().values().size();
        if (referenceObject.size() > modelSize) {
            var i = modelSize;
            while (referenceObject.size() > getElements().values().size()) {
                addElement(referenceObject.get(i));
                i++;
                index++;
            }
        } else if (referenceObject.size() < modelSize) {
            var i = modelSize;
            while (referenceObject.size() < getElements().values().size()) {
                removeElement(String.valueOf(index));
                i--;
                index--;
            }
        }
        var fields = this.getElements();
        var editors = this.getEditors();
        int i = 0;
        for (var fieldName : fields.keySet()) {
            if (fieldName.equals("ADDER")) continue;
            var field = fields.get(fieldName);
            var editor = editors.get(fieldName);
            editor.updateView(referenceObject.get(i));
            i++;
        }
        listener.updateModel(referenceObject);
    }
}
