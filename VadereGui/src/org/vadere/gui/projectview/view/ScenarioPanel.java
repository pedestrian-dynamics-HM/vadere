package org.vadere.gui.projectview.view;


import org.vadere.gui.components.control.HelpTextView;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.onlinevisualization.OnlineVisualization;
import org.vadere.gui.postvisualization.view.PostvisualizationWindow;
import org.vadere.gui.projectview.control.IProjectChangeListener;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.topographycreator.view.TopographyWindow;
import org.vadere.simulator.models.ModelHelper;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.state.attributes.ModelAttributeFactory;
import org.vadere.state.scenario.Topography;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.logging.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.swing.*;
import javax.swing.border.EmptyBorder;


public class ScenarioPanel extends JPanel implements IProjectChangeListener, ProjectFinishedListener {

	private static Logger logger = Logger.getLogger(ScenarioPanel.class);
	private static final long serialVersionUID = 0L;

	private JTabbedPane tabbedPane;
//	private final ScenarioNamePanel scenarioNamePanel;

	// tabs
	private List<JMenu> menusInTabs = new ArrayList<>();
	private TextView attributesSimulationView; // Simulation tab
	private TextView attributesModelView; // Model tab
	private TextView attributesPsychologyView; // Psychology tab
	private TextView topographyFileView; // Topography tab
	private TextView perceptionFileView; // Stimulus tab
	private DataProcessingView dataProcessingGUIview; // DataProcessing
	private TopographyWindow topographyCreatorView; // Topography creator tab... OR:
	private final PostvisualizationWindow postVisualizationView; // Post-Visualization tab, replaces Topography tab if output is selected

	// during simulation-run, only this is shown instead of the tabs above:
	private final OnlineVisualization onlineVisualization;

	private String visualizationCardName = "visualization";
	private String editCardName = "edit";

	private Scenario scenario;
	private boolean initialized;
	private ProjectViewModel model;

	private static String activeJsonParsingErrorMsg = null;
	private static JEditorPane activeTopographyErrorMsg = null;


	ScenarioPanel(ProjectViewModel model) {
		this.onlineVisualization = new OnlineVisualization(true);
		this.postVisualizationView = new PostvisualizationWindow(model.getCurrentProjectPath());
        this.model = model;

		setBorder(new EmptyBorder(0, 0, 0, 0));
		setLayout(new CardLayout(0, 0));
		setBounds(0, 0, 500, 100);
	}

