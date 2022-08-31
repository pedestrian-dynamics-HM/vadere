package org.vadere.gui.projectview.view;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.apache.commons.configuration2.Configuration;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.HelpTextView;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.projectview.model.IScenarioChecker;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.DataProcessingJsonManager;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.attributes.ModelDefinition;
import org.vadere.state.psychology.perception.json.StimulusInfoStore;
import org.vadere.state.psychology.perception.presettings.StimulusPresettings;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;

/**
 * Shows text like the JSON formatted attributes.
 *
 *
 */
public class TextView extends JPanel implements IJsonView {

	private static Logger logger = Logger.getLogger(TextView.class);
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private AttributeType attributeType;
	private String default_resource;


	private JPanel panelTop = new JPanel();

	private static final long serialVersionUID = 3975758744810301970L;
	private Scenario currentScenario;

	private JsonValidIndicator jsonValidIndicator;

	private AbstractButton btnLoadFromFile;

	private boolean isEditable;

	private DocumentListener documentListener;
	private IScenarioChecker scenarioChecker;

	private RSyntaxTextArea textfileTextarea;
	private ActionListener saveToFileActionListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			String path = IOUtils.chooseJSONFileSave(Messages.getString("TextFileView.btnSaveToFile.text"), CONFIG.getString(default_resource));

			if (path == null)
				return;

