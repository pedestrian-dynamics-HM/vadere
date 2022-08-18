package org.vadere.gui.components.control;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ListDataListener;
import javax.swing.text.*;

import java.util.List;
import java.util.stream.Collectors;

public class JSearchComboBox extends JComboBox<String> {
    final private List<String> comboboxModel;
    private String searchString = "";
    private JTextComponent textComponentProxy;
    private JSearchComboBox comboBoxProxy;

    private class SearchModel<T> implements ComboBoxModel<T> {
        List<T> items;
        T search;

        // yep, we don't use searchString here,because we need a copy of the current state which we store into search
        public SearchModel(List<T> items, T search) {
            this.items = items;
            this.search = search;
        }

        // this method is called by a JComboBox after the user clicks on an item of the popup
        // before updating the text
        @Override
        public void setSelectedItem(Object anItem) {
            // model needs to be replaced to only show the one item searched for
            JComboBox box = (JComboBox) comboBoxProxy;
            box.setModel(new SearchModel<>(List.of(anItem), anItem));
        }
        // this method will be called everytime there is a CaretEvent
        @Override
        public Object getSelectedItem() {
            return search;
        }

        @Override
        public int getSize() {
            return this.items.size();
        }

        @Override
        public T getElementAt(int index) {
            return this.items.get(index);
        }

        @Override
        public void addListDataListener(ListDataListener l) {

        }

        @Override
        public void removeListDataListener(ListDataListener l) {

        }
    }
    // CaretListener is used as a method of detecting a change in the
    // ComboBox JTextComponent. Why? because at the time of implementation
    // CaretListener seemed to be the only Listener for JTextComponent which
    // had updated the text with the last typed key before calling the callback.
    private class CustomCaretListener implements CaretListener {
        @Override
        public void caretUpdate(CaretEvent e) {
            if (!(textComponentProxy.getText().equals(searchString))) {
                searchString = textComponentProxy.getText();
                SwingUtilities.invokeLater(() -> {
                    updateModel(searchString);
                    comboBoxProxy.showPopup();
                });
            }
        }

    }
    public JSearchComboBox(String[] items) {
        super(items);
        this.comboboxModel = List.of(items);
        initComponents();
        initChangeListener();
        clearTextField();
    }
    private void initChangeListener() {

        textComponentProxy.addCaretListener(new CustomCaretListener());
        ((PlainDocument)textComponentProxy.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                // fix for windows
                textComponentProxy.setSelectionStart(0);
                textComponentProxy.setSelectionEnd(0);
                textComponentProxy.setCaretPosition(textComponentProxy.getText().length());
                // end fix
                super.replace(fb, offset, length, text, attrs);
            }
        });
    }
    private void initComponents() {
        this.setEditable(true);
        // used for lazy referencing in nested classes
        this.textComponentProxy = (JTextComponent) this.getEditor().getEditorComponent();
        this.comboBoxProxy = this;
    }
    private void clearTextField() {
        textComponentProxy.setText("");
    }
    private void updateModel(String searchString) {
        // replace with better search algorith if needed
        List<String> array = comboboxModel
                .stream()
                .filter(entry -> {
                    String questioned = entry.toLowerCase();
                    String searchpatt = searchString.toLowerCase();
                    return questioned.contains(searchpatt);

                }).collect(Collectors.toList());
        ComboBoxModel<String> newModel = new SearchModel<>(array, searchString);
        this.setModel(newModel);

    }
}
