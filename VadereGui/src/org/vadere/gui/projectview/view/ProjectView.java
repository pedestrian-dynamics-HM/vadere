package org.vadere.gui.projectview.view;


import com.formdev.flatlaf.FlatLightLaf;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.utils.Localization;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.postvisualization.control.Player;
import org.vadere.gui.postvisualization.model.AirFlowData;
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
	private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));
    private static final int SMALL_ICON_SIZE = Math.max(16, (int)(ICON_SIZE * 0.7));
    private static final int MIN_SPLIT_PANEL_DIVIDER_WIDTH = 30;
    private static final int SPLIT_PANEL_DIVIDER_SAFETY_MARGIN = 25;
	private final Resources RESOURCE = Resources.getInstance("global");
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
    private JToolBar toolBar;
    private JButton overflowButton;
    private JPopupMenu overflowMenu;
    private final List<JButton> runButtons = new ArrayList<>();
    private final List<JButton> runtimeButtons = new ArrayList<>();
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
			progressPanel.setData(Localization.getString("ProgressPanelDone.text"), 100);
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
			progressPanel.setData(Localization.getString("ProgressPanelWorking.text"), 0);
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
			progressPanel.setData(Localization.getString("ProgressPanelWorking.text"), 100 * doneScenariosCount
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
								Localization.getString("ProjectView.simulationRunErrorDialog.text"),
								scenario,
								causingException);
						IOUtils.errorBox(errorText, Localization.getString("ProjectView.simulationRunErrorDialog.title"));
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

    private static void showSuppressibleWarning(JFrame frame, String text, String title, String suppressConfigKey) {
        if (VadereConfig.getConfig().getBoolean(suppressConfigKey, false)) {
            return;
        }

        JTextArea messageArea = new JTextArea(text);
        messageArea.setEditable(false);
        messageArea.setOpaque(false);
        messageArea.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JCheckBox checkToNotShow = new JCheckBox(Localization.getString("ProjectView.warning.checkToNotShow.text"));

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(messageArea, BorderLayout.CENTER);
        panel.add(checkToNotShow, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(frame, panel, title, JOptionPane.WARNING_MESSAGE);

        if (checkToNotShow.isSelected()) {
            VadereConfig.getConfig().setProperty(suppressConfigKey, true);
        }
    }

    private static void checkDependencies(@NotNull final JFrame frame) {
        try {
            if (!CLUtils.isOpenCLSupported()) {
                showSuppressibleWarning(frame,
                        Localization.getString("ProjectView.warning.opencl.text"),
                        Localization.getString("ProjectView.warning.opencl.title"),
                        "Gui.suppressOpenClWarning");
            }
        } catch (UnsatisfiedLinkError linkError) {
            JOptionPane.showMessageDialog(frame,
                    "[LWJGL]: " + linkError.getMessage(),
                    Localization.getString("ProjectView.warning.lwjgl.title"),
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
        SwingUtilities.invokeLater(this::updateOverflowToolbar);
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
				new ActionCloseApplication(Localization.getString("ProjectView.mntmExit.text"), model);
		setAcceleratorFromLocalizedShortcut(closeApplicationAction, "ProjectView.mntmExit.shortcut");
		ActionAddScenario addScenarioAction =
				new ActionAddScenario(Localization.getString("ProjectView.mntmNew_1.text"), model);
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

		JMenu mnFile = new JMenu(Localization.getString("ProjectView.mnFile.text"));
		menuBar.add(mnFile);

		Action createProjectAction =
				new ActionCreateProject(Localization.getString("ProjectView.mntmNewTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(createProjectAction, "ProjectView.mntmNewTestProject.shortcut");
		JMenuItem mntmNewProject = new JMenuItem(createProjectAction);

		Action loadProjectAction =
				new ActionLoadProject(Localization.getString("ProjectView.mntmLoadTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(loadProjectAction, "ProjectView.mntmLoadTestProject.shortcut");
		JMenuItem mntmLoadProject = new JMenuItem(loadProjectAction);

		mntmRecentProjects = new JMenu(Localization.getString("ProjectView.mntmRecentProjects.text"));
		mntmRecentProjects.setEnabled(true);
		updateRecentProjectsMenu();

		Action changeNameAction = new ActionRenameProject(Localization.getString("ProjectView.mntmChangeName.text"), model);
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
				new ActionSaveProject(Localization.getString("ProjectView.mntmSaveTestProject.text"), model);
		setAcceleratorFromLocalizedShortcut(saveProjectAction, "ProjectView.mntmSaveTestProject.shortcut");
		JMenuItem mntmSaveProject = new JMenuItem(saveProjectAction);
		addToProjectSpecificActions(saveProjectAction);
		mnFile.add(mntmSaveProject);

		Action saveProjectAsAction = new ActionSaveAsProject(Localization.getString("ProjectView.mntmSaveAs.text"), model);
		setAcceleratorFromLocalizedShortcut(saveProjectAsAction, "ProjectView.mntmSaveAs.shortcut",
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() | KeyEvent.SHIFT_DOWN_MASK);

		JMenuItem mntmSaveAs = new JMenuItem(saveProjectAsAction);
		addToProjectSpecificActions(saveProjectAsAction);
		mnFile.add(mntmSaveAs);

		// Checkbox menu item to turn off result dialog of project run.
		mnFile.addSeparator();
		JCheckBoxMenuItem showResultDialogMenu = new JCheckBoxMenuItem(Localization.getString("ProjectView.mntmSimulationResult.text"), null, model.getShowSimulationResultDialog());
		Action showResultDialogMenuAction = new ShowResultDialogAction(Localization.getString("ProjectView.mntmSimulationResult.text"), model, showResultDialogMenu);
		showResultDialogMenu.setAction(showResultDialogMenuAction);
		mnFile.add(showResultDialogMenu);

		// Checkbox menu item to turn off Scenario Checker during  topography creation
		JCheckBoxMenuItem toggleScenarioCheckerDialogMenu = new JCheckBoxMenuItem(Localization.getString("ProjectView.btnToggleScenarioChecker.text"), null, model.getShowSimulationResultDialog());
		Action toggleScenarioCheckerMenuAction = new ToggleScenarioManagerAction(Localization.getString("ProjectView.btnToggleScenarioChecker.text"), model, toggleScenarioCheckerDialogMenu);
		toggleScenarioCheckerDialogMenu.setAction(toggleScenarioCheckerMenuAction);
		mnFile.add(toggleScenarioCheckerDialogMenu);


		JMenuItem mntmExit = new JMenuItem(closeApplicationAction);
		mnFile.addSeparator();
		mnFile.add(mntmExit);

		JMenu mnScenario = new JMenu(Localization.getString("ProjectView.mnScenario.text"));
		menuBar.add(mnScenario);

		JMenuItem mntmNew_1 = new JMenuItem(addScenarioAction);

		Action generateScenarioFromOutputAction = new ActionGenerateScenarioFromOutputFile(
				Localization.getString("ProjectView.mntmGenerateScenario.text"), model);
		addToProjectSpecificActions(generateScenarioFromOutputAction);
		setAcceleratorFromLocalizedShortcut(generateScenarioFromOutputAction,
				"ProjectView.mntmGenerateScenario.shortcut");
		JMenuItem mntmGenerateScenarioFromOutput = new JMenuItem(generateScenarioFromOutputAction);

		mnScenario.add(mntmNew_1);
		mnScenario.add(mntmGenerateScenarioFromOutput);

		JMenu mnHelp = new JMenu(Localization.getString("ProjectView.mnHelp.text"));
		menuBar.add(mnHelp);

		Action showAboutAction = new ActionShowAboutDialog(Localization.getString("ProjectView.mntmAbout.text"));
		JMenuItem mntmAbout = new JMenuItem(showAboutAction);

		mnHelp.add(mntmAbout);

		JMenu mntmLanguageChoiceMenu = new JMenu(Localization.getString("ProjectView.mntmLanguageChoiceMenu.text"));
		mnHelp.add(mntmLanguageChoiceMenu);
		JRadioButtonMenuItem mntmEnglishLocale =
				new JRadioButtonMenuItem(new AbstractAction(Localization.getString("ProjectView.mntmEnglishLocale.text")) {
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						Localization.changeLanguage(Locale.ENGLISH);
					}
				});
		mntmLanguageChoiceMenu.add(mntmEnglishLocale);
		JRadioButtonMenuItem mntmGermanLocale =
				new JRadioButtonMenuItem(new AbstractAction(Localization.getString("ProjectView.mntmGermanLocale.text")) {
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						Localization.changeLanguage(Locale.GERMANY);
					}
				});
		mntmLanguageChoiceMenu.add(mntmGermanLocale);
		ButtonGroup languageChoicesGroup = new ButtonGroup();
		languageChoicesGroup.add(mntmEnglishLocale);
		languageChoicesGroup.add(mntmGermanLocale);
		if (Localization.languageIsGerman())
			mntmGermanLocale.setSelected(true);
		else
			mntmEnglishLocale.setSelected(true);

		JMenuItem mntmReapplyMigration = new JMenuItem(new AbstractAction(Localization.getString("ProjectView.mntmReapplyMigration.text")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				(new ActionLoadProject(Localization.getString("ProjectView.mntmLoadTestProject.text"), model)).loadProject(true);
			}
		});
		mnHelp.add(mntmReapplyMigration);
	}

	private void setAcceleratorFromLocalizedShortcut(Action action, String localizedShortcutKey) {
		setAcceleratorFromLocalizedShortcut(action, localizedShortcutKey,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
	}

	private void setAcceleratorFromLocalizedShortcut(Action action, String localizedShortcutKey, int mask) {
		char shortcut = Localization.getString(localizedShortcutKey).charAt(0);
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

		progressPanel.setData(Localization.getString("ProgressPanelDone.text"), 100);

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

        SwingUtilities.invokeLater(this::enforceSplitPanelDividerMinWidth);
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
				List<String> tableNames = new ArrayList<>(Arrays.asList(ContactData.TABLE_NAME, TableAerosolCloudData.TABLE_NAME, AirFlowData.TABLE_NAME));
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
				Localization.getString("ProjectView.mntmDelete.text"), model, outputTable);

		JPopupMenu outputListPopupMenu = new JPopupMenu();
		outputListPopupMenu.add(new JMenuItem(deleteOutputFileAction));
		outputListPopupMenu.add(
				new JMenuItem(new ActionRenameOutputFile(Localization.getString("ProjectView.mntmRename.text"), model)));
		outputListPopupMenu.add(new JMenuItem(
				new ActionOutputToScenario(Localization.getString("ProjectView.mntmOutputToSceneario.text"), model)));
		outputListPopupMenu
				.add(new JMenuItem(new ActionOpenInExplorer(Localization.getString("ProjectView.OpenInExplorer.text"), model)));

		JMenu copyPath = new JMenu(Localization.getString("ProjectView.mntmCopyOutputDir.text"));
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
				new ActionDeleteScenarios(Localization.getString("ProjectView.mntmDelete.text"), model, scenarioTable);
		ActionRunSelectedScenarios runSelectedScenarios = new ActionRunSelectedScenarios(
				Localization.getString("ProjectView.mntmRunSelectedTests.text"), model, scenarioTable);
		ActionRunRepeatedlyScenarios runRepeatedlyScenarios = new ActionRunRepeatedlyScenarios(
				Localization.getString("ProjectView.mntmRunRepeatedlyTests.text"), model, scenarioTable, n_repetitions);
		ActionSeeDiscardChanges seeDiscardChangesAction = new ActionSeeDiscardChanges(
				Localization.getString("ActionSeeDiscardChanges.menu.title"), model, scenarioTable);

		JPopupMenu scenarioListPopupMenu = new JPopupMenu();
		scenarioListPopupMenu.add(new JMenuItem(addScenarioAction));
		scenarioListPopupMenu.add(new JMenuItem(deleteScenariosAction));
		scenarioListPopupMenu.add(new JMenuItem(runSelectedScenarios));
		scenarioListPopupMenu.add(new JMenuItem(seeDiscardChangesAction));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionEditScenarioDescription(Localization.getString("ActionEditScenarioDescription.menu.title"), model)));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionCloneScenario(Localization.getString("ProjectView.mntmClone.text"), model)));
		scenarioListPopupMenu.add(new JMenuItem(
				new ActionRenameScenario(Localization.getString("ProjectView.mntmRename.text"), model)));
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
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setLayout(new BoxLayout(toolBar, BoxLayout.X_AXIS));
        controlPanel.add(toolBar, initializeConstraints());

		ButtonGroup mainButtonsGroup = new ButtonGroup();

		ActionRunSelectedScenarios runSelectedScenarios = new ActionRunSelectedScenarios(
				Localization.getString("ProjectView.mntmRunSelectedTests.text"), model, scenarioTable);
		runSelectedScenarios.putValue(Action.SHORT_DESCRIPTION,
				Localization.getString("ProjectView.btnRunSelectedTest.toolTipText"));
		runSelectedScenarios.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_play", ICON_SIZE, ICON_SIZE));
        runSelectedScenarios.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_play", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnRunSelectedScenario = new JButton(runSelectedScenarios);
		btnRunSelectedScenario.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunSelectedScenario.setHorizontalTextPosition(SwingConstants.CENTER);
		toolBar.add(btnRunSelectedScenario);
        runButtons.add(btnRunSelectedScenario);
        addToProjectSpecificActions(runSelectedScenarios);
        mainButtonsGroup.add(btnRunSelectedScenario);

		Action runAllScenariosAction =
				new ActionRunAllScenarios(Localization.getString("ProjectView.btnRunAllTests.text"), model);
		runAllScenariosAction.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_multiplay", ICON_SIZE, ICON_SIZE));
        runAllScenariosAction.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_multiplay", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnRunAllScenarios = new JButton(runAllScenariosAction);
		btnRunAllScenarios.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunAllScenarios.setHorizontalTextPosition(SwingConstants.CENTER);
		toolBar.add(btnRunAllScenarios);
        runButtons.add(btnRunAllScenarios);
		addToProjectSpecificActions(runAllScenariosAction);
		mainButtonsGroup.add(btnRunAllScenarios);

		ActionRunRepeatedlyScenarios runRepeatedlyScenarios = new ActionRunRepeatedlyScenarios(
				Localization.getString("ProjectView.mntmRunRepeatedlyTests.text"), model, scenarioTable, n_repetitions);
		runRepeatedlyScenarios.putValue(Action.SHORT_DESCRIPTION,
				Localization.getString("ProjectView.btnRunRepeatedlyTest.toolTipText"));
		runRepeatedlyScenarios.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_multiplay", ICON_SIZE, ICON_SIZE));
        runRepeatedlyScenarios.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_multiplay", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnRunRepeatedlyScenario = new JButton(runRepeatedlyScenarios);
		btnRunRepeatedlyScenario.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnRunRepeatedlyScenario.setHorizontalTextPosition(SwingConstants.CENTER);
		toolBar.add(btnRunRepeatedlyScenario);
        runButtons.add(btnRunRepeatedlyScenario);
		addToProjectSpecificActions(runRepeatedlyScenarios);
		mainButtonsGroup.add(btnRunRepeatedlyScenario);

		Action interruptScenariosAction =
				new ActionInterruptScenarios(Localization.getString("ProjectView.btnStopRunningTests.text"), model);
		interruptScenariosAction.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_stop", ICON_SIZE, ICON_SIZE));
        interruptScenariosAction.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_stop", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnStopRunningScenarios = new JButton(interruptScenariosAction);
		toolBar.add(btnStopRunningScenarios);
        runtimeButtons.add(btnStopRunningScenarios);

		ActionResumeNormalSpeed resumeNormalSpeedAction =
				new ActionResumeNormalSpeed(Localization.getString("ProjectView.btnResumeNormalSpeed.text"), model);
		resumeNormalSpeedAction.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_play", ICON_SIZE, ICON_SIZE));
        resumeNormalSpeedAction.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_play", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnResumeNormalSpeed = new JButton(resumeNormalSpeedAction);
		toolBar.add(btnResumeNormalSpeed);
        runtimeButtons.add(btnResumeNormalSpeed);

		ActionPauseScenario pauseScenarioAction =
				new ActionPauseScenario(Localization.getString("ProjectView.btnPauseRunningTests.text"), model);
		pauseScenarioAction.putValue(Action.LONG_DESCRIPTION,
				Localization.getString("ProjectView.btnPauseRunningTests.toolTipText") + " ("
						+ Localization.getString("ProjectView.pauseTests.shortcut").charAt(0) + ")");
		pauseScenarioAction.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_pause", ICON_SIZE, ICON_SIZE));
        pauseScenarioAction.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_pause", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnPauseRunningScenarios = new JButton(pauseScenarioAction);
		toolBar.add(btnPauseRunningScenarios);
        runtimeButtons.add(btnPauseRunningScenarios);
		toolBar.getInputMap().put(
				KeyStroke.getKeyStroke(Localization.getString("ProjectView.pauseTests.shortcut").charAt(0)), "pauseTests");
		toolBar.getActionMap().put("pauseTests", pauseScenarioAction);

		ActionNextTimeStep nextTimeStepAction =
				new ActionNextTimeStep(Localization.getString("ProjectView.btnNextSimulationStep"), model);
		nextTimeStepAction.putValue(Action.LONG_DESCRIPTION, "Next Step");
		nextTimeStepAction.putValue(Action.LARGE_ICON_KEY,
				RESOURCE.getIconSVG("transport_skip", ICON_SIZE, ICON_SIZE));
        nextTimeStepAction.putValue(Action.SMALL_ICON,
                RESOURCE.getIconSVG("transport_skip", SMALL_ICON_SIZE, SMALL_ICON_SIZE));
		btnNextSimulationStep = new JButton(nextTimeStepAction);
		toolBar.add(btnNextSimulationStep);
        runtimeButtons.add(btnNextSimulationStep);

		buildKeyboardShortcuts(pauseScenarioAction, interruptScenariosAction);

        toolBar.add(Box.createHorizontalGlue());
        overflowMenu = new JPopupMenu();
        overflowButton = createOverflowButton();
        toolBar.add(overflowButton);

        toolBar.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateOverflowToolbar();
            }
        });
        SwingUtilities.invokeLater(this::updateOverflowToolbar);
	}

    private JButton createOverflowButton() {
        JButton button = new JButton("⋮");
        button.setFont(
                button.getFont().deriveFont(Font.BOLD, button.getFont().getSize2D() + 16f)
        );
        button.setMargin(new Insets(0, 8, 0, 8));
        button.setPreferredSize(new Dimension(ICON_SIZE + 10, ICON_SIZE + 10));
        button.setAlignmentY(Component.CENTER_ALIGNMENT);
        button.setFocusable(false);
        button.setVisible(false);

        button.addActionListener(e -> overflowMenu.show(button, 0, button.getHeight()));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isVisible() && overflowMenu.getComponentCount() > 0) {
                    overflowMenu.show(button, 0, button.getHeight());
                }
            }
        });
        return button;
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
        super.validate();
        enforceSplitPanelDividerMaxWidth();
        SwingUtilities.invokeLater(this::updateOverflowToolbar);
	}

    private void enforceSplitPanelDividerMaxWidth() {
        int toolbarWidth = (Objects.nonNull(toolBar)) ? measureToolbarVisibleWidth() : 0;
        int dividerMaxWidth = Math.max(scenarioTable.getSize().width, toolbarWidth) + SPLIT_PANEL_DIVIDER_SAFETY_MARGIN;

        if (mainSplitPanel.getDividerLocation() > dividerMaxWidth) {
            mainSplitPanel.setDividerLocation(dividerMaxWidth);
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

    private int measureToolbarVisibleWidth() {
        if (Objects.isNull(toolBar)) {;
            return 0;
        }

        int width = 0;
        for (Component component : toolBar.getComponents()) {
            if (!component.isVisible()) {
                continue;
            }
            if (component instanceof Box.Filler) {
                continue;
            }
            width += component.getPreferredSize().width;
        }

        Insets insets = toolBar.getInsets();
        return width + insets.left + insets.right;
    }

    private void enforceSplitPanelDividerMinWidth() {
        if (Objects.isNull(toolBar)) {
            return;
        }

        int requiredWidth = measureToolbarVisibleWidth() + MIN_SPLIT_PANEL_DIVIDER_WIDTH;
        int currentWidth = mainSplitPanel.getDividerLocation();
        if (currentWidth < requiredWidth) {
            mainSplitPanel.setDividerLocation(requiredWidth);
        }
    }

    private void updateOverflowToolbar() {
        if (Objects.isNull(toolBar)) {
            return;
        }

        List<JButton> activeButtons = scenariosRunning ? runtimeButtons : runButtons;
        if (activeButtons.isEmpty()) {
            return;
        }

        resetOverflowToolbarState(activeButtons);

        int toolBarWidth = calculateToolbarAvailableWidth();
        if (toolBarWidth <= 0) {
            return;
        }

        int requiredWidth = calculateOverflowToolbarRequiredWidth();
        if (requiredWidth <= toolBarWidth) {
            updateToolbarLayout();
            return;
        }

        List<JButton> hiddenButtons = setHiddenButtons(toolBarWidth, activeButtons, requiredWidth);
        updateOverflowMenu(hiddenButtons);
        updateToolbarLayout();
    }

    private void resetOverflowToolbarState(List<JButton> activeButtons) {
        for (JButton button : activeButtons) {
            button.setVisible(true);
        }
        overflowMenu.removeAll();
        overflowButton.setVisible(false);
    }

    private int calculateToolbarAvailableWidth() {
        int toolBarWidth = toolBar.getWidth();
        if (toolBarWidth <= 0) {
            return 0;
        }
        Insets insets = toolBar.getInsets();
        toolBarWidth -= (insets.left + insets.right);
        return toolBarWidth;
    }

    private int calculateOverflowToolbarRequiredWidth() {
        int requiredWidth = 0;
        for (Component component : toolBar.getComponents()) {
            if (!component.isVisible()) {
                continue;
            }
            if (component == overflowButton) {
                continue;
            }
            if (component instanceof Box.Filler) {
                continue;
            }
            requiredWidth += component.getPreferredSize().width;
        }
        return requiredWidth;
    }

    private void updateToolbarLayout() {
        toolBar.revalidate();
        toolBar.repaint();
    }

    private List<JButton> setHiddenButtons(int toolBarWidth, List<JButton> activeButtons, int requiredWidth) {
        int overflowButtonWidth = overflowButton.getPreferredSize().width;
        int usableWidth = Math.max(0, toolBarWidth - overflowButtonWidth);
        List<JButton> hiddenButtons = new ArrayList<>();
        for (int i = activeButtons.size() - 1; i >= 0 && requiredWidth > usableWidth; i--) {
            JButton button = activeButtons.get(i);
            if (!button.isVisible()) {
                continue;
            }
            button.setVisible(false);
            hiddenButtons.add(button);
            requiredWidth -= button.getPreferredSize().width;
        }
        return hiddenButtons;
    }

    private void updateOverflowMenu(List<JButton> hiddenButtons) {
        for (JButton button : hiddenButtons) {
            overflowMenu.add(new JMenuItem(button.getAction()));
        }
        overflowButton.setVisible(!hiddenButtons.isEmpty());
    }
}
