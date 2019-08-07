package org.vadere.gui.components.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.projectview.view.JsonValidIndicator;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.gui.projectview.view.ScenarioPanel;
import org.vadere.gui.topographycreator.model.AgentWrapper;
import org.vadere.gui.topographycreator.model.TopographyCreatorModel;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * The ScenarioElementView display's a ScenarioElement in JSON-Format.
 */
public class ScenarioElementView extends JPanel implements ISelectScenarioElementListener {

	private static final long serialVersionUID = -1567362675580536991L;
	private static Logger logger = Logger.getLogger(ScenarioElementView.class);
	private RSyntaxTextArea txtrTextfiletextarea;
	private IDefaultModel panelModel;
	private DocumentListener documentListener;

	private JsonValidIndicator jsonValidIndicator;

	public ScenarioElementView(final IDefaultModel defaultModel) {
		this(defaultModel,null, null);
	}

	public ScenarioElementView(final IDefaultModel defaultModel, final JsonValidIndicator jsonValidIndicator, final Component topComponent) {
		this.panelModel = defaultModel;
		this.panelModel.addSelectScenarioElementListener(this);
		this.jsonValidIndicator = jsonValidIndicator;
		CellConstraints cc = new CellConstraints();

		txtrTextfiletextarea = new RSyntaxTextArea();
		txtrTextfiletextarea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
		txtrTextfiletextarea.setCodeFoldingEnabled(true);

		RTextScrollPane sp = new RTextScrollPane(txtrTextfiletextarea);
		sp.setFoldIndicatorEnabled(true);
		sp.setLineNumbersEnabled(true);
		sp.setPreferredSize(new Dimension(1, Toolkit.getDefaultToolkit().getScreenSize().height));

		if (topComponent != null && this.jsonValidIndicator !=null) {
			setLayout(new FormLayout("default:grow", "pref, default"));

			JPanel jsonMeta = new JPanel(); // name of the scenario element and indicator of
			// valid/invalid
			jsonMeta.setLayout(new BoxLayout(jsonMeta, BoxLayout.Y_AXIS));

			jsonMeta.add(this.jsonValidIndicator);
			this.jsonValidIndicator.hide();
			jsonMeta.add(topComponent);

			add(jsonMeta, cc.xy(1, 1));
			add(sp, cc.xy(1, 2));
		} else {
			setLayout(new FormLayout("default:grow", "default"));
			add(sp, cc.xy(1, 1));
		}

		// set other color theme for text area...
		InputStream in = getClass().getResourceAsStream("/syntaxthemes/idea.xml");
		try {
			Theme syntaxTheme = Theme.load(in);
			syntaxTheme.apply(txtrTextfiletextarea);
		} catch (IOException e) {
			logger.error("could not loadFromFilesystem theme" + e.getMessage());
		}



		// documentListener = new JSONDocumentListener(defaultModel);
		// txtrTextfiletextarea.getDocument().addDocumentListener(documentListener);

		documentListener = new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				updateModel();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				updateModel();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {

				updateModel();
			}
		};

		txtrTextfiletextarea.getDocument().addDocumentListener(documentListener);
	}

	public int textWidth(){
		return txtrTextfiletextarea.getPreferredSize().width;
	}

	private void updateModel() {
		// set the content for the view
		// defaultModel.setJSONContent(event.getDocument().getText(0,
		// event.getDocument().getLength()));
		ScenarioElement element = panelModel.getSelectedElement();
		if (element != null) {
			String json = txtrTextfiletextarea.getText();
			//logger.info(json);
			if (json.length() == 0)
				return;

			// try {
			if (element instanceof AgentWrapper) {
				// JsonSerializerVShape shapeSerializer = new JsonSerializerVShape();
				Pedestrian ped = null;
				try {
					ped = StateJsonConverter.deserializePedestrian(json);
				} catch (IOException e) {
					e.printStackTrace();
				}
				((AgentWrapper) element).setAgentInitialStore(ped);
			} else {
				try {
					Attributes attributes = StateJsonConverter.deserializeScenarioElementType(json, element.getType());
					element.setAttributes(attributes); // Replaces previous AttributeModifier.setAttributes (see #91)
					ScenarioPanel.removeJsonParsingErrorMsg();
					ProjectView.getMainWindow().refreshScenarioNames();
					jsonValidIndicator.setValid();
					((TopographyCreatorModel) panelModel).getScenario().updateCurrentStateSerialized(); // casting should be safe her because in the other two modes (onlineVis and postVis), updateModel() won't be called because it's set to uneditable
				} catch (IOException e) {
					ScenarioPanel.setActiveJsonParsingErrorMsg("TOPOGRAPHY CREATOR tab:\n" + e.getMessage()); // add name of scenario element?
					jsonValidIndicator.setInvalid();
				}
			}
			panelModel.setElementHasChanged(element);
			panelModel.notifyObservers();
		}
	}

	public void setEditable(final boolean editable) {
		txtrTextfiletextarea.setEditable(editable);
		if (editable) {
			txtrTextfiletextarea.setBackground(Color.WHITE);
			txtrTextfiletextarea.getDocument().addDocumentListener(documentListener);
		} else {
			txtrTextfiletextarea.setBackground(Color.LIGHT_GRAY);
			txtrTextfiletextarea.getDocument().removeDocumentListener(documentListener);
		}
	}

	@Override
	public void selectionChange(final ScenarioElement scenarioElement) {
		synchronized (txtrTextfiletextarea) {
			if (scenarioElement == null) {
				this.txtrTextfiletextarea.setText("");
				if(jsonValidIndicator != null) {
					jsonValidIndicator.hide();
				}
			} else {
				if (scenarioElement instanceof AgentWrapper) {
					this.txtrTextfiletextarea.setText(
							StateJsonConverter.serializeObjectPretty(((AgentWrapper) scenarioElement).getAgentInitialStore()));
				} else if (scenarioElement instanceof Pedestrian) {
					this.txtrTextfiletextarea.setText(StateJsonConverter.serializeObjectPretty(scenarioElement));
				} else {
					this.txtrTextfiletextarea.setText(StateJsonConverter
							.serializeObjectPretty(scenarioElement.getAttributes()));
				}
			}
		}
	}
}
