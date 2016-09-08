package org.vadere.gui.projectview.view;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.ModelDefinition;
import org.vadere.state.scenario.Topography;
import org.vadere.util.io.IOUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Shows text like the JSON formatted attributes.
 * 
 * 
 */
public class TextView extends JPanel {

	private static Logger logger = LogManager.getLogger(TextView.class);

	private AttributeType attributeType;
	private String default_folder;
	private String default_resource;

	private JPanel panelTop = new JPanel();

	private static final long serialVersionUID = 3975758744810301970L;
	private ScenarioRunManager currentScenario;

	private JsonValidIndicator jsonValidIndicator;

	private AbstractButton btnLoadFromFile;

	private boolean isEditable;

	private DocumentListener documentListener;


	private JTextArea txtrTextfiletextarea;
	private ActionListener saveToFileActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String path = IOUtils.chooseJSONFileSave(Messages.getString("TextFileView.btnSaveToFile.text"),
					Preferences.userNodeForPackage(VadereApplication.class).get(default_resource, default_folder));

			if (path == null)
				return;

			try {
				IOUtils.writeTextFile(path.endsWith(".json") ? path : path + ".json", txtrTextfiletextarea.getText());
			} catch (IOException e1) {
				IOUtils.errorBox(e1.getLocalizedMessage(), Messages.getString("SaveFileErrorMessage.title"));
				logger.error(e1);
			}
		}
	};

	private ActionListener loadFromFileActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			FileFilter filter = new FileNameExtensionFilter("JSON file", "json");
			String path = IOUtils.chooseFile("Choose file...",
					Preferences.userNodeForPackage(VadereApplication.class).get(default_resource, default_folder),
					filter);

			if (path == null)
				return;

			try {
				String content = IOUtils.readTextFile(path);
				txtrTextfiletextarea.setText(content);
			} catch (IOException e) {
				logger.error(e);
			}
		}
	};

	/**
	 * Create the panel.
	 */
	public TextView(String default_folder, String default_resource, final AttributeType attributeType) {
		this.default_folder = default_folder;
		this.default_resource = default_resource;
		this.attributeType = attributeType;
		setLayout(new BorderLayout(0, 0));

		panelTop = new JPanel();
		add(panelTop, BorderLayout.NORTH);
		panelTop.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		JButton btnSaveToFile = new JButton(Messages.getString("TextFileView.btnSaveToFile.text"));
		btnSaveToFile.addActionListener(saveToFileActionListener);
		btnSaveToFile.setIcon(new ImageIcon(TextView.class
				.getResource("/javax/swing/plaf/metal/icons/ocean/floppy.gif")));
		panelTop.add(btnSaveToFile);

		btnLoadFromFile = new JButton(Messages.getString("TextView.btnLoadFromFile.text"));
		btnLoadFromFile.addActionListener(loadFromFileActionListener);
		btnLoadFromFile.setIcon(new ImageIcon(
				TextView.class.getResource("/javax/swing/plaf/metal/icons/ocean/floppy.gif")));
		panelTop.add(btnLoadFromFile);

		jsonValidIndicator = new JsonValidIndicator();
		panelTop.add(jsonValidIndicator);

		JScrollPane scrollPane = new JScrollPane();
		add(scrollPane, BorderLayout.CENTER);

		RSyntaxTextArea textAreaLocal = new RSyntaxTextArea();
		textAreaLocal.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);

		// set other color theme for text area...
		InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
		try {
			Theme syntaxTheme = Theme.load(in);
			syntaxTheme.apply(textAreaLocal);
		} catch (IOException e) {
			logger.error(e);
		}

		txtrTextfiletextarea = textAreaLocal;

		scrollPane.setViewportView(txtrTextfiletextarea);
		txtrTextfiletextarea.setText(Messages.getString("TextFileView.txtrTextfiletextarea.text"));

		documentListener = new DocumentListener() {
			@Override
			public void changedUpdate(DocumentEvent e) {
				setScenarioContent();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setScenarioContent();
			}

			@Override
			public void insertUpdate(DocumentEvent e) {
				setScenarioContent();
			}

			public void setScenarioContent() {
				if (isEditable) {
					String json = txtrTextfiletextarea.getText(); // TODO [priority=medium] [task=bugfix] this can sometimes give the wrong text if an integer is added at the end of
																  // random-seed in simulation tab, very weird, investigate...
					if (json.length() == 0)
						return;

					try {
						switch (attributeType) {
							case MODEL:
								ModelDefinition modelDefinition = JsonConverter.deserializeModelDefinition(json);
								currentScenario.getScenarioStore().mainModel = modelDefinition.getMainModel();
								currentScenario.setAttributesModel(modelDefinition.getAttributesList());
								break;
							case SIMULATION:
								currentScenario.setAttributesSimulation(JsonConverter.deserializeAttributesSimulation(json));
								break;
							case OUTPUTPROCESSOR:
								currentScenario.setDataProcessingJsonManager(DataProcessingJsonManager.deserialize(json));
								break;
							case TOPOGRAPHY:
								currentScenario.setTopography(JsonConverter.deserializeTopography(json));
								break;
						}
						currentScenario.updateCurrentStateSerialized();
						ScenarioJPanel.removeJsonParsingErrorMsg();
						ProjectView.getMainWindow().refreshScenarioNames();
						jsonValidIndicator.setValid();
					} catch (Exception e) {
						ScenarioJPanel.setActiveJsonParsingErrorMsg(attributeType.name() + " tab:\n" + e.getMessage());
						jsonValidIndicator.setInvalid();
					}
				}
			}
		};

		this.attributeType = attributeType;
		jsonValidIndicator.setValid();
	}

	public void setVadereScenario(ScenarioRunManager scenario) { // in order to avoid passing the exception upwards. might not be the best solution
		try {
			setVadereScenarioThrows(scenario);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private void setVadereScenarioThrows(ScenarioRunManager scenario) throws JsonProcessingException {
		this.currentScenario = scenario;

		switch (attributeType) {
			case MODEL:
				this.txtrTextfiletextarea.setText(JsonConverter.serializeMainModelAttributesModelBundle(
						scenario.getSortedAttributesMode(), scenario.getScenarioStore().mainModel));
				break;
			case SIMULATION:
				this.txtrTextfiletextarea
						.setText(JsonConverter.serializeAttributesSimulation(scenario.getAttributesSimulation()));
				break;
			case OUTPUTPROCESSOR:
				this.txtrTextfiletextarea.setText(scenario.getDataProcessingJsonManager().serialize());
				break;

			case TOPOGRAPHY:
				Topography topography = scenario.getTopography().clone();
				topography.removeBoundary();
				this.txtrTextfiletextarea.setText(JsonConverter.serializeTopography(topography));
				break;
		}
		this.txtrTextfiletextarea.setCaretPosition(0);
	}

	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
		btnLoadFromFile.setEnabled(isEditable);
		txtrTextfiletextarea.setEnabled(isEditable);
		if (isEditable) {
			txtrTextfiletextarea.setBackground(Color.WHITE);
			txtrTextfiletextarea.getDocument().addDocumentListener(documentListener);
		} else {
			txtrTextfiletextarea.setBackground(Color.LIGHT_GRAY);
			txtrTextfiletextarea.getDocument().removeDocumentListener(documentListener);
		}
	}

	public JPanel getPanelTop() {
		return panelTop;
	}

	public void setText(String text) {
		txtrTextfiletextarea.setText(text);
	}

	public String getText() {
		return txtrTextfiletextarea.getText();
	}

	public void insertAtCursor(String text) {
		txtrTextfiletextarea.insert(text, txtrTextfiletextarea.getCaretPosition());
	}
}
