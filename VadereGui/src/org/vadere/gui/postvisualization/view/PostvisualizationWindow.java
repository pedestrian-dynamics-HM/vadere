package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.*;
import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.postvisualization.control.*;
import org.vadere.gui.postvisualization.model.ContactData;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.model.TableAerosolCloudData;
import org.vadere.gui.projectview.control.ActionDeselect;
import org.vadere.gui.projectview.view.ProjectView;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.processor.AerosolCloudDataProcessor;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.config.VadereConfig;
import org.vadere.util.io.IOUtils;
import tech.tablesaw.api.Table;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * Main Window of the new post visualization.
 */
public class PostvisualizationWindow extends JPanel implements Observer, DropTargetListener {
	private static final long serialVersionUID = -8177132133860336295L;
	private static final Configuration CONFIG = VadereConfig.getConfig();

	private final JToolBar toolbar;
	private final ScenarioPanel scenarioPanel;
	private final AdjustPanel adjustPanel;
	private final PostvisualizationModel model;
	private final JMenu mRecentFiles;
	private final JMenuBar menuBar;
	private static final Resources resources = Resources.getInstance("postvisualization");
	private final ScenarioElementView textView;

	private final ButtonGroup playControlGroup;
	private final JToggleButton playButton;
	private final JToggleButton pauseButton;
	private final JToggleButton stopButton;

	public PostvisualizationWindow(final String projectPath) {
		this(false, projectPath);
	}

	private PostvisualizationWindow(final boolean loadTopographyInformationsOnly, final String projectPath) {

		// 1. get data from the user screen
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int windowHeight = screenSize.height - 250;

		// 2. construct the model
		model = new PostvisualizationModel();
		model.addObserver(this);
		model.config.setLoadTopographyInformationsOnly(loadTopographyInformationsOnly);

		// 3. construct the renderer (he draws also the svg and the png's)
		PostvisualizationRenderer renderer = new PostvisualizationRenderer(model);
		renderer.setLogo(resources.getImage("vadere.png"));

		// 4. construct the jscrollpane
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getViewport()
				.addChangeListener(new JViewportChangeListener(model, scrollPane.getVerticalScrollBar()));
		scrollPane.setPreferredSize(new Dimension(1, windowHeight));
		IViewportChangeListener viewportChangeListener = new ViewportChangeListener(model, scrollPane);
		model.addViewportChangeListener(viewportChangeListener);
		model.addScrollPane(scrollPane);

		// 5. construct the scenario panel on that the renderer draw all the content.
		scenarioPanel = new ScenarioPanel(renderer, scrollPane);
		model.addObserver(scenarioPanel);
		scenarioPanel.addComponentListener(new PanelResizeListener(model));
		model.addScaleChangeListener(scenarioPanel);
		scrollPane.setViewportView(scenarioPanel);

		// 6. construct the toolbar
		toolbar = new JToolBar("Toolbar");
		int toolbarSize = CONFIG.getInt("Gui.toolbar.size");
		//toolbar.setPreferredSize(new Dimension(toolbarSize, toolbarSize));
		toolbar.setBorderPainted(false);
		toolbar.setFloatable(false);
		toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
		toolbar.setAlignmentY(Component.TOP_ALIGNMENT);

		// 7. construct the adjust panel
		adjustPanel = new AdjustPanel(model);
		model.addObserver(adjustPanel);

		// 8. set the view options of this frame
		FormLayout layout;
		CellConstraints cc = new CellConstraints();

		// 9. add all components to this frame
		if (CONFIG.getBoolean("PostVis.enableJsonInformationPanel")) {
			layout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
					"2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
			setLayout(layout);

			textView = new ScenarioElementView(model);
			textView.setEditable(false);
			textView.setPreferredSize(new Dimension(1, windowHeight));

			JSplitPane splitPaneForTopographyAndJsonPane = new JSplitPane();
			splitPaneForTopographyAndJsonPane.setResizeWeight(0.8);
			splitPaneForTopographyAndJsonPane.resetToPreferredSizes();
			splitPaneForTopographyAndJsonPane.setLeftComponent(scrollPane);
			splitPaneForTopographyAndJsonPane.setRightComponent(textView);

			add(toolbar, cc.xyw(2, 2, 3));
			add(splitPaneForTopographyAndJsonPane, cc.xywh(2, 4, 4, 1));
			add(adjustPanel, cc.xyw(2, 6, 4));
		} else {
			layout = new FormLayout("2dlu, default:grow, 2dlu", // col
					"2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
			setLayout(layout);

			textView = null;

			add(toolbar, cc.xy(2, 2));
			add(scrollPane, cc.xy(2, 4));
			add(adjustPanel, cc.xy(2, 6));
		}

		int iconHeight = VadereConfig.getConfig().getInt("ProjectView.icon.height.value");
		int iconWidth = VadereConfig.getConfig().getInt("ProjectView.icon.width.value");

		// Player controls
		playControlGroup = new ButtonGroup();
		playButton = addToggleActionToToolbar(toolbar,
				new ActionPlay("play", resources.getIcon("play.png", iconWidth, iconHeight), model),
				"PostVis.btnPlay.tooltip");

		pauseButton = addToggleActionToToolbar(toolbar,
				new ActionPause("pause", resources.getIcon("pause.png", iconWidth, iconHeight), model),
				"PostVis.btnPause.tooltip");
		stopButton = addToggleActionToToolbar(toolbar,
				new ActionStop("stop", resources.getIcon("stop.png", iconWidth, iconHeight), model),
				"PostVis.btnStop.tooltip");


		playControlGroup.add(playButton);
		playControlGroup.add(pauseButton);
		playControlGroup.add(stopButton);

		stopButton.setSelected(true);
		model.addObserver(new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				if(model.getStep() == model.getLastStep()){
					// yes this is intended pause the player but visually show stop in the gui,
					// because this way the slider will stay at the end position
					Player.getInstance(model).pause();
					stopButton.setSelected(true);
				}
			}
		});
		toolbar.addSeparator(new Dimension(5, 50));

