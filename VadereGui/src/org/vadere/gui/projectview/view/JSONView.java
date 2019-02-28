package org.vadere.gui.projectview.view;


import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.*;
import javax.swing.event.DocumentListener;

public class JSONView extends JPanel {
	private static final long serialVersionUID = -5147644809515076284L;
	private static Logger logger = Logger.getLogger(JSONView.class);
	private JTextArea txtrTextfiletextarea;
	private JLabel lbljsonvalid;
	private JLabel lbljsoninvalid;
	private final boolean jsonLabel;

	public JSONView(final boolean jsonLabel) {
		this.jsonLabel = jsonLabel;
		setLayout(new BorderLayout(0, 0));

		RSyntaxTextArea textAreaLocal = new RSyntaxTextArea();
		textAreaLocal.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);

		// set other color theme for text area...
		InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
		try {
			Theme syntaxTheme = Theme.load(in);
			syntaxTheme.apply(textAreaLocal);
		} catch (IOException e) {
			logger.error("could not apply theme: " + e.getMessage());
		}

		JPanel panelTop = new JPanel();
		add(panelTop, BorderLayout.NORTH);
		panelTop.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		txtrTextfiletextarea = textAreaLocal;
		JScrollPane scrollPane = new JScrollPane(txtrTextfiletextarea);
		add(scrollPane, BorderLayout.CENTER);

		lbljsoninvalid = new JLabel(Messages.getString("TextView.lbljsoninvalid.text"));
		lbljsoninvalid.setIcon(new ImageIcon(Resources.class.getResource("/icons/Error.gif")));
		lbljsonvalid = new JLabel(Messages.getString("TextView.lbljsonvalid.text"));
		lbljsonvalid.setIcon(new ImageIcon(Resources.class.getResource("/icons/Inform.gif")));

		panelTop.add(lbljsoninvalid);
		panelTop.add(lbljsonvalid);
		lbljsoninvalid.setVisible(false);
		if (!jsonLabel) {
			lbljsoninvalid.setVisible(false);
			lbljsonvalid.setVisible(false);
		}
		scrollPane.setViewportView(txtrTextfiletextarea);
	}

	public JTextArea getTextArea() {
		return this.txtrTextfiletextarea;
	}

	public void setEditable(boolean isEditable) {
		txtrTextfiletextarea.setEditable(isEditable);
	}

	public void addDocumentListener(final DocumentListener listener) {
		txtrTextfiletextarea.getDocument().addDocumentListener(listener);
	}

	public void setJsonValid(boolean valid) {
		if (jsonLabel) {
			lbljsoninvalid.setVisible(!valid);
			lbljsonvalid.setVisible(valid);
		}
	}

}
