package org.vadere.gui.projectview.view;


import com.formdev.flatlaf.FlatLightLaf;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.postvisualization.control.Player;
import org.vadere.gui.postvisualization.model.ContactData;
import org.vadere.gui.postvisualization.model.TableAerosolCloudData;
import org.vadere.gui.projectview.control.*;
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
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import org.vadere.util.logging.Logger;
import org.vadere.util.opencl.CLUtils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

/**
 * Main view of the Vadere GUI.
 */
public class ProjectView extends JFrame implements ProjectFinishedListener, SingleScenarioFinishedListener,
		IOutputFileRefreshListener, IProjectChangeListener {
	/**
	 * Static variables
	 */
	private static final long serialVersionUID = -2081363246241235943L;
	private static final Logger logger = Logger.getLogger(ProjectView.class);
	/**
	 * Store a reference to the main window as "owner" parameter for dialogs.
	 */
	private static ProjectView mainWindow;

	/**
	 * The model of the {@link ProjectView}
	 */
	private final ProjectViewModel model;

	private final int n_repetitions = 10;

	/**
	 * GUI elements (part of the view) of the {@link ProjectView}
	 *
	 * TODO [priority=medium] [task=refactoring] do the actions have to be stored in member
	 * variables or could it be better to store them locally where they are needed? Some are used in
	 * different methods, maybe only store these as members?
	 */
	private final JPanel contentPane = new JPanel();
	private final JPanel controlPanel = new JPanel(new GridBagLayout());
	private JSplitPane mainSplitPanel = new JSplitPane();
	private VTable scenarioTable;
	private VTable outputTable;
	private JButton btnRunSelectedScenario;
	private JButton btnRunRepeatedlyScenario;
	private JButton btnRunAllScenarios;
	private JButton btnStopRunningScenarios;
	private JButton btnPauseRunningScenarios;
	private JButton btnNextSimulationStep;
	private JButton btnResumeNormalSpeed;
	private JMenu mntmRecentProjects;
	private final ProgressPanel progressPanel = new ProgressPanel();
	private ScenarioPanel scenarioJPanel;
	private ScenarioNamePanel scenarioNamePanel;
	private boolean scenariosRunning = false;
	private final Set<Action> projectSpecificActions = new HashSet<>(); // actions that should only be enabled, when a project is loaded
	private final ProjectRunResultDialog projectRunResultDialog;

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

		if (index != -1) {
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
			logger.info("all running scenarios interrupted");
		});
	}

	@Override
	public void error(final Scenario scenario, final int scenarioLefts, final Throwable throwable) {
		EventQueue.invokeLater(() -> {
			replace(scenario, VadereState.INTERRUPTED);
			new Thread(
					() -> {
						// Use the causing exception (if available) to get a more meaningful error message.
						Throwable causingException = (throwable.getCause() == null) ? throwable : throwable.getCause();

						String errorTextTemplate = "%s: %s\n\n%s";
						String errorText = String.format(errorTextTemplate,
								Messages.getString("ProjectView.simulationRunErrorDialog.text"),
								scenario,
								causingException);
						IOUtils.errorBox(errorText, Messages.getString("ProjectView.simulationRunErrorDialog.title"));
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
	public static void start(String projectPath){
		EventQueue.invokeLater(() -> {
			FlatLightLaf.setup();
			// show GUI
			ProjectViewModel model = new ProjectViewModel();
			ProjectView frame = new ProjectView(model);
			frame.setProjectSpecificActionsEnabled(false);
			frame.setVisible(true);
			frame.setSize(1200, 800);

			frame.setIconImage(Toolkit.getDefaultToolkit()
					.getImage(ProjectView.class.getResource("/icons/vadere-icon.png")));
			if (projectPath.equals("")){
				frame.openLastUsedProject(model);
			} else {
				frame.openProject(model, projectPath);
			}
			checkDependencies(frame);
		});
	}

	private static void checkDependencies(@NotNull final JFrame frame) {
		try {
			if (!CLUtils.isOpenCLSupported()) {
				JOptionPane.showMessageDialog(frame,
						Messages.getString("ProjectView.warning.opencl.text"),
						Messages.getString("ProjectView.warning.opencl.title"),
						JOptionPane.WARNING_MESSAGE);
			}
		} catch (UnsatisfiedLinkError linkError) {
			JOptionPane.showMessageDialog(frame,
					"[LWJGL]: " + linkError.getMessage(),
					Messages.getString("ProjectView.warning.lwjgl.title"),
					JOptionPane.WARNING_MESSAGE);
		}
	}

	private void openLastUsedProject(final ProjectViewModel model) {
		String lastUsedProjectPath =
				VadereConfig.getConfig().getString("History.lastUsedProject");
		if (lastUsedProjectPath != null && !lastUsedProjectPath.isBlank()) {
			if (Files.exists(Paths.get(lastUsedProjectPath))) {
				ActionLoadProject.loadProjectByPath(model, lastUsedProjectPath);
			}
		}
	}

	private void openProject(final  ProjectViewModel model, String projectPath) {
		if (Files.exists(Paths.get(projectPath))) {
			ActionLoadProject.loadProjectByPath(model, projectPath);
		} else {
			IOUtils.errorBox("No project under "+ projectPath, "Project not found");
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
		btnRunRepeatedlyScenario.setVisible(!flag);
		btnStopRunningScenarios.setVisible(flag);
		btnPauseRunningScenarios.setVisible(flag);
		btnNextSimulationStep.setVisible(flag);
		btnResumeNormalSpeed.setVisible(flag);
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

		buildMenuBar(closeApplicationAction, addScenarioAction);

		buildContentPane();
		buildOutputTablePopup();
		buildScenarioTablePopup(addScenarioAction);
		buildToolBar();

		setScenariosRunning(false);

		this.addWindowListener(new WindowAdapter() { // always ask the user if the current project should be saved before exit.
			@Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {
				closeApplicationAction.actionPerformed(null);
			}
		});
		pack();
	}

	private void buildKeyboardShortcuts(ActionPauseScenario pauseScenarioAction, Action interruptScenariosAction) {
		addKeyboardShortcut("SPACE", "Typed Space", btnPauseRunningScenarios.getAction());
		addKeyboardShortcut("BACK_SPACE", "Typed Backspace", btnStopRunningScenarios.getAction());
	}

	private void addKeyboardShortcut(String key, String actionKey, Action action) {
		controlPanel.getInputMap().put(KeyStroke.getKeyStroke(key), actionKey);
		controlPanel.getActionMap().put(actionKey, action);
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
		mntmRecentProjects.setEnabled(true);
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
		JCheckBoxMenuItem showResultDialogMenu = new JCheckBoxMenuItem(Messages.getString("ProjectView.mntmSimulationResult.text"), null, model.getShowSimulationResultDialog());
		Action showResultDialogMenuAction = new ShowResultDialogAction(Messages.getString("ProjectView.mntmSimulationResult.text"), model, showResultDialogMenu);
		showResultDialogMenu.setAction(showResultDialogMenuAction);
		mnFile.add(showResultDialogMenu);

		// Checkbox menu item to turn off Scenario Checker during  topography creation
		JCheckBoxMenuItem toggleScenarioCheckerDialogMenu = new JCheckBoxMenuItem(Messages.getString("ProjectView.btnToggleScenarioChecker.text"), null, model.getShowSimulationResultDialog());
		Action toggleScenarioCheckerMenuAction = new ToggleScenarioManagerAction(Messages.getString("ProjectView.btnToggleScenarioChecker.text"), model, toggleScenarioCheckerDialogMenu);
		toggleScenarioCheckerDialogMenu.setAction(toggleScenarioCheckerMenuAction);
		mnFile.add(toggleScenarioCheckerDialogMenu);


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

		JMenu mnHelp = new JMenu(Messages.getString("ProjectView.mnHelp.text"));
		menuBar.add(mnHelp);

		Action showAboutAction = new ActionShowAboutDialog(Messages.getString("ProjectView.mntmAbout.text"));
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
		if (Messages.languageIsGerman())
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
		panel_1.setLayout(new BorderLayout(0, 0));

		panel_1.add(progressPanel, BorderLayout.SOUTH);
		progressPanel.setLayout(new GridLayout(1, 0, 0, 0));

		progressPanel.setData(Messages.getString("ProgressPanelDone.text"), 100);

		OutputTableRenderer outputTableRenderer = new OutputTableRenderer();
		outputTable = model.createOutputTable();

		buildScenarioTable(outputTableRenderer);

		buildOutputTable(outputTableRenderer);

		JSplitPane splitPane = new JSplitPane();
		JPanel panelContainer = new JPanel(new BorderLayout());
		panelContainer.add(splitPane);
		splitPane.setResizeWeight(0.7);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		panel_1.add(splitPane, BorderLayout.CENTER);

		JScrollPane scrollPanel = new JScrollPane(scenarioTable);
		splitPane.setLeftComponent(scrollPanel);
		scrollPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		JScrollPane scrollPanel_output = new JScrollPane(outputTable);
		splitPane.setRightComponent(scrollPanel_output);
		scrollPanel_output.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		panel_1.add(controlPanel, BorderLayout.NORTH);
		JPanel panel_2 = buildRightSidePanel();

		mainSplitPanel = new JSplitPane();
		((BasicSplitPaneUI) mainSplitPanel.getUI()).getDivider().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					mainSplitPanel.setDividerLocation(scenarioTable.getSize().width + 5);
				}
			}
		});
		mainSplitPanel.setResizeWeight(0.2);
		mainSplitPanel.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		panel_1.setMinimumSize(new Dimension(1, 1));
		panel_2.setMinimumSize(new Dimension(1, 1));
		mainSplitPanel.setLeftComponent(panel_1);
		mainSplitPanel.setRightComponent(panel_2);
		mainSplitPanel.resetToPreferredSizes();
		contentPane.add(mainSplitPanel, BorderLayout.CENTER);
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
				Optional<File> trajectoryFile = IOUtils
						.getFirstFile(bundle.getDirectory(), IOUtils.TRAJECTORY_FILE_EXTENSION);
				
				File[] txtFiles = IOUtils.getFileList(bundle.getDirectory(), ".txt");
				List<String> tableNames = new ArrayList<>(Arrays.asList(ContactData.TABLE_NAME, TableAerosolCloudData.TABLE_NAME));
				HashMap<String, File> optionalPostVisFiles = new HashMap<>();
				for (String name : tableNames) {
					for (File f : txtFiles) {
						if (f.getName().contains(name)) {
							optionalPostVisFiles.put(name, f);
						}
					}
				}

				if (trajectoryFile.isPresent()) {
					if (optionalPostVisFiles.size() > 0) {
						scenarioJPanel.loadOutputFileForPostVis(trajectoryFile.get(), optionalPostVisFiles, scenarioRM);
					} else {
						scenarioJPanel.loadOutputFileForPostVis(trajectoryFile.get(), scenarioRM);
					}
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
		outputListPopupMenu
				.add(new JMenuItem(new ActionOpenInExplorer(Messages.getString("ProjectView.OpenInExplorer.text"), model)));

		JMenu copyPath = new JMenu(Messages.getString("ProjectView.mntmCopyOutputDir.text"));
		outputTable.getSelectionModel().addListSelectionListener(new TableSelectionListener(outputTable) {
			@Override
			public void onSelect(ListSelectionEvent e) {
				try {
					OutputBundle bundle = model.getSelectedOutputBundle();
					File outDir = bundle.getDirectory();
					copyPath.removeAll();
					copyPath.add(new JMenuItem(
							new ActionToClipboard(outDir.getName() + "/", outDir.getAbsolutePath()))
					);
					File[] children = outDir.listFiles();
					if (children != null) {
						for (File file : children) {
							String name = file.isDirectory() ? "---*" + file.getName() + "/" : "---*" + file.getName();
							copyPath.add(new JMenuItem(
									new ActionToClipboard(name, file.getAbsolutePath()))
							);
						}
					}

				} catch (IOException ex) {
					logger.error(ex);
				}
			}
		});

		outputListPopupMenu.add(copyPath);

		JPopupMenu outputListPopupMenuMultiSelect = new JPopupMenu();
		outputListPopupMenuMultiSelect.add(new JMenuItem(deleteOutputFileAction));

		outputTable.setPopupMenus(outputListPopupMenu, outputListPopupMenuMultiSelect);
	}

	private void buildScenarioTablePopup(ActionAddScenario addScenarioAction) {
		ActionDeleteScenarios deleteScenariosAction =
				new ActionDeleteScenarios(Messages.getString("ProjectView.mntmDelete.text"), model, scenarioTable);
		ActionRunSelectedScenarios runSelectedScenarios = new ActionRunSelectedScenarios(
				Messages.getString("ProjectView.mntmRunSelectedTests.text"), model, scenarioTable);
		ActionRunRepeatedlyScenarios runRepeatedlyScenarios = new ActionRunRepeatedlyScenarios(
				Messages.getString("ProjectView.mntmRunRepeatedlyTests.text"), model, scenarioTable, n_repetitions);
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
		toolBar.setLayout(new FlowLayout(FlowLayout.CENTER));
		controlPanel.add(toolBar,initializeConstraints());

		ButtonGroup mainButtonsGroup = new ButtonGroup();

		Action runAllScenariosAction =
				new ActionRunAllScenarios(Messages.getString("ProjectView.btnRunAllTests.text"), model);
		runAllScenariosAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrows_right_small.png")));
		btnRunAllScenarios = new JButton(runAllScenariosAction);
		btnRunAllScenarios.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunAllScenarios.setHorizontalTextPosition(SwingConstants.CENTER);
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
		btnRunSelectedScenario.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunSelectedScenario.setHorizontalTextPosition(SwingConstants.CENTER);
		toolBar.add(btnRunSelectedScenario);
		addToProjectSpecificActions(runSelectedScenarios);
		mainButtonsGroup.add(btnRunSelectedScenario);

		ActionRunRepeatedlyScenarios runRepeatedlyScenarios = new ActionRunRepeatedlyScenarios(
				Messages.getString("ProjectView.mntmRunRepeatedlyTests.text"), model, scenarioTable, n_repetitions);
		runRepeatedlyScenarios.putValue(Action.SHORT_DESCRIPTION,
				Messages.getString("ProjectView.btnRunRepeatedlyTest.toolTipText"));
		runRepeatedlyScenarios.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrow_right_small.png")));
		btnRunRepeatedlyScenario = new JButton(runRepeatedlyScenarios);
		btnRunRepeatedlyScenario.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunRepeatedlyScenario.setHorizontalTextPosition(SwingConstants.CENTER);
		toolBar.add(btnRunRepeatedlyScenario);
		addToProjectSpecificActions(runRepeatedlyScenarios);
		mainButtonsGroup.add(btnRunRepeatedlyScenario);

		Action interruptScenariosAction =
				new ActionInterruptScenarios(Messages.getString("ProjectView.btnStopRunningTests.text"), model);
		interruptScenariosAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/redcross_small.png")));
		btnStopRunningScenarios = new JButton(interruptScenariosAction);
		toolBar.add(btnStopRunningScenarios);

		ActionResumeNormalSpeed resumeNormalSpeedAction =
				new ActionResumeNormalSpeed(Messages.getString("ProjectView.btnResumeNormalSpeed.text"), model);
		resumeNormalSpeedAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrow_right_small.png")));
		btnResumeNormalSpeed = new JButton(resumeNormalSpeedAction);
		toolBar.add(btnResumeNormalSpeed);

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

		ActionNextTimeStep nextTimeStepAction =
				new ActionNextTimeStep(Messages.getString("ProjectView.btnNextSimulationStep"), model);
		nextTimeStepAction.putValue(Action.LONG_DESCRIPTION, "Next Step");
		nextTimeStepAction.putValue(Action.LARGE_ICON_KEY,
				new ImageIcon(ProjectView.class.getResource("/icons/greenarrow_step.png")));
		btnNextSimulationStep = new JButton(nextTimeStepAction);
		toolBar.add(btnNextSimulationStep);

		buildKeyboardShortcuts(pauseScenarioAction, interruptScenariosAction);
	}

	private JPanel buildRightSidePanel() {
		JPanel rightSidePanel = new JPanel();
		rightSidePanel.setLayout(new BorderLayout(0, 0));
		contentPane.add(rightSidePanel, BorderLayout.CENTER);

		scenarioNamePanel = new ScenarioNamePanel();
		rightSidePanel.add(scenarioNamePanel, BorderLayout.NORTH);

		scenarioJPanel = new ScenarioPanel(model);
		model.setScenarioNamePanel(scenarioNamePanel); // TODO [priority=low] [task=refactoring] breaking mvc pattern (?) - but I need access to refresh the scenarioName
		model.addProjectChangeListener(scenarioJPanel);
		rightSidePanel.add(scenarioJPanel, BorderLayout.CENTER);
		return rightSidePanel;
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
		java.util.List<String> recentProjectPaths = VadereConfig.getConfig().getList(String.class, "History.recentProjects", Collections.EMPTY_LIST);
		boolean hasEntry = false;
		for (String path : recentProjectPaths) {
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

	@Override
	public void validate() {
		int max_div = scenarioTable.getSize().width + 25;
		super.validate();
		if (mainSplitPanel.getDividerLocation() > max_div) {
			mainSplitPanel.setDividerLocation(max_div);
		}
	}

	private static GridBagConstraints initializeConstraints() {
		var gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER;
		gbc.gridheight = GridBagConstraints.REMAINDER;
		gbc.anchor = GridBagConstraints.PAGE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.weightx = 1;
		return gbc;
	}
}
