package org.vadere.gui.topographycreator.control.attribtable.cells.delegates;

import org.vadere.gui.topographycreator.control.attribtable.tree.AttributeTree;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;

/**
 * @author Ludwig Jaeck
 * This class is a table delegate for editing string fields.
 * To figure out if the UI model changed a DocumentFilter is used.
 */
public class TextEditCellEditor extends AttributeEditor {
    private JTextField textField;

    public TextEditCellEditor(AttributeTree.TreeNode model, JPanel contentPanel) {
        super(model, contentPanel);
    }

    @Override
    protected void initialize() {
        this.textField = new JTextField();
        initializeTextFieldListener();
        this.add(textField);
    }

    private void initializeTextFieldListener() {
        var document = (PlainDocument) this.textField.getDocument();
        document.setDocumentFilter(new MyDocumentFilter());
    }

    @Override
    public void onModelChanged(Object value) {
        this.textField.setText((String) value);

    }

    private class MyDocumentFilter extends DocumentFilter {
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
    }
}