	@SuppressWarnings("serial")
	private void initialize() {
		initialized = true;

		// Edit card...
		JPanel editCard = new JPanel();
		editCard.setBorder(new EmptyBorder(0, 0, 0, 0));
		editCard.setLayout(new BorderLayout(0, 5));
		editCard.setBounds(0, 0, 500, 100);

		tabbedPane = new JTabbedPane(SwingConstants.TOP);
		editCard.add(tabbedPane, BorderLayout.CENTER);



		tabbedPane.addChangeListener(e -> {
			// remove ScenarioChecker listener if exists
			model.scenarioCheckerStopObserve();

			int index = tabbedPane.getSelectedIndex();
			if (index >= 0 && topographyFileView != null
					&& index == tabbedPane.indexOfTab(Messages.getString("Tab.Topography.title"))
					&& scenario != null) {
				topographyFileView.setVadereScenario(scenario);
			} else 	if (index >= 0 && topographyCreatorView != null
					&& index == tabbedPane.indexOfTab(Messages.getString("Tab.TopographyCreator.title"))
					&& scenario != null) {
				setTopography(scenario.getTopography());
				model.scenarioCheckerStartObserve(topographyCreatorView.getPanelModel());
				return;
			}

//			model.scenarioCheckerCheck(scenario);
		});

		//Tab
		attributesSimulationView =
				new TextView("ProjectView.defaultDirectoryAttributes", AttributeType.SIMULATION);
		attributesSimulationView.setScenarioChecker(model);
		tabbedPane.addTab(Messages.getString("Tab.Simulation.title"), attributesSimulationView);

		//Tab
		attributesModelView = new TextView("ProjectView.defaultDirectoryAttributes", AttributeType.MODEL);
		attributesModelView.setScenarioChecker(model);

		JMenuBar presetMenuBar = new JMenuBar();

		JMenu mnPresetMenu = new JMenu(Messages.getString("Tab.Model.loadTemplateMenu.title"));
		presetMenuBar.add(mnPresetMenu);

		menusInTabs.add(mnPresetMenu);
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
								attributesModelView.setText(StateJsonConverter.serializeModelPreset(modelDefinition));
							} catch (Exception e1) {
								e1.printStackTrace();
							}
						}
					}
				})));

		JMenu mnAttributesMenu = new JMenu(Messages.getString("Tab.Model.addAttributesMenu.title"));
		presetMenuBar.add(mnAttributesMenu);
		menusInTabs.add(mnAttributesMenu);
		ModelAttributeFactory attributeFactory = ModelAttributeFactory.instance();
		attributeFactory.sortedAttributeStream().forEach(
				attributesClassName -> mnAttributesMenu.add(new JMenuItem(new AbstractAction(attributesClassName) {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							attributesModelView.setText(StateJsonConverter.addAttributesModel(attributesClassName,
									attributesModelView.getText()));
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				})));

		JMenu mnHelpAttributesMenu = new JMenu(Messages.getString("Tab.Model.helpAttributesMenu.title"));
		presetMenuBar.add(mnHelpAttributesMenu);
		menusInTabs.add(mnHelpAttributesMenu);
		attributeFactory.sortedAttributeStream().forEach(
				attributesClassName -> mnHelpAttributesMenu.add(new JMenuItem(new AbstractAction(attributesClassName) {
					@Override
					public void actionPerformed(ActionEvent e) {
						VDialogManager.showMessageDialogWithBodyAndTextEditorPane("Help", attributesClassName,
								HelpTextView.create(attributesClassName), JOptionPane.INFORMATION_MESSAGE);
					}
				})));
		
		JMenu mnModelNameMenu = new JMenu(Messages.getString("Tab.Model.insertModelNameMenu.title"));
		presetMenuBar.add(mnModelNameMenu);
		menusInTabs.add(mnModelNameMenu);
		
		JMenu submenuMainModels = new JMenu(Messages.getString("Tab.Model.insertModelNameSubMenu.title"));
		mnModelNameMenu.add(submenuMainModels);

		ModelHelper.instance().getSortedMainModel()
				.forEach(className -> submenuMainModels.add(new JMenuItem(new AbstractAction(className) {
					@Override
					public void actionPerformed(ActionEvent e) {
						attributesModelView.insertAtCursor("\"" + className + "\"");
					}
				}
				)));
		

		ModelHelper.instance().getModelsSortedByPackageStream().forEach( entry -> {
			JMenu currentSubMenu = new JMenu(entry.getKey());

			for (String className : entry.getValue()) {
				currentSubMenu.add(new JMenuItem(new AbstractAction(className) {
					@Override
					public void actionPerformed(ActionEvent e) {
						attributesModelView.insertAtCursor("\"" + className + "\"");
					}
				}));
			}

			mnModelNameMenu.add(currentSubMenu);
		});
	
		attributesModelView.getPanelTop().add(presetMenuBar, 0); // the 0 puts it at the leftmost position instead of the rightmost
		tabbedPane.addTab(Messages.getString("Tab.Model.title"), attributesModelView);

		attributesPsychologyView =
				new TextView("ProjectView.defaultDirectoryAttributes", AttributeType.PSYCHOLOGY);
		attributesPsychologyView.setScenarioChecker(model); // use .isEditable(true); to save time (no check!)
		tabbedPane.addTab(Messages.getString("Tab.Psychology.title"), attributesPsychologyView);

		topographyFileView = new TextView("ProjectView.defaultDirectoryScenarios", AttributeType.TOPOGRAPHY);
		topographyFileView.setScenarioChecker(model);
		tabbedPane.addTab(Messages.getString("Tab.Topography.title"), topographyFileView);

		perceptionFileView = new TextView( "ProjectView.defaultDirectoryAttributes", AttributeType.PERCEPTION);
		perceptionFileView.setScenarioChecker(model);
		tabbedPane.addTab(Messages.getString("Tab.Perception.title"), perceptionFileView);

		dataProcessingGUIview = new DataProcessingView(model);
		tabbedPane.addTab(Messages.getString("Tab.OutputProcessors.title"), dataProcessingGUIview);
		
		// online visualization card...
		JPanel visualizationCard = new JPanel();

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
		// [issue 280] set visibility and remove mouse listeners during setup. Will be added
		// after setup of simulation run is complete.
		onlineVisualization.showVisualization();
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
	public void setScenario(Scenario scenario, boolean isEditable) {
		this.scenario = scenario;
		model.setScenarioNameLabelString(scenario.getDisplayName());

		if (!initialized) {
			initialize();
		}

		if (isEditable) {
			menusInTabs.forEach(menu -> menu.setEnabled(true));

				int index = tabbedPane.getSelectedIndex();
				if (topographyCreatorView != null && tabbedPane.indexOfComponent(topographyCreatorView) >= 0) {
					tabbedPane.removeTabAt(tabbedPane.indexOfComponent(topographyCreatorView));
				}

				topographyCreatorView = new TopographyWindow(scenario, model);
				tabbedPane.addTab(Messages.getString("Tab.TopographyCreator.title"), topographyCreatorView);
				tabbedPane.validate();
				tabbedPane.repaint();
				tabbedPane.setSelectedIndex(index);
				setTopography(scenario.getTopography());

		} else {
			menusInTabs.forEach(menu -> menu.setEnabled(false));
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
			tabbedPane.validate();
			tabbedPane.repaint();
			postVisualizationView.revalidate();
			postVisualizationView.repaint(); // force a repaint, otherwise it sometimes only repaints when the mouse moves from the output table to the postvis-view
			postVisualizationView.getDefaultModel().resetTopographySize();
		}

		this.attributesSimulationView.setVadereScenario(scenario);
		this.attributesSimulationView.isEditable(isEditable);

		this.attributesModelView.setVadereScenario(scenario);
		this.attributesModelView.isEditable(isEditable);

		this.attributesPsychologyView.setVadereScenario(scenario);
		this.attributesPsychologyView.isEditable(isEditable);

		this.topographyFileView.setVadereScenario(scenario);
		this.topographyFileView.isEditable(isEditable);

		this.perceptionFileView.setVadereScenario(scenario);
		this.perceptionFileView.isEditable(isEditable);

		this.dataProcessingGUIview.setVadereScenario(scenario);
		this.dataProcessingGUIview.isEditable(isEditable);
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

	public void clearScenarioView() {
		model.setScenarioNameLabelString("");
		initialized = false;

		removeAll();

		setBorder(new EmptyBorder(5, 5, 5, 5));
		setLayout(new CardLayout(0, 0));
		setBounds(0, 0, 500, 100);
	}

	@Override
	public void projectChanged(final VadereProject project) {
		clearScenarioView();
		project.setVisualization(onlineVisualization);
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

	public void loadOutputFileForPostVis(Scenario scenarioRM) throws IOException {
		postVisualizationView.loadOutputFile(scenarioRM);
	}
	public void loadOutputFileForPostVis(File trajectoryFile, HashMap<String, File> additionalPostVisFiles, Scenario scenarioRM) throws IOException {
		postVisualizationView.loadOutputFile(trajectoryFile, additionalPostVisFiles, scenarioRM);
	}
	public void loadOutputFileForPostVis(File trajectoryFile, Scenario scenarioRM) throws IOException {
		postVisualizationView.loadOutputFile(trajectoryFile, scenarioRM);
	}

	public static void setActiveTopographyErrorMsg(JEditorPane msg){
		activeTopographyErrorMsg = msg;
	}

	public static JEditorPane getActiveTopographyErrorMsg(){
		return activeTopographyErrorMsg;
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
