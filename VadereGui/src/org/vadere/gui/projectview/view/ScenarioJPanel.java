package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.gui.projectview.control.IProjectChangeListener;
import org.vadere.gui.projectview.utils.ClassFinder;
import org.vadere.gui.topographycreator.view.TopographyWindow;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.ScenarioRunManager;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.simulator.projects.io.JsonConverter;
import org.vadere.state.scenario.Topography;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.beans.IntrospectionException;
import java.io.File;
import java.io.IOException;


public class ScenarioJPanel extends JPanel implements IProjectChangeListener, ProjectFinishedListener {

	private static Logger logger = LogManager.getLogger(ScenarioJPanel.class);
	private static final long serialVersionUID = 7217609523783631174L;

	private JTabbedPane tabbedPane;
	private final JFrame owner;
	private final JLabel scenarioName;

	// tabs
	private TextView attributesSimulationView; // Simulation tab
	private TextView attributesModelView; // Model tab
	private TextView topographyFileView; // Topography tab
	private OutputProcessorsView outputProcessorsView; // Output processors tab
	private TopographyWindow topographyCreatorView; // Topography creator tab... OR:
	private final PostvisualizationWindow postVisualizationView; // Post-Visualization tab, replaces Topography tab if output is selected

	// during simulation-run, only this is shown instead of the tabs above:
	private final OnlineVisualization onlineVisualization;

	private JPanel visualizationCard;

	private String visualizationCardName = "visualization";
	private String editCardName = "edit";

	private ScenarioRunManager scenario;
	private boolean initialized;

	private static String activeJsonParsingErrorMsg = null;


	public ScenarioJPanel(JFrame owner, JLabel scenarioName) {
		this.owner = owner;
		this.scenarioName = scenarioName;
		this.onlineVisualization = new OnlineVisualization(true);
		this.postVisualizationView = new PostvisualizationWindow();

		super.setBorder(new EmptyBorder(5, 5, 5, 5));
		super.setLayout(new CardLayout(0, 0));
		super.setBounds(0, 0, 500, 100);
	}

	private void initialize() {
		this.initialized = true;

		// Edit card...
		JPanel editCard = new JPanel();

		editCard.setBorder(new EmptyBorder(5, 5, 5, 5));
		editCard.setLayout(new BorderLayout(0, 0));
		editCard.setBounds(0, 0, 500, 100);

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		editCard.add(tabbedPane, BorderLayout.CENTER);

		tabbedPane.addChangeListener(e -> {
			int index = tabbedPane.getSelectedIndex();
			if (index >= 0 && topographyFileView != null
					&& index == tabbedPane.indexOfTab(Messages.getString("Tab.Topography.title"))
					&& scenario != null) {
				topographyFileView.setVadereScenario(scenario);
			}
			if (index >= 0 && topographyFileView != null
					&& index == tabbedPane.indexOfTab(Messages.getString("Tab.TopographyCreator.title"))
					&& scenario != null) {
				setTopography(scenario.getTopography());
			}
		});

		attributesSimulationView =
				new TextView("/attributes", "default_directory_attributes", AttributeType.SIMULATION);
		tabbedPane.addTab(Messages.getString("Tab.Simulation.title"), null, attributesSimulationView, null);

		attributesModelView = new TextView("/attributes", "default_directory_attributes", AttributeType.MODEL);

		JMenuBar presetMenuBar = new JMenuBar();

		JMenu mnPresetMenu = new JMenu(Messages.getString("Tab.Model.loadTemplateMenu.title"));
		presetMenuBar.add(mnPresetMenu);
		ModelPresets.getPresets().forEach(
				modelDefinition -> mnPresetMenu.add(new JMenuItem(new AbstractAction(modelDefinition.getMainModel()) {
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						if (JOptionPane.showConfirmDialog(ProjectView.getMainWindow(),
								Messages.getString("Tab.Model.confirmLoadTemplate.text"),
								Messages.getString("Tab.Model.confirmLoadTemplate.title"),
								JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
							try {
								attributesModelView.setText(JsonConverter.serializeModelPreset(modelDefinition));
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
				})));


		JMenu mnAttributesMenu = new JMenu(Messages.getString("Tab.Model.addAttributesMenu.title"));
		presetMenuBar.add(mnAttributesMenu);
		ClassFinder.getAttributesNames().forEach(
				attributesClassName -> mnAttributesMenu.add(new JMenuItem(new AbstractAction(attributesClassName) {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							attributesModelView.setText(JsonConverter.addAttributesModel(attributesClassName,
									attributesModelView.getText()));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				})));

		JMenu mnModelNameMenu = new JMenu(Messages.getString("Tab.Model.insertModelNameMenu.title"));
		presetMenuBar.add(mnModelNameMenu);
		ClassFinder.getMainModelNames()
				.forEach(className -> mnModelNameMenu.add(new JMenuItem(new AbstractAction(className + " (MainModel)") {
					@Override
					public void actionPerformed(ActionEvent e) {
						attributesModelView.insertAtCursor("\"" + className + "\"");
					}
				})));
		ClassFinder.getModelNames()
				.forEach(className -> mnModelNameMenu.add(new JMenuItem(new AbstractAction(className) {
					@Override
					public void actionPerformed(ActionEvent e) {
						attributesModelView.insertAtCursor("\"" + className + "\"");
					}
				})));

		attributesModelView.getPanelTop().add(presetMenuBar, 0); // the 0 puts it at the leftest position instead of the rightest
		tabbedPane.addTab(Messages.getString("Tab.Model.title"), null, attributesModelView, null);

		topographyFileView = new TextView("/scenarios", "default_directory_scenarios", AttributeType.TOPOGRAPHY);
		tabbedPane.addTab(Messages.getString("Tab.Topography.title"), null, topographyFileView, null);

		try {
			outputProcessorsView = new OutputProcessorsView(owner);
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getLocalizedMessage());
		}

		tabbedPane.addTab(Messages.getString("Tab.OutputProcessors.title"), null, outputProcessorsView, null);

		// online visualization card...
		visualizationCard = new JPanel();

		visualizationCard.setBorder(new EmptyBorder(5, 5, 5, 5));
		visualizationCard.setLayout(new BorderLayout(0, 0));
		visualizationCard.setBounds(0, 0, 500, 100);
		visualizationCard.add(onlineVisualization.getVisualizationPanel());

		// Add panels
		super.add(editCard, editCardName);
		super.add(visualizationCard, visualizationCardName);

	}

	public void showVisualization() {
		CardLayout cl = (CardLayout) this.getLayout();
		cl.show(this, visualizationCardName);
		onlineVisualization.getMainPanel().setVisible(true);
	}

	public void showEditScenario() {
		CardLayout cl = (CardLayout) this.getLayout();
		cl.show(this, editCardName);
	}

	/**
	 * Shows data of a specific scenario.
	 * 
	 * @param scenario
	 *        Vadere with data that should be shown and edited.
	 */
	public void setScenario(ScenarioRunManager scenario, boolean isEditable) {
		this.scenario = scenario;
		this.scenarioName.setText(scenario.getDisplayName());
		if (!initialized) {
			initialize();
		}

		if (isEditable) {
			try {
				int index = tabbedPane.getSelectedIndex();
				if (topographyCreatorView != null && tabbedPane.indexOfComponent(topographyCreatorView) >= 0) {
					tabbedPane.removeTabAt(tabbedPane.indexOfComponent(topographyCreatorView));
				}

				topographyCreatorView = new TopographyWindow(scenario);
				tabbedPane.addTab(Messages.getString("Tab.TopographyCreator.title"), topographyCreatorView);
				setTopography(scenario.getTopography());
				tabbedPane.setSelectedIndex(index);

			} catch (IOException | IntrospectionException e) {
				e.printStackTrace();
				logger.error(e.getLocalizedMessage());
			}
		} else {
			boolean topoWasSelected = false;
			if (tabbedPane.indexOfComponent(topographyCreatorView) >= 0) {
				topoWasSelected = tabbedPane.getSelectedComponent().equals(topographyCreatorView);
				tabbedPane.removeTabAt(tabbedPane.indexOfComponent(topographyCreatorView));
			}
			if (tabbedPane.indexOfComponent(postVisualizationView) < 0) {
				tabbedPane.addTab(Messages.getString("Tab.PostVisualization.title"), postVisualizationView);
				if (topoWasSelected) {
					tabbedPane.setSelectedComponent(postVisualizationView);
				}
			}
			postVisualizationView.repaint(); // force a repaint, otherwise it sometimes only repaints when the mouse moves from the output table to the postvis-view
		}

		this.attributesModelView.isEditable(isEditable);
		this.attributesModelView.setVadereScenario(scenario);

		this.attributesSimulationView.isEditable(isEditable);
		this.attributesSimulationView.setVadereScenario(scenario);

		this.topographyFileView.isEditable(isEditable);
		this.topographyFileView.setVadereScenario(scenario);

		this.outputProcessorsView.isEditable(isEditable);
		this.outputProcessorsView.setScenario(scenario);
	}

	private void setTopography(Topography topography) {
		if (tabbedPane.indexOfComponent(postVisualizationView) >= 0) {
			tabbedPane.removeTabAt(tabbedPane.indexOfComponent(postVisualizationView));
		}

		try {
			topography.removeBoundary();
			topographyCreatorView.getPanelModel().setTopography(topography);
			topographyCreatorView.getPanelModel().resetTopographySize();
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public void clear() {
		this.scenarioName.setText("");
		this.initialized = false;

		super.removeAll();

		super.setBorder(new EmptyBorder(5, 5, 5, 5));
		super.setLayout(new CardLayout(0, 0));
		super.setBounds(0, 0, 500, 100);
	}

	@Override
	public void projectChanged(final VadereProject project) {
		clear();
		project.addVisualization(onlineVisualization);
	}

	@Override
	public void propertyChanged(final VadereProject project) {}

	@Override
	public void preProjectRun(final VadereProject project) {
		showVisualization();
	}

	@Override
	public void postProjectRun(final VadereProject scenario) {
		showEditScenario();
	}

	public void loadOutputFileForPostVis(ScenarioRunManager scenarioRM) throws IOException {
		postVisualizationView.loadOutputFile(scenarioRM);
	}

	public void loadOutputFileForPostVis(File trajectoryFile, ScenarioRunManager scenarioRM) throws IOException {
		postVisualizationView.loadOutputFile(trajectoryFile, scenarioRM);
	}

	public static void setActiveJsonParsingErrorMsg(String msg) {
		activeJsonParsingErrorMsg = msg;
	}

	public static String getActiveJsonParsingErrorMsg() {
		return activeJsonParsingErrorMsg;
	}

	public static void removeJsonParsingErrorMsg() {
		activeJsonParsingErrorMsg = null;
	}

	public void updateScenario() {
		setScenario(scenario, true);
	}
}
