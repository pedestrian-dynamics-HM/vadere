package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.model.AbstractModel;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * All TableCellEditors in JAttributeTable need to implement the interface
 * AttributeEditor.
 * setValue() and getValue() are self explainatory.
 * JAttributeTable does add a addChangeListener(..) once instantiated, so
 * the attached objects fields get updated.
 */
public class TextEditCellEditor extends AttributeEditor {
    private JTextField textField;

    public TextEditCellEditor(AbstractModel parent, String id, JPanel contentPanel) {
        super(parent, id, contentPanel);
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
        this.textField.setText((String) value);
    }
}
