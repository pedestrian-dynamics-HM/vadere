package org.vadere.gui.topographycreator.control.attribtable.model;

import org.vadere.gui.topographycreator.control.attribtable.ViewListener;
import org.vadere.gui.topographycreator.control.attribtable.cells.delegates.AttributeEditor;
import org.vadere.gui.topographycreator.control.attribtable.cells.editors.EditorRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractModel<T> {
    private final HashMap<String, T> modelElements;
    private final HashMap<String, AttributeEditor> attributeEditors;
    private final HashMap<String, JPanel> editorContentPanels;
    private final Function<? super Object, String> keyMapper;

    protected ViewListener listener;
    protected Object ownerRef;

    protected static EditorRegistry registry = EditorRegistry.getInstance();
    public AbstractModel(ArrayList<T> model, Object ownerRef, ViewListener listener) {
        keyMapper = keyMapper();
        this.ownerRef = ownerRef;
        this.attributeEditors = new HashMap<>();
        this.editorContentPanels = new HashMap<>();
        this.modelElements = new HashMap<>();
        this.listener = listener;
        if (model != null) {
            for (var val : model) {
                addElement(val);
            }
        }
    }

    protected abstract Function<? super Object, String> keyMapper();

    protected abstract AttributeEditor createEditor(T object, JPanel contentPanel);

    public T getElement(String id) {
        return this.modelElements.get(id);
    }

    public Map<String, T> getElements() {
        return Collections.unmodifiableMap(this.modelElements);
    }

    public Map<String, AttributeEditor> getEditors() {
        return Collections.unmodifiableMap(this.attributeEditors);
    }

    public Map<String, JPanel> getContentPanels() {
        return Collections.unmodifiableMap(this.editorContentPanels);
    }

    public abstract void updateModel(String index, Object obj);

    public abstract void updateView(Object obj);

    public void addElement(T element) {
        var key = keyMapper.apply(element);
        modelElements.put(key, element);

        var subPanel = new JPanel(new GridBagLayout());
        subPanel.setBackground(UIManager.getColor("Table.selectionBackground").brighter());
        var editor = createEditor(element, subPanel);

        attributeEditors.put(key, editor);
        editorContentPanels.put(key, subPanel);
    }

    public void removeElement(String mappedKey) {
        this.modelElements.remove(mappedKey);
        this.attributeEditors.remove(mappedKey);
        this.editorContentPanels.remove(mappedKey);
    }
}