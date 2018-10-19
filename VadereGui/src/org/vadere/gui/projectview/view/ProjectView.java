package org.vadere.gui.projectview.view;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.vadere.gui.components.utils.Language;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.postvisualization.control.Player;
import org.vadere.gui.projectview.VadereApplication;
import org.vadere.gui.projectview.control.ActionAddScenario;
import org.vadere.gui.projectview.control.ActionCloneScenario;
import org.vadere.gui.projectview.control.ActionCloseApplication;
import org.vadere.gui.projectview.control.ActionCreateProject;
import org.vadere.gui.projectview.control.ActionDeleteOutputDirectories;
import org.vadere.gui.projectview.control.ActionDeleteScenarios;
import org.vadere.gui.projectview.control.ActionEditScenarioDescription;
import org.vadere.gui.projectview.control.ActionGenerateScenarioFromOutputFile;
import org.vadere.gui.projectview.control.ActionInterruptScenarios;
import org.vadere.gui.projectview.control.ActionLoadProject;
import org.vadere.gui.projectview.control.ActionLoadRecentProject;
import org.vadere.gui.projectview.control.ActionOutputToScenario;
import org.vadere.gui.projectview.control.ActionPauseScenario;
import org.vadere.gui.projectview.control.ActionRenameOutputFile;
import org.vadere.gui.projectview.control.ActionRenameProject;
import org.vadere.gui.projectview.control.ActionRenameScenario;
import org.vadere.gui.projectview.control.ActionRunAllScenarios;
import org.vadere.gui.projectview.control.ActionRunOutput;
import org.vadere.gui.projectview.control.ActionRunSelectedScenarios;
import org.vadere.gui.projectview.control.ActionRunSelectedScenariosOnline;
import org.vadere.gui.projectview.control.ActionSaveAsProject;
import org.vadere.gui.projectview.control.ActionSaveProject;
import org.vadere.gui.projectview.control.ActionSeeDiscardChanges;
import org.vadere.gui.projectview.control.ActionShowAboutDialog;
import org.vadere.gui.projectview.control.IOutputFileRefreshListener;
import org.vadere.gui.projectview.control.IProjectChangeListener;
import org.vadere.gui.projectview.control.ShowResultDialogAction;
import org.vadere.gui.projectview.model.ProjectViewModel;
import org.vadere.gui.projectview.model.ProjectViewModel.OutputBundle;
import org.vadere.gui.projectview.model.ProjectViewModel.ScenarioBundle;
import org.vadere.gui.projectview.model.VadereScenarioTableModel.VadereDisplay;
import org.vadere.gui.projectview.model.VadereState;
import org.vadere.gui.projectview.utils.TableSelectionListener;
import org.vadere.simulator.projects.ProjectFinishedListener;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.SingleScenarioFinishedListener;
import org.vadere.simulator.projects.VadereProject;
import org.vadere.util.io.IOUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;

/**
 * Main view of the Vadere GUI.
 *
 *
 */
