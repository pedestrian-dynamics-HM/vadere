package org.vadere.gui.topographycreator.control.attribtable.model;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ValueModel<T> extends AbstractModel<T> {
    ArrayList<String> listIndice;
    ArrayList<T> referenceObject;
    private int index = 0;

    public ValueModel(ArrayList model, Object ownerRef, ViewListener listener) {
        super(model, ownerRef, listener);
        model.forEach(e -> listIndice.add(keyMapper().apply(e)));
    }

    @Override
    protected Function<? super Object, String> keyMapper() {
        return (c) -> String.valueOf(index++);
    }

    @Override
    public void updateModel(String index, Object obj) {
    }

    @Override
    public void updateView(Object obj) {
        referenceObject = (ArrayList<T>) obj;
        if (referenceObject.size() > listIndice.size()) {
            var i = listIndice.size();
            while (referenceObject.size() > listIndice.size()) {
                addElement(referenceObject.get(i));
                i++;
                index++;
            }
        } else if (referenceObject.size() < listIndice.size()) {
            var i = listIndice.size();
            while (referenceObject.size() < listIndice.size()) {
                removeElement(listIndice.get(listIndice.size() - 1));
                i--;
                index--;
            }
        }
        var fields = this.getElements();
        var editors = this.getEditors();
        int i = 0;
        for (var fieldName : fields.keySet()) {
            var field = fields.get(fieldName);
            var editor = editors.get(fieldName);
            editor.updateView(referenceObject.get(i));
        }
    }

    @Override
    protected void createEditors(Map model, HashMap attributeEditors, HashMap editorContentPanels) {
        var registry = EditorRegistry.getInstance();
        for (var field : model.keySet()) {
            var subPanel = new JPanel(new GridBagLayout());
            var clazz = ((ParameterizedType) this.getClass()
                    .getGenericSuperclass())
                    .getActualTypeArguments()[0]
                    .getClass();
            var editor = registry.create(clazz, this, (String) field, subPanel);
            editorContentPanels.put(field, subPanel);
            attributeEditors.put(field, editor);
            subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        }
    }
}