			try {
				IOUtils.writeTextFile(path.endsWith(".json") ? path : path + ".json", textfileTextarea.getText());
				File file = new File(path);
				VadereConfig.getConfig().setProperty(default_resource, file.getParentFile().getAbsolutePath());
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
			String path = IOUtils.chooseFile(Messages.getString("ChooseFile.text"), CONFIG.getString(default_resource), filter);

			if (path == null)
				return;

			try {
				String content = IOUtils.readTextFile(path);
				File file = new File(path);
				VadereConfig.getConfig().setProperty(default_resource, file.getParentFile().getAbsolutePath());
				textfileTextarea.setText(content);
			} catch (IOException e) {
				logger.error("could not load from file: " + e.getMessage());
			}
		}
	};

	/**
	 * Create the panel.
	 */
	public TextView(@NotNull final String default_resource, final AttributeType attributeType) {
		this.default_resource = default_resource;
		this.attributeType = attributeType;
		setLayout(new BorderLayout(0, 0));
		panelTop = new JPanel();
		add(panelTop, BorderLayout.NORTH);
		panelTop.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		generatePresettingsMenu(attributeType);

		JButton btnSaveToFile = new JButton(Messages.getString("TextFileView.btnSaveToFile.text"));
		btnSaveToFile.addActionListener(saveToFileActionListener);
        btnSaveToFile.setIcon(new ImageIcon(Resources.class.getResource("/icons/floppy.gif")));

		panelTop.add(btnSaveToFile);
		btnLoadFromFile = new JButton(Messages.getString("TextView.btnLoadFromFile.text"));
        btnLoadFromFile.setIcon(new ImageIcon(Resources.class.getResource("/icons/floppy.gif")));
        panelTop.add(btnLoadFromFile);

		jsonValidIndicator = new JsonValidIndicator();
		panelTop.add(jsonValidIndicator);

		RSyntaxTextArea textAreaLocal = new RSyntaxTextArea();
		textAreaLocal.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		textAreaLocal.setCodeFoldingEnabled(true);
		// set other color theme for text area...
		InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
		try {
			Theme syntaxTheme = Theme.load(in);
			syntaxTheme.apply(textAreaLocal);
		} catch (Exception e) {
			logger.error("could not loead theme " + e.getMessage());
		}

		textfileTextarea = textAreaLocal;

		RTextScrollPane sp = new RTextScrollPane(textfileTextarea);
		sp.setFoldIndicatorEnabled(true);
		sp.setLineNumbersEnabled(true);

		add(sp, BorderLayout.CENTER);

		textfileTextarea.setText(Messages.getString("TextFileView.txtrTextfiletextarea.text"));
		AbstractDocument document = (AbstractDocument)textfileTextarea.getDocument();
		document.setDocumentFilter(new DocumentFilter(){
			@Override
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
				Document document = fb.getDocument();
				String removedString = document.getText(offset,length);
				switch (removedString){
					case "\"":{
						checkRemoveEmptyQuotations(fb, offset, length);
						break;
					}
					case "[":{
						checkRemoveEmptySquareBrackets(fb, offset, length);
						break;
					}
					case "{":{
						checkRemoveEmptySwirlyBrackets(fb, offset, length);
						break;
					}
					default: {
						super.remove(fb, offset, length);
					}
				}


			}
			private void checkRemoveEmptyQuotations(FilterBypass fb, int offset, int length) throws BadLocationException {
				String postChar = document.getText(offset +1, length);
				if (postChar.equals("\"")){
					super.remove(fb, offset, length +1);
				}else {
					super.remove(fb, offset, length);
				}
			}

			private void checkRemoveEmptySquareBrackets(FilterBypass fb, int offset, int length) throws BadLocationException {
				String postChar = document.getText(offset +1, length);
				if (postChar.equals("]")){
					super.remove(fb, offset, length +1);
				}else {
					super.remove(fb, offset, length);
				}
			}

			private void checkRemoveEmptySwirlyBrackets(FilterBypass fb, int offset, int length) throws BadLocationException {
				String postChar = document.getText(offset +1, length);
				if (postChar.equals("}")){
					super.remove(fb, offset, length +1);
				}else {
					super.remove(fb, offset, length);
				}
			}

		});
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
				String insertedText = getInsertedText(e);
				switch (insertedText){
					case "\"": {
						checkInsert2ndQuotationMark(e);
						break;
					}
					case "{": {
						checkInsert2ndSwirlyBracket(e);
						break;
					}
					case "[": {
						checkInsert2ndSquareBracket(e);
						break;
					}
				}
				setScenarioContent();
			}

			public void setScenarioContent() {
				if (isEditable) {
					String json = textfileTextarea.getText(); // TODO [priority=medium] [task=bugfix] this can sometimes give the wrong text if an integer is added at the end of
																  // random-seed in simulation tab, very weird, investigate...
					if (json.length() == 0)
						return;

					try {
						switch (attributeType) {
							case MODEL:
								ModelDefinition modelDefinition = JsonConverter.deserializeModelDefinition(json);
								currentScenario.getScenarioStore().setMainModel(modelDefinition.getMainModel());
								currentScenario.setAttributesModel(modelDefinition.getAttributesList());
								break;
							case SIMULATION:
								currentScenario
										.setAttributesSimulation(StateJsonConverter.deserializeAttributesSimulation(json));
								break;
							case PSYCHOLOGY:
								currentScenario
										.setAttributesPsychology(StateJsonConverter.deserializeAttributesPsychology(json));
								break;
							case OUTPUTPROCESSOR:
								currentScenario.setDataProcessingJsonManager(DataProcessingJsonManager.deserialize(json));
								break;
							case TOPOGRAPHY:
								currentScenario.setTopography(StateJsonConverter.deserializeTopography(json));
								break;
							case PERCEPTION:
								StimulusInfoStore stimulusInfoStore = StateJsonConverter.deserializeStimuli(json);
								currentScenario.getScenarioStore().setStimulusInfoStore(stimulusInfoStore);
								break;
							default:
								throw new RuntimeException("attribute type not implemented.");
						}
						currentScenario.updateCurrentStateSerialized();
						ScenarioPanel.removeJsonParsingErrorMsg();
						ProjectView.getMainWindow().refreshScenarioNames();
						jsonValidIndicator.setValid();
						if (scenarioChecker != null){
							scenarioChecker.checkScenario(currentScenario);
						}
					} catch (IOException  e) {
						ScenarioPanel.setActiveJsonParsingErrorMsg(attributeType.name() + " tab:\n" + e.getMessage());
						jsonValidIndicator.setInvalid();
					}
				}
			}
		};

		this.attributeType = attributeType;
		jsonValidIndicator.setValid();
	}

	private void checkInsert2ndQuotationMark(DocumentEvent e) {
		Document document = e.getDocument();
		String prevChar;
		String postChar;
		try {
			prevChar = document.getText(e.getOffset()-1,1);
			postChar = document.getText(e.getOffset()+1,1);
			if(prevChar.equals("\"")) {
			}
			else if(postChar.equals("\"")) {
			}
			else if(isOpenString(document.getText(e.getOffset()-2,1).charAt(0))){}
			else if(isOpenString(document.getText(e.getOffset()+1,1).charAt(0))){}
			else {
				SwingUtilities.invokeLater(() -> {
					try {
						document.insertString(e.getOffset() + 1, "\"", null);
					} catch (BadLocationException ex) {
						throw new RuntimeException(ex);
					}
					textfileTextarea.setCaretPosition(e.getOffset()+1);
				});
			}
		} catch (BadLocationException ex) {
			throw new RuntimeException(ex);
		}
	}

	private boolean isOpenString(char character) {
		return Character.isDigit(character) || Character.isAlphabetic(character);
	}

	private void checkInsert2ndSwirlyBracket(DocumentEvent e){
		Document document = e.getDocument();
		SwingUtilities.invokeLater(() -> {
			try {
				document.insertString(e.getOffset() + 1, "}", null);
			} catch (BadLocationException ex) {
				throw new RuntimeException(ex);
			}
			textfileTextarea.setCaretPosition(e.getOffset()+1);
		});
	}
	private void checkInsert2ndSquareBracket(DocumentEvent e){
		Document document = e.getDocument();
		SwingUtilities.invokeLater(() -> {
			try {
				document.insertString(e.getOffset() + 1, "]", null);
			} catch (BadLocationException ex) {
				throw new RuntimeException(ex);
			}
			textfileTextarea.setCaretPosition(e.getOffset()+1);
		});
	}
	private static String getInsertedText(DocumentEvent e) {
		Document document = e.getDocument();
		String insertedText;
		try {
			insertedText = document.getText(e.getOffset(), e.getLength());
		} catch (BadLocationException ex) {
			throw new RuntimeException(ex);
		}
		return insertedText;
	}

	private void generatePresettingsMenu(final AttributeType attributeType) {
		if (attributeType == AttributeType.PERCEPTION) {
			JMenuBar presetMenuBar = new JMenuBar();
			JMenu mnPresetMenu = new JMenu(Messages.getString("TextView.Button.LoadPresettings"));
			presetMenuBar.add(mnPresetMenu);

			StimulusPresettings.PRESETTINGS_MAP.forEach(
					(clazz, jsonString) -> mnPresetMenu.add(new JMenuItem(new AbstractAction(clazz.getSimpleName()) {
						private static final long serialVersionUID = 1L;

						@Override
						public void actionPerformed(ActionEvent e) {
							if (JOptionPane.showConfirmDialog(ProjectView.getMainWindow(),
									Messages.getString("Tab.Model.confirmLoadTemplate.text"),
									Messages.getString("Tab.Model.confirmLoadTemplate.title"),
									JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
								try {
									textfileTextarea.setText(jsonString);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						}
					})));

			JMenu mnPresetHelpMenu = new JMenu(Messages.getString("ProjectView.mnHelp.text"));
			presetMenuBar.add(mnPresetHelpMenu);

			StimulusPresettings.PRESETTINGS_MAP.forEach(
					(clazz, jsonString) -> mnPresetHelpMenu.add(new JMenuItem(new AbstractAction(clazz.getSimpleName()) {
						private static final long serialVersionUID = 1L;

						@Override
						public void actionPerformed(ActionEvent e) {
							VDialogManager.showMessageDialogWithBodyAndTextEditorPane("Help", clazz.getName(),
									HelpTextView.create(clazz.getName()), JOptionPane.INFORMATION_MESSAGE);
						}
					})));

			panelTop.add(presetMenuBar);
		}
	}

	@Override
	public void setVadereScenario(Scenario scenario) { // in order to avoid passing the exception upwards. might not be the best solution
		try {
			setVadereScenarioThrows(scenario);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

	private void setVadereScenarioThrows(Scenario scenario) throws JsonProcessingException {
		currentScenario = scenario;

		switch (attributeType) {
		case MODEL:
			textfileTextarea.setText(StateJsonConverter.serializeMainModelAttributesModelBundle(
					scenario.getModelAttributes(), scenario.getScenarioStore().getMainModel()));
			break;
		case SIMULATION:
			textfileTextarea
					.setText(StateJsonConverter.serializeAttributesSimulation(scenario.getAttributesSimulation()));
			break;
		case PSYCHOLOGY:
			textfileTextarea
					.setText(StateJsonConverter.serializeAttributesPsychology(scenario.getAttributesPsychology()));
			break;
		case OUTPUTPROCESSOR:
			textfileTextarea.setText(scenario.getDataProcessingJsonManager().serialize());
			break;
		case TOPOGRAPHY:
			Topography topography = scenario.getTopography().clone();
			topography.removeBoundary();
			textfileTextarea.setText(StateJsonConverter.serializeTopography(topography));
			break;
		case PERCEPTION:
			StimulusInfoStore stimulusInfoStore = scenario.getScenarioStore().getStimulusInfoStore();
			textfileTextarea.setText(StateJsonConverter.serializeStimuli(stimulusInfoStore));
			break;
		default:
			throw new RuntimeException("attribute type not implemented.");
		}
		textfileTextarea.setCaretPosition(0);
	}

	@Override
	public void isEditable(boolean isEditable) {
		this.isEditable = isEditable;
		btnLoadFromFile.setEnabled(isEditable);
		textfileTextarea.setEnabled(isEditable);
		if (isEditable) {
			textfileTextarea.setBackground(Color.WHITE);
			textfileTextarea.getDocument().addDocumentListener(documentListener);
		} else {
			textfileTextarea.setBackground(Color.LIGHT_GRAY);
			textfileTextarea.getDocument().removeDocumentListener(documentListener);
		}
	}

	public JPanel getPanelTop() {
		return panelTop;
	}

	public void setText(String text) {
		textfileTextarea.setText(text);
	}

	public String getText() {
		return textfileTextarea.getText();
	}

	public void insertAtCursor(String text) {
		textfileTextarea.insert(text, textfileTextarea.getCaretPosition());
	}

	public void setScenarioChecker(IScenarioChecker scenarioChecker) {
		this.scenarioChecker = scenarioChecker;
	}
}
