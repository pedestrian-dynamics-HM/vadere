package org.vadere.gui.topographycreator.control.celleditor.impl;

import org.vadere.gui.topographycreator.control.JAttributeTable;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.lang.reflect.Field;

/**
 * All TableCellEditors in JAttributeTable need to implement the interface
 * AttributeEditor.
 * setValue() and getValue() are self explainatory.
 * JAttributeTable does add a addChangeListener(..) once instantiated, so
 * the attached objects fields get updated.
 */

public class TextEditCellEditor extends AttributeEditor {
    private JTextField textField;

    private String oldValue;

    public TextEditCellEditor(
            JAttributeTable parent,
            Object fieldOwner,
            Field field,
            TopographyCreatorModel model,
            JPanel unused
    ) {
        super(parent,fieldOwner, field, model,null);
    }

    @Override
    protected void initialize() {
        this.textField = new JTextField();
        this.add(textField);
        ((PlainDocument) this.textField.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                    try {
                        super.replace(fb, offset, length, text, attrs);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                    var document = fb.getDocument();
                    var textBegin = 0;
                    var textEnd = document.getLength();
                    try {
                        var texte = fb.getDocument().getText(textBegin, textEnd);
                    } catch (BadLocationException e) {
                        throw new RuntimeException(e);
                    }
                    updateModel(text);
            }
        });
    }

    @Override
    public void modelChanged(Object value) {
        var text = (String)value;
        if(text!=oldValue) {
            oldValue = text;
            this.textField.setText(text);
        }
    }
}