public class ProjectView extends JFrame implements ProjectFinishedListener, SingleScenarioFinishedListener,
		IOutputFileRefreshListener, IProjectChangeListener {
	/**
	 * Static variables
	 */
	private static final long serialVersionUID = -2081363246241235943L;
	private static Logger logger = LogManager.getLogger(ProjectView.class);
	/** Store a reference to the main window as "owner" parameter for dialogs. */
	private static ProjectView mainWindow;

	/**
	 * The model of the {@link ProjectView}
	 */
	private ProjectViewModel model;

	/**
	 * GUI elements (part of the view) of the {@link ProjectView}
	 * 
	 * TODO [priority=medium] [task=refactoring] do the actions have to be stored in member variables
	 * or could it be better to store them locally where they are needed?
	 * Some are used in different methods, maybe only store these as members?
	 */
	private JPanel contentPane = new JPanel();
	private JPanel controlPanel = new JPanel();
	private VTable scenarioTable;
	private VTable outputTable;
	private JButton btnRunSelectedScenario;
	private JButton btnRunAllScenarios;
	private JButton btnStopRunningScenarios;
	private JButton btnPauseRunningScenarios;
	private JMenu mntmRecentProjects;
	private ProgressPanel progressPanel = new ProgressPanel();
	private ScenarioPanel scenarioJPanel;
	private ScenarioNamePanel scenarioNamePanel;
	private boolean scenariosRunning = false;
	private Set<Action> projectSpecificActions = new HashSet<>(); // actions that should only be enabled, when a project is loaded
	private ProjectRunResultDialog projectRunResultDialog;

	// ####################### Part of the control this should also be part of another class
	// ##################
	@Override
	public void postProjectRun(final VadereProject scenario) {
		EventQueue.invokeLater(() -> {
			scenariosRunning = false;
			model.refreshOutputTable();
			setScenariosRunning(false);
			progressPanel.setData(Messages.getString("ProgressPanelDone.text"), 100);
			scenarioJPanel.showEditScenario();
			selectCurrentScenarioRunManager();
		});
	}

	private void selectCurrentScenarioRunManager() {
		int index = model.getProject().getScenarioIndexByName(model.getProject().getCurrentScenario());

		if(index != -1) {
			scenarioTable.setRowSelectionInterval(index, index);
		}
	}

	@Override
	public void preProjectRun(final VadereProject project) {
		EventQueue.invokeLater(() -> {
			setScenariosRunning(true);
			progressPanel.setData(Messages.getString("ProgressPanelWorking.text"), 0);
		});
	}

	@Override
	public void preScenarioRun(final Scenario scenario, final int scenariosLeft) {
		EventQueue.invokeLater(() -> {
			model.setScenarioNameLabelString(scenario.getName());
			repaint();
		});
	}

	@Override
	public void postScenarioRun(final Scenario cloneScenario, final int scenarioLeft) {
		EventQueue.invokeLater(() -> {
			replace(cloneScenario, VadereState.INITIALIZED);

			// model.refreshOutputTable();
			// find index of scenario
			int totalScenariosCount = model.getProject().getScenarios().size();
			int doneScenariosCount = totalScenariosCount - scenarioLeft;
			progressPanel.setData(Messages.getString("ProgressPanelWorking.text"), 100 * doneScenariosCount
					/ totalScenariosCount);
			logger.info(String.format("scenario %s finished", cloneScenario.getName()));
		});
	}

	@Override
	public void scenarioStarted(final Scenario cloneScenario, final int scenariosLeft) {
		// take the original!
		EventQueue.invokeLater(() -> {
			replace(cloneScenario, VadereState.RUNNING);
		});
	}

	@Override
	public void scenarioPaused(final Scenario cloneScenario, final int scenariosLeft) {
		// take the original!
		EventQueue.invokeLater(() -> {
			replace(cloneScenario, VadereState.PAUSED);
		});
	}

	@Override
	public void scenarioInterrupted(final Scenario scenario, final int scenariosLeft) {
		EventQueue.invokeLater(() -> {
			replace(scenario, VadereState.INTERRUPTED);
			setScenariosRunning(false);
			selectCurrentScenarioRunManager();
			logger.info(String.format("all running scenarios interrupted"));
		});
	}

	@Override
	public void error(final Scenario scenario, final int scenarioLefts, final Throwable throwable) {
		EventQueue.invokeLater(() -> {
			replace(scenario, VadereState.INTERRUPTED);
			new Thread(
					() -> {
						IOUtils.errorBox(Messages.getString("ProjectView.simulationRunErrorDialog.text") + " " + scenario
								+ ": " + throwable, Messages.getString("ProjectView.simulationRunErrorDialog.title"));
					}).start();
		});
	}

	private void replace(final Scenario scenarioRM, final VadereState state) {
		int rowIndex = model.getScenarioTableModel().indexOfRow(scenarioRM);
		VadereDisplay originalScenario = model.getScenarioTableModel().getValue(rowIndex);
		VadereDisplay dubiousCopy = new VadereDisplay(originalScenario.scenarioRM, state);
		model.getScenarioTableModel().replace(originalScenario, dubiousCopy);
	}

	@Override
	public void preRefresh() {
		EventQueue.invokeLater(() -> {
			outputTable.setEnabled(false);
		});
	}

	@Override
	public void postRefresh() {
		EventQueue.invokeLater(() -> {
			if (!scenariosRunning)
				outputTable.setEnabled(true);
		});
	}

	@Override
	public void projectChanged(final VadereProject project) {
		EventQueue.invokeLater(() -> {
			setTitle();
			model.getProject().addProjectFinishedListener(this);
			model.getProject().addSingleScenarioFinishedListener(this);
			model.getProject().addProjectFinishedListener(scenarioJPanel);
			model.getProject().addProjectFinishedListener(projectRunResultDialog);
		});
	}

	@Override
	public void propertyChanged(final VadereProject project) {
		setTitle();
	}
	// ####################### End Part of the control ##################

	/**
	 * Launch the application.
	 */
	public static void start() {
		EventQueue.invokeLater(() -> {
			try {
				// Set Java L&F from system
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
					| IllegalAccessException e) {
				IOUtils.errorBox("The system look and feel could not be loaded.", "Error setLookAndFeel");
			}
			// show GUI
			ProjectViewModel model = new ProjectViewModel();
			ProjectView frame = new ProjectView(model);
			frame.setProjectSpecificActionsEnabled(false);
			frame.setVisible(true);
			frame.setSize(1200, 800);

			frame.openLastUsedProject(model);
		});
	}

	private void openLastUsedProject(final ProjectViewModel model) {
		String lastUsedProjectPath =
				Preferences.userNodeForPackage(VadereApplication.class).get("last_used_project", null);
		if (lastUsedProjectPath != null) {
			if (Files.exists(Paths.get(lastUsedProjectPath))) {
				ActionLoadProject.loadProjectByPath(model, lastUsedProjectPath);
			}
		}
	}

	public static ProjectView getMainWindow() {
		return mainWindow;
	}

	private void setTitle() {
		if (model.isProjectAvailable()) {
			this.setTitle("Vadere GUI - " + model.getProject().getName());
		} else {
			this.setTitle("Vadere GUI");
		}
	}

	/**
	 * Set the scenarioStarted scenario(s) buttons invisible and the stop button visible.
	 */
	public synchronized void setScenariosRunning(boolean flag) {
		scenariosRunning = flag;
		btnRunAllScenarios.setVisible(!flag);
		btnRunSelectedScenario.setVisible(!flag);
		btnStopRunningScenarios.setVisible(flag);
		btnPauseRunningScenarios.setVisible(flag);
		scenarioTable.setEnabled(!flag);
		scenarioTable.clearSelection();
		outputTable.setEnabled(!flag);
		outputTable.clearSelection();
	}

	/**
	 * Create the main frame.
	 */
	public ProjectView(final ProjectViewModel model) {
		ProjectView.mainWindow = this;

		model.addOutputFileRefreshListener(this);
		model.addProjectChangeListener(this);
		this.model = model;
		projectRunResultDialog = new ProjectRunResultDialog(this, model);

		setTitle("Vadere GUI");
		setBounds(100, 100, 1000, 600);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // DO_NOTHING_ON_CLOSE so that the cancel button on the "save project on exit" question does not cause the windows to close.

		ActionCloseApplication closeApplicationAction =
				new ActionCloseApplication(Messages.getString("ProjectView.mntmExit.text"), model);
		setAcceleratorFromLocalizedShortcut(closeApplicationAction, "ProjectView.mntmExit.shortcut");

		ActionAddScenario addScenarioAction =
				new ActionAddScenario(Messages.getString("ProjectView.mntmNew_1.text"), model);
		setAcceleratorFromLocalizedShortcut(addScenarioAction, "ProjectView.mntmNew_1.shortcut");
		addToProjectSpecificActions(addScenarioAction);

		buildContentPane();
		/*ActionRunSelectedScenariosOnline runSelectedScenariosOnline = new ActionRunSelectedScenariosOnline(
				Messages.getString("ProjectView.runOnline.text"), model, scenarioTable);*/
		buildMenuBar(closeApplicationAction, addScenarioAction);
		buildOutputTablePopup();
		buildScenarioTablePopup(addScenarioAction);
		buildToolBar();
		buildRightSidePanel();

		setScenariosRunning(false);

		this.addWindowListener(new WindowAdapter() { // always ask the user if the current project should be saved before exit.
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				closeApplicationAction.actionPerformed(null);
			}
		});

		pack();
	}

	private void buildMenuBar(ActionCloseApplication closeApplicationAction, ActionAddScenario addScenarioAction) {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu mnFile = new JMenu(Messages.getString("ProjectView.mnFile.text"));
		menuBar.add(mnFile);

		Action createProjectAction =
				new ActionCreateProject(Messages.getString("ProjectView.mntmNewTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(createProjectAction, "ProjectView.mntmNewTestProject.shortcut");
		JMenuItem mntmNewProject = new JMenuItem(createProjectAction);

		Action loadProjectAction =
				new ActionLoadProject(Messages.getString("ProjectView.mntmLoadTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(loadProjectAction, "ProjectView.mntmLoadTestProject.shortcut");
		JMenuItem mntmLoadProject = new JMenuItem(loadProjectAction);

		mntmRecentProjects = new JMenu(Messages.getString("ProjectView.mntmRecentProjects.text"));
		mntmRecentProjects.setEnabled(false);
		updateRecentProjectsMenu();

		Action changeNameAction = new ActionRenameProject(Messages.getString("ProjectView.mntmChangeName.text"), model);
		setAcceleratorFromLocalizedShortcut(changeNameAction, "ProjectView.mntmChangeName.shortcut");
		JMenuItem mntmChangeName = new JMenuItem(changeNameAction);
		addToProjectSpecificActions(changeNameAction);

		mnFile.add(mntmNewProject);
		mnFile.add(mntmLoadProject);
		mnFile.add(mntmRecentProjects);
		mnFile.addSeparator();
		mnFile.add(mntmChangeName);
		mnFile.addSeparator();

		Action saveProjectAction =
				new ActionSaveProject(Messages.getString("ProjectView.mntmSaveTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(saveProjectAction, "ProjectView.mntmSaveTestProject.shortcut");
		JMenuItem mntmSaveProject = new JMenuItem(saveProjectAction);
		addToProjectSpecificActions(saveProjectAction);
		mnFile.add(mntmSaveProject);

		Action saveProjectAsAction = new ActionSaveAsProject(Messages.getString("ProjectView.mntmSaveAs.text"), model);
		setAcceleratorFromLocalizedShortcut(saveProjectAsAction, "ProjectView.mntmSaveAs.shortcut",
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK);

		JMenuItem mntmSaveAs = new JMenuItem(saveProjectAsAction);
		addToProjectSpecificActions(saveProjectAsAction);
		mnFile.add(mntmSaveAs);

		// Checkbox menu item to turn off result dialog of project run.
		mnFile.addSeparator();
		boolean showDialogDefault = Preferences.userNodeForPackage(VadereApplication.class)
				.getBoolean("Project.simulationResult.show", false);
		JCheckBoxMenuItem showResultDialogMenu = new JCheckBoxMenuItem(Messages.getString("ProjectView.mntmSimulationResult.text"), null, showDialogDefault);
		Action showResultDialogMenuAction = new ShowResultDialogAction(Messages.getString("ProjectView.mntmSimulationResult.text"), model, showResultDialogMenu);
		showResultDialogMenu.setAction(showResultDialogMenuAction);
		mnFile.add(showResultDialogMenu);

		JMenuItem mntmExit = new JMenuItem(closeApplicationAction);
		mnFile.addSeparator();
		mnFile.add(mntmExit);

		JMenu mnScenario = new JMenu(Messages.getString("ProjectView.mnScenario.text"));
		menuBar.add(mnScenario);

		JMenuItem mntmNew_1 = new JMenuItem(addScenarioAction);

		Action generateScenarioFromOutputAction = new ActionGenerateScenarioFromOutputFile(
				Messages.getString("ProjectView.mntmGenerateScenario.text"), model);
		addToProjectSpecificActions(generateScenarioFromOutputAction);
		setAcceleratorFromLocalizedShortcut(generateScenarioFromOutputAction,
				"ProjectView.mntmGenerateScenario.shortcut");
		JMenuItem mntmGenerateScenarioFromOutput = new JMenuItem(generateScenarioFromOutputAction);

		mnScenario.add(mntmNew_1);
		mnScenario.add(mntmGenerateScenarioFromOutput);

		//JMenuItem mntmRunOnline = new JMenuItem(runSelectedScenariosOnline);
		//mnScenario.add(mntmRunOnline);

		JMenu mnHelp = new JMenu(Messages.getString("ProjectView.mnHelp.text"));
		menuBar.add(mnHelp);

		Action showAboutAction = new ActionShowAboutDialog(Messages.getString("ProjectView.mntmAbout.text"));
		setAcceleratorFromLocalizedShortcut(showAboutAction, "ProjectView.mntmAbout.shortcut");
		JMenuItem mntmAbout = new JMenuItem(showAboutAction);

		mnHelp.add(mntmAbout);

		JMenu mntmLanguageChoiceMenu = new JMenu(Messages.getString("ProjectView.mntmLanguageChoiceMenu.text"));
		mnHelp.add(mntmLanguageChoiceMenu);
		JRadioButtonMenuItem mntmEnglishLocale =
				new JRadioButtonMenuItem(new AbstractAction(Messages.getString("ProjectView.mntmEnglishLocale.text")) {
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						Messages.changeLanguage(Locale.ENGLISH);
					}
				});
		mntmLanguageChoiceMenu.add(mntmEnglishLocale);
		JRadioButtonMenuItem mntmGermanLocale =
				new JRadioButtonMenuItem(new AbstractAction(Messages.getString("ProjectView.mntmGermanLocale.text")) {
					private static final long serialVersionUID = 1L;
					@Override
					public void actionPerformed(ActionEvent e) {
						Messages.changeLanguage(Locale.GERMAN);
					}
				});
		mntmLanguageChoiceMenu.add(mntmGermanLocale);
		ButtonGroup languageChoicesGroup = new ButtonGroup();
		languageChoicesGroup.add(mntmEnglishLocale);
		languageChoicesGroup.add(mntmGermanLocale);
		if (Language.languageIsGerman())
			mntmGermanLocale.setSelected(true);
		else
			mntmEnglishLocale.setSelected(true);

		JMenuItem mntmReapplyMigration = new JMenuItem(new AbstractAction(Messages.getString("ProjectView.mntmReapplyMigration.text")) {
			private static final long serialVersionUID = 1L;
			@Override
			public void actionPerformed(ActionEvent e) {
				(new ActionLoadProject(Messages.getString("ProjectView.mntmLoadTestProject.text"), model)).loadProject(true);
			}
		});
		mnHelp.add(mntmReapplyMigration);
	}

	private void setAcceleratorFromLocalizedShortcut(Action action, String localizedShortcutKey) {
		setAcceleratorFromLocalizedShortcut(action, localizedShortcutKey,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
	}

	private void setAcceleratorFromLocalizedShortcut(Action action, String localizedShortcutKey, int mask) {
		char shortcut = Messages.getString(localizedShortcutKey).charAt(0);
		KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcut, mask);
		action.putValue(Action.ACCELERATOR_KEY, keyStroke);
	}

	private void buildContentPane() {
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));

		JPanel panel_1 = new JPanel();
		contentPane.add(panel_1, BorderLayout.WEST);
		panel_1.setLayout(new BorderLayout(0, 0));

		panel_1.add(progressPanel, BorderLayout.SOUTH);
		progressPanel.setLayout(new GridLayout(1, 0, 0, 0));

		progressPanel.setData(Messages.getString("ProgressPanelDone.text"), 100);

		OutputTableRenderer outputTableRenderer = new OutputTableRenderer();
		outputTable = model.createOutputTable();

		buildScenarioTable(outputTableRenderer);
		contentPane.add(scenarioTable.getTableHeader(), BorderLayout.CENTER);

		buildOutputTable(outputTableRenderer);
		contentPane.add(outputTable.getTableHeader(), BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.6);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panel_1.add(splitPane, BorderLayout.WEST);

		JScrollPane scrollPanel = new JScrollPane(scenarioTable);
		splitPane.setLeftComponent(scrollPanel);
		scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JScrollPane scrollPanel_output = new JScrollPane(outputTable);
		splitPane.setRightComponent(scrollPanel_output);
		scrollPanel_output.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		panel_1.add(controlPanel, BorderLayout.NORTH);
		FlowLayout fl_controlPanel = (FlowLayout) controlPanel.getLayout();
		fl_controlPanel.setAlignment(FlowLayout.LEFT);
	}

	private void buildScenarioTable(OutputTableRenderer outputTableRenderer) {
		scenarioTable = model.createScenarioTable();
		scenarioTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		scenarioTable.getSelectionModel().addListSelectionListener(new TableSelectionListener(scenarioTable) {
			@Override
			public void onSelect(ListSelectionEvent e) {
				outputTable.clearSelection(); // clear other table's selection
				Player.kill();

				ScenarioBundle bundle = model.getSelectedScenarioBundle();

				model.setCurrentScenario(bundle.getScenario());
                logger.info(String.format("selected scenario '%s'", bundle.getScenario().getName()));
                scenarioJPanel.setScenario(bundle.getScenario(), true);

				outputTableRenderer.setMarkedOutputFiles(bundle.getOutputDirectories());
				outputTable.repaint(); // make cell renderer mark associated outputs
                logger.info("repainted output table");
			}
		});
		scenarioTable.setDefaultRenderer(Object.class, new ScenarioTableRenderer(model));
		scenarioTable.setDeleteAction(new ActionDeleteScenarios(null, model, scenarioTable));
	}

	private void buildOutputTable(OutputTableRenderer outputTableRenderer) {
		outputTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		outputTable.getSelectionModel().addListSelectionListener(new TableSelectionListener(outputTable) {
			@Override
			public void onSelect(ListSelectionEvent e) {
				scenarioTable.clearSelection(); // clear other table's selection
				Player.kill();

				try {
					OutputBundle bundle = model.getSelectedOutputBundle();
					logger.info(String.format("selected output file '%s'", bundle.getDirectory().getName()));

					loadScenarioIntoGui(bundle);
				} catch (IOException ex) {
					logger.error(ex);
				}
			}

			private void loadScenarioIntoGui(OutputBundle bundle) throws IOException {

				Scenario scenarioRM = bundle.getScenarioRM();
				Optional<File> optionalTrajectoryFile = IOUtils
						.getFirstFile(bundle.getDirectory(), IOUtils.TRAJECTORY_FILE_EXTENSION);
				if (optionalTrajectoryFile.isPresent()) {
					scenarioJPanel.loadOutputFileForPostVis(optionalTrajectoryFile.get(), scenarioRM);
				} else {
					scenarioJPanel.loadOutputFileForPostVis(scenarioRM);
					logger.error("could not find trajectory file in : "
							+ bundle.getDirectory().getAbsolutePath());
				}
				model.setCurrentScenario(scenarioRM);
				scenarioJPanel.setScenario(scenarioRM, false);
			}
		});
		outputTable.setDefaultRenderer(Object.class, outputTableRenderer);
		outputTable.setDeleteAction(new ActionDeleteOutputDirectories(null, model, outputTable));
	}

	private void buildOutputTablePopup() {
		ActionDeleteOutputDirectories deleteOutputFileAction = new ActionDeleteOutputDirectories(
				Messages.getString("ProjectView.mntmDelete.text"), model, outputTable);

		JPopupMenu outputListPopupMenu = new JPopupMenu();
		outputListPopupMenu.add(new JMenuItem(deleteOutputFileAction));
		outputListPopupMenu.add(
				new JMenuItem(new ActionRenameOutputFile(Messages.getString("ProjectView.mntmRename.text"), model)));
		outputListPopupMenu.add(new JMenuItem(
				new ActionOutputToScenario(Messages.getString("ProjectView.mntmOutputToSceneario.text"), model)));
		outputListPopupMenu
				.add(new JMenuItem(new ActionRunOutput(Messages.getString("ProjectView.mntmRunOutput.text"), model)));

		JPopupMenu outputListPopupMenuMultiSelect = new JPopupMenu();
		outputListPopupMenuMultiSelect.add(new JMenuItem(deleteOutputFileAction));

		outputTable.setPopupMenus(outputListPopupMenu, outputListPopupMenuMultiSelect);
	}

	private void buildScenarioTablePopup(ActionAddScenario addScenarioAction) {
		ActionDeleteScenarios deleteScenariosAction =
				new ActionDeleteScenarios(Messages.getString("ProjectView.mntmDelete.text"), model, scenarioTable);
		ActionRunSelectedScenarios runSelectedScenarios = new ActionRunSelectedScenarios(
				Messages.getString("ProjectView.mntmRunSelectetTests.text"), model, scenarioTable);
		ActionSeeDiscardChanges seeDiscardChangesAction = new ActionSeeDiscardChanges(
				Messages.getString("ActionSeeDiscardChanges.menu.title"), model, scenarioTable);

		JPopupMenu scenarioListPopupMenu = new JPopupMenu();
		scenarioListPopupMenu.add(new JMenuItem(addScenarioAction));
		scenarioListPopupMenu.add(new JMenuItem(deleteScenariosAction));
		scenarioListPopupMenu.add(new JMenuItem(runSelectedScenarios));
		scenarioListPopupMenu.add(new JMenuItem(seeDiscardChangesAction));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionEditScenarioDescription(Messages.getString("ActionEditScenarioDescription.menu.title"), model)));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionCloneScenario(Messages.getString("ProjectView.mntmClone.text"), model)));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionRenameScenario(Messages.getString("ProjectView.mntmRename.text"), model)));
		/*scenarioListPopupMenu.add(new JMenuItem(
				new ActionConvertScenarioToWMP(Messages.getString("ProjectView.mntmConvertToWMP.text"), model)));*/

		JPopupMenu scenarioListPopupMenuMultiSelect = new JPopupMenu();
		scenarioListPopupMenuMultiSelect.add(new JMenuItem(addScenarioAction));
		scenarioListPopupMenuMultiSelect.add(new JMenuItem(deleteScenariosAction));
		scenarioListPopupMenuMultiSelect.add(new JMenuItem(runSelectedScenarios));
		scenarioListPopupMenuMultiSelect.add(new JMenuItem(seeDiscardChangesAction));

		scenarioTable.setPopupMenus(scenarioListPopupMenu, scenarioListPopupMenuMultiSelect);
	}

	private void buildToolBar() {
		JToolBar toolBar = new JToolBar();
		controlPanel.add(toolBar);

		ButtonGroup mainButtonsGroup = new ButtonGroup();

		Action runAllScenariosAction =
				new ActionRunAllScenarios(Messages.getString("ProjectView.btnRunAllTests.text"), model);
		runAllScenariosAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrows_right_small.png")));
		btnRunAllScenarios = new JButton(runAllScenariosAction);
		toolBar.add(btnRunAllScenarios);
		addToProjectSpecificActions(runAllScenariosAction);
		mainButtonsGroup.add(btnRunAllScenarios);

		ActionRunSelectedScenarios runSelectedScenarios = new ActionRunSelectedScenarios(
				Messages.getString("ProjectView.mntmRunSelectedTests.text"), model, scenarioTable);
		runSelectedScenarios.putValue(Action.SHORT_DESCRIPTION,
				Messages.getString("ProjectView.btnRunSelectedTest.toolTipText"));
		runSelectedScenarios.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrow_right_small.png")));
		btnRunSelectedScenario = new JButton(runSelectedScenarios);
		toolBar.add(btnRunSelectedScenario);
		addToProjectSpecificActions(runSelectedScenarios);
		mainButtonsGroup.add(btnRunSelectedScenario);

		Action interruptScenariosAction =
				new ActionInterruptScenarios(Messages.getString("ProjectView.btnStopRunningTests.text"), model);
		interruptScenariosAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/redcross_small.png")));
		btnStopRunningScenarios = new JButton(interruptScenariosAction);
		toolBar.add(btnStopRunningScenarios);

		ActionPauseScenario pauseScenarioAction =
				new ActionPauseScenario(Messages.getString("ProjectView.btnPauseRunningTests.text"), model);
		pauseScenarioAction.putValue(Action.LONG_DESCRIPTION,
				Messages.getString("ProjectView.btnPauseRunningTests.toolTipText") + " ("
						+ Messages.getString("ProjectView.pauseTests.shortcut").charAt(0) + ")");
		pauseScenarioAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenpause_small.png")));
		btnPauseRunningScenarios = new JButton(pauseScenarioAction);
		toolBar.add(btnPauseRunningScenarios);
		toolBar.getInputMap().put(
				KeyStroke.getKeyStroke(Messages.getString("ProjectView.pauseTests.shortcut").charAt(0)), "pauseTests");
		toolBar.getActionMap().put("pauseTests", pauseScenarioAction);
	}

	private void buildRightSidePanel() {
		JPanel rightSidePanel = new JPanel();
		rightSidePanel.setLayout(new BorderLayout(0, 0));
		contentPane.add(rightSidePanel, BorderLayout.CENTER);

		scenarioNamePanel = new ScenarioNamePanel();
		rightSidePanel.add(scenarioNamePanel, BorderLayout.NORTH);

		scenarioJPanel = new ScenarioPanel(model);
		model.setScenarioNamePanel(scenarioNamePanel); // TODO [priority=low] [task=refactoring] breaking mvc pattern (?) - but I need access to refresh the scenarioName
		model.addProjectChangeListener(scenarioJPanel);
		rightSidePanel.add(scenarioJPanel, BorderLayout.CENTER);
	}

	private void addToProjectSpecificActions(Action action) {
		projectSpecificActions.add(action);
	}

	public void setProjectSpecificActionsEnabled(boolean enabled) {
		for (Action a : projectSpecificActions) {
			a.setEnabled(enabled);
		}
	}

	public void updateRecentProjectsMenu() {
		mntmRecentProjects.removeAll();
		String str = Preferences.userNodeForPackage(VadereApplication.class).get("recent_projects", "");
		boolean hasEntry = false;
		if (str.length() > 0) {
			for (String path : str.split(",")) {
				if (Files.exists(Paths.get(path))) { // show only those that still exist
					if (model.getCurrentProjectPath() != null) {
						if (!model.getCurrentProjectPath().equals(Paths.get(path).getParent().toString())) { // when project loaded, hide that from recent list
							addRecentProjectsMenuItem(path);
							hasEntry = true;
						}
					} else { // no project loaded, show all from recent list
						addRecentProjectsMenuItem(path);
						hasEntry = true;
					}
				}
			}
		}
		mntmRecentProjects.setEnabled(hasEntry);
	}

	private void addRecentProjectsMenuItem(String path) {
		Action loadRecentProjectAction = new ActionLoadRecentProject(path, model);
		mntmRecentProjects.add(new JMenuItem(loadRecentProjectAction));
	}

	public void refreshScenarioNames() {
		model.refreshScenarioNames();
	}

	public void updateScenarioJPanel() {
		scenarioJPanel.updateScenario();
	}
}