		// Pedestrian-related options
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_pedestrian", resources.getIcon("pedestrian.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowPedestrians(!model.config.isShowPedestrians());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowPedestrian.tooltip");
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_trajectory",
						resources.getIcon("trajectories.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowTrajectories(!model.config.isShowTrajectories());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowTrajectories.tooltip");
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_direction",
						resources.getIcon("walking_direction.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowWalkdirection(!model.config.isShowWalkdirection());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowWalkingDirection.tooltip");
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_groups",
						resources.getIcon("group.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowGroups(!model.config.isShowGroups());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowGroupInformation.tooltip");
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_contacts", resources.getIcon("contacts.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (!model.config.isContactsRecorded()) {
							JOptionPane.showMessageDialog(ProjectView.getMainWindow(),
									Messages.getString("PostVis.ShowContactsErrorMessage.text"));
						} else {
							model.config.setShowContacts(!model.config.isShowContacts());
							model.notifyObservers();
						}
					}
				}, "ProjectView.btnShowContacts.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));

		// Other information (neither related to pedestrians nor to measuring tools)
		addToggleActionToToolbar(
				toolbar,
				new ActionVisualization(
						"show_aerosolClouds",
						resources.getIcon("aerosolCloud_icon.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (!model.config.isAerosolCloudsRecorded()) {
							JOptionPane.showMessageDialog(ProjectView.getMainWindow(),
									Messages.getString("PostVis.ShowAerosolCloudsErrorMessage.text") + "\n" + AerosolCloudDataProcessor.class.getName() + "\n" + TableAerosolCloudData.TABLE_NAME + ".txt");
						} else {
							model.config.setShowAerosolClouds(!model.config.isShowAerosolClouds());
							model.notifyObservers();
						}
					}
				}, "ProjectView.btnShowAerosolClouds.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));

		// "Measuring" tools
		addToggleActionToToolbar(toolbar,
				new ActionSwapSelectionMode("draw_voronoi_diagram",
						resources.getIcon("voronoi.png", iconWidth, iconHeight), model),
				"ProjectView.btnDrawVoronoiDiagram.tooltip");
		addToggleActionToToolbar(toolbar,
				new ActionVisualization("show_grid", resources.getIcon("grid.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowGrid(!model.config.isShowGrid());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowGrid.tooltip");
		addToggleActionToToolbar(
				toolbar,
				new ActionVisualization("show_density", resources.getIcon("density.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowDensity(!model.config.isShowDensity());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowDensity.tooltip");
		addToggleActionToToolbar(
				toolbar,
				new ActionShowPotentialField("show_potentialField", resources.getIcon("potentialField.png", iconWidth,
						iconHeight), model),
				"ProjectView.btnShowPotentialfield.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));

		// Recording and snapshot options
		ActionRecording recordAction = new ActionRecording("record", resources.getIcon("record.png", iconWidth,
				iconHeight), renderer);
		JButton recordButton = addActionToToolbar(toolbar, recordAction, "PostVis.btnRecord.tooltip");
		recordAction.setButton(recordButton);

		ArrayList<Action> imgOptions = new ArrayList<>();
		AbstractAction pngImg = new ActionGeneratePNG(Messages.getString("ProjectView.btnPNGSnapshot.tooltip"), resources.getIcon("camera_png.png", iconWidth, iconHeight),
				renderer, model);
		AbstractAction svgImg = new ActionGenerateSVG(Messages.getString("ProjectView.btnSVGSnapshot.tooltip"), resources.getIcon("camera_svg.png", iconWidth, iconHeight),
				renderer, model);
		AbstractAction tikzImg = new ActionGenerateTikz(Messages.getString("ProjectView.btnTikZSnapshot.tooltip"), resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				renderer, model);
		AbstractAction inetImg = new ActionGenerateINETenv(Messages.getString("ProjectView.btnINETSnapshot.tooltip"), resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				renderer, model);

		AbstractAction polyImg = new ActionGeneratePoly(Messages.getString("ProjectView.btnPolySnapshot.tooltip"), resources.getIcon("camera_poly.png", iconWidth, iconHeight),
				ResourceStrings.TOPOGRAPHY_CREATOR_BTN_GENERATE_POLY_TOOLTIP,
				model);

		imgOptions.add(pngImg);
		imgOptions.add(svgImg);
		imgOptions.add(tikzImg);
		imgOptions.add(inetImg);
		imgOptions.add(polyImg);

		ActionVisualizationMenu imgDialog = new ActionVisualizationMenu(
				"camera_menu",
				resources.getIcon("camera.png", iconWidth, iconHeight),
				model, null, imgOptions);
		addActionMenuToToolbar(toolbar, imgDialog, Messages.getString("ProjectView.btnSnapshot.tooltip"));

		toolbar.add(Box.createHorizontalGlue());

		addActionToToolbar(
				toolbar,
				new ActionVisualization("settings", resources.getIcon("settings.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						DialogFactory.createSettingsDialog(model).setVisible(true);
					}

				}, "ProjectView.btnSettings.tooltip");

		menuBar = new JMenuBar();
		JMenu mFile = new JMenu(Messages.getString("PostVis.menuFile.title"));
		JMenu mEdit = new JMenu(Messages.getString("PostVis.menuSettings.title"));
		mRecentFiles = new JMenu(Messages.getString("PostVis.menuRecentFiles.title"));

		menuBar.add(mFile);
		menuBar.add(mRecentFiles);
		menuBar.add(mEdit);

		JMenuItem miLoadFile =
				new JMenuItem(new ActionOpenFile(Messages.getString("PostVis.menuOpenFile.title"), model));
		JMenuItem miCloseFloorFile = new JMenuItem(new ActionRemoveFloorFieldFile(
				Messages.getString("PostVis.menuCloseFloorFieldFile.title"), model));
		/*
		 * JMenuItem miGenerateHighResolutionImage = new JMenuItem(new
		 * ActionGenerateHighResolutionImage(
		 * properties.getProperty("generate_high_resolution_image"), panelModel));
		 */
		JMenuItem miGlobalSettings = new JMenuItem("View");

		String[] paths =
				VadereConfig.getConfig().getString("recentlyOpenedFiles", "").split(",");

		if (paths != null) {
			int i = 1;
			for (String path : paths) {
				mRecentFiles.add(new ActionOpenFile("[" + i + "]" + " " + path, null, model, path));
				i++;
			}
		}

		buildKeyboardShortcuts();

		miGlobalSettings.addActionListener(e -> DialogFactory.createSettingsDialog(model).setVisible(true));

		mFile.add(miLoadFile);
		// mFile.add(miLoadFloorFile);
		// mFile.add(miCloseFloorFile);
		// mFile.add(miGenerateHighResolutionImage);
		mEdit.add(miGlobalSettings);

		// setJMenuBar(menuBar);
		// pack();

		// deselect selected element on esc
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "deselect");
		getActionMap().put("deselect", new ActionDeselect(model, this, null));
		repaint();
		revalidate();

		// Make "this" window a drop target ("this" also handles the drops).
		new DropTarget(this, DnDConstants.ACTION_MOVE, this, true);
	}

	private void buildKeyboardShortcuts() {
		Action spaceKeyReaction = new ActionVisualization("Typed Space Key Reaction", model){
			boolean isRunning = false;
			@Override
			public void actionPerformed(ActionEvent e){
				(isRunning ? pauseButton : playButton).getAction().actionPerformed(null);
				isRunning = !isRunning;
			}
		};
		addKeyboardShortcut("SPACE","Typed Space", spaceKeyReaction);
		addKeyboardShortcut("BACK_SPACE","Typed Backspace", stopButton.getAction());
	}

	private void addKeyboardShortcut(String key, String actionKey, Action action) {
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionKey);
		getActionMap().put(actionKey, action);
	}

	private JMenuBar getMenu() {
		return menuBar;
	}

	public void loadOutputFile(final File trajectoryFile, final HashMap<String, File> additionalFiles, final Scenario scenario) throws IOException {
		Player.getInstance(model).stop();
		try {
			HashMap<String, Table> additionalTables = new HashMap<>();
			for (HashMap.Entry<String, File> entry : additionalFiles.entrySet()) {
				switch (entry.getKey()) {
					case ContactData.TABLE_NAME:
						additionalTables.put(entry.getKey(), IOOutput.readContactData(entry.getValue().toPath()));
					case TableAerosolCloudData.TABLE_NAME:
						additionalTables.put(entry.getKey(), IOOutput.readAerosolCloudData(entry.getValue().toPath()));
				}
			}
			model.init(IOOutput.readTrajectories(trajectoryFile.toPath()), additionalTables, scenario, trajectoryFile.getParent());
			model.notifyObservers();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), Messages.getString("Error.text"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadOutputFile(final File trajectoryFile, final Scenario scenario) throws IOException {
		Player.getInstance(model).stop();
		try {
			model.init(IOOutput.readTrajectories(trajectoryFile.toPath()), scenario, trajectoryFile.getParent());
			model.notifyObservers();
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage(), Messages.getString("Error.text"), JOptionPane.ERROR_MESSAGE);
		}
	}

	public void loadOutputFile(final Scenario scenario) {
		Player.getInstance(model).stop();
		model.init(scenario, model.getOutputPath());
		model.notifyObservers();
	}

	private static JButton addActionToToolbar(final JToolBar toolbar, final Action action,
			final String toolTipProperty) {
		return SwingUtils.addActionToToolbar(toolbar, action, Messages.getString(toolTipProperty));
	}

	private static JToggleButton addToggleActionToToolbar(final JToolBar toolbar, final Action action,
											  final String toolTipProperty) {
		return SwingUtils.addToggleActionToToolbar(toolbar, action, Messages.getString(toolTipProperty));
	}

	private static JButton addActionMenuToToolbar(final JToolBar toolbar, final ActionVisualizationMenu menuAction,
												  final String toolTipProperty) {
		JButton btn = SwingUtils.addActionToToolbar(toolbar, menuAction, Messages.getString(toolTipProperty));
		menuAction.setParent(btn);
		return btn;
	}

	public IDefaultModel getDefaultModel(){
		return this.model;
	}

	@Override
	public void update(java.util.Observable o, Object arg) {
		SwingUtilities.invokeLater(() -> {
			if(model.hasOutputChanged()) {
				String[] paths =
						VadereConfig.getConfig().getString("recentlyOpenedFiles", "").split(",");
				if (paths != null) {
					mRecentFiles.removeAll();
					int i = 1;
					for (String path : paths) {
						if (path.length() > 0) {
							mRecentFiles.add(new ActionOpenFile("[" + i + "]" + " " + path, null, model, path));
							i++;
						}
					}
				}
			}
		});
	}

	public static void start() {
		try {
			// Set Java L&F from system
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException e) {
			IOUtils.errorBox("The system look and feel could not be loaded.", "Error setLookAndFeel");
		}

		EventQueue.invokeLater(() -> {
			JFrame frame = new JFrame();
			PostvisualizationWindow postVisWindow = new PostvisualizationWindow(true, "./");
			frame.add(postVisWindow);
			frame.setJMenuBar(postVisWindow.getMenu());

			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

			frame.setVisible(true);
			frame.pack();
		});
	}

	// Methods for drop support of this window.
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {

	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {

	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {

	}

	@Override
	public void dragExit(DropTargetEvent dte) {

	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		try{
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
			List<File> fileList = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

			// This is a robust solution, but user should be warned if multiple files are dropped.
			for(File file : fileList){
				openScenarioAndTrajectoryFile(file);
			}
		} catch (Exception ex){
			JOptionPane.showMessageDialog(
					this,
					Messages.getString("Gui.DropAction.Error.text") + "\n"
							+ ex.getMessage(),
					Messages.getString("InformationDialogError.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void openScenarioAndTrajectoryFile(@NotNull File scenarioOrTrajectoryFile) {
		VadereConfig.getConfig().setProperty("SettingsDialog.outputDirectory.path", scenarioOrTrajectoryFile.getParent());
		VadereConfig.getConfig().setProperty("SettingsDialog.outputDirectory.path", scenarioOrTrajectoryFile.getParent());

		Runnable runnable = () -> {
			Player.getInstance(model).stop();

			final JFrame dialog = DialogFactory.createLoadingDialog();
			dialog.setVisible(true);

			try {
				Player.getInstance(model).stop();

				File parentDirectory = scenarioOrTrajectoryFile.getParentFile();

				Optional<File> trajectoryFile =
						IOUtils.getFirstFile(parentDirectory, IOUtils.TRAJECTORY_FILE_EXTENSION);
				Optional<File> scenarioFile =
						IOUtils.getFirstFile(parentDirectory, IOUtils.SCENARIO_FILE_EXTENSION);

				if (trajectoryFile.isPresent() && scenarioFile.isPresent()) {
					Scenario vadereScenario = IOOutput.readScenario(scenarioFile.get().toPath());
					model.init(IOOutput.readTrajectories(trajectoryFile.get().toPath()), vadereScenario, trajectoryFile.get().getParent());
					model.notifyObservers();
					dialog.dispose();
				} else {
					String errorMessage = String.format("%s\n%s\n%s", Messages.getString("Data.TrajectoryOrScenarioFile.NoData.text"),
							trajectoryFile,
							scenarioFile);
					throw new IOException(errorMessage);
				}
			} catch (Exception e) {
				JOptionPane.showMessageDialog(
						null,
						e.getMessage(),
						Messages.getString("InformationDialogFileError"),
						JOptionPane.ERROR_MESSAGE);
			}

			// when loading is finished, make frame disappear
			SwingUtilities.invokeLater(() -> dialog.dispose());
		};

		new Thread(runnable).start();
	}

}
