package org.vadere.gui.postvisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.control.JViewportChangeListener;
import org.vadere.gui.components.control.PanelResizeListener;
import org.vadere.gui.components.control.ViewportChangeListener;
import org.vadere.gui.components.control.simulation.ActionGeneratePNG;
import org.vadere.gui.components.control.simulation.ActionGenerateSVG;
import org.vadere.gui.components.control.simulation.ActionGenerateTikz;
import org.vadere.gui.components.control.simulation.ActionSwapSelectionMode;
import org.vadere.gui.components.control.simulation.ActionVisualization;
import org.vadere.gui.components.model.IDefaultModel;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.postvisualization.PostVisualisation;
import org.vadere.gui.postvisualization.control.*;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.projectview.control.ActionDeselect;
import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.io.HashGenerator;
import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.io.IOUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Observer;
import java.util.prefs.Preferences;

import javax.swing.*;

/**
 * Main Window of the new post visualization.
 * 
 * @Version 1.0
 * 
 */
public class PostvisualizationWindow extends JPanel implements Observer {
	private static final long serialVersionUID = -8177132133860336295L;
	private JToolBar toolbar;
	private ScenarioPanel scenarioPanel;
	private AdjustPanel adjustPanel;
	private PostvisualizationModel model;
	private JMenu mRecentFiles;
	private JMenuBar menuBar;
	private static Resources resources = Resources.getInstance("postvisualization");
	private final ScenarioElementView textView;

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
		int toolbarSize = Integer.parseInt(resources.getProperty("Toolbar.size"));
		toolbar.setPreferredSize(new Dimension(toolbarSize, toolbarSize));
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
		if (resources.getBooleanProperty("PostVis.enableJsonInformationPanel")) {
			layout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
					"2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
			textView = new ScenarioElementView(model);
			textView.setEditable(false);
			textView.setPreferredSize(new Dimension(1, windowHeight));
			setLayout(layout);
			add(toolbar, cc.xyw(2, 2, 4));
			add(scrollPane, cc.xy(2, 4));
			add(adjustPanel, cc.xyw(2, 6, 4));
			add(textView, cc.xy(4, 4));
			// model.addObserver(textView);
		} else {
			layout = new FormLayout("2dlu, default:grow, 2dlu", // col
					"2dlu, default, 2dlu, default, 2dlu, default, 2dlu"); // rows
			textView = null;
			setLayout(layout);
			add(toolbar, cc.xy(2, 2));
			add(scrollPane, cc.xy(2, 4));
			add(adjustPanel, cc.xy(2, 6));
		}

		int iconHeight = Integer.valueOf(resources.getProperty("ProjectView.icon.height.value"));
		int iconWidth = Integer.valueOf(resources.getProperty("ProjectView.icon.width.value"));
		addActionToToolbar(toolbar,
				new ActionPlay("play", resources.getIcon("play.png", iconWidth, iconHeight), model),
				"PostVis.btnPlay.tooltip");
		addActionToToolbar(toolbar,
				new ActionPause("pause", resources.getIcon("pause.png", iconWidth, iconHeight), model),
				"PostVis.btnPause.tooltip");
		addActionToToolbar(toolbar,
				new ActionStop("play", resources.getIcon("stop.png", iconWidth, iconHeight), model),
				"PostVis.btnStop.tooltip");
		toolbar.addSeparator(new Dimension(5, 50));

		addActionToToolbar(toolbar,
				new ActionVisualization("show_pedestrian", resources.getIcon("pedestrian.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowPedestrians(!model.config.isShowPedestrians());
						model.notifyObservers();
					}

				}, "ProjectView.btnShowPedestrian.tooltip");

		addActionToToolbar(toolbar,
				new ActionVisualization("show_trajectory",
						resources.getIcon("trajectories.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowTrajectories(!model.config.isShowTrajectories());
						model.notifyObservers();
					}


				}, "ProjectView.btnShowTrajectories.tooltip");


		addActionToToolbar(toolbar,
				new ActionVisualization("show_triangulation",
						resources.getIcon("triangulation.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						if(model.isTriangulationVisible()) {
							model.hideTriangulation();
						}
						else {
							model.showTriangulation();
						}
						model.notifyObservers();
					}

					;
				}, "View.btnShowTriangulation.tooltip"
		);

		addActionToToolbar(toolbar,
				new ActionVisualization("show_direction",
						resources.getIcon("walking_direction.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowWalkdirection(!model.config.isShowWalkdirection());
						model.notifyObservers();
					}


				}, "ProjectView.btnShowWalkingDirection.tooltip");

		addActionToToolbar(toolbar,
				new ActionVisualization("show_groups",
						resources.getIcon("group.png", iconWidth, iconHeight), model) {

					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowGroups(!model.config.isShowGroups());
						model.notifyObservers();
					}
				}, "ProjectView.btnShowGroupInformation.tooltip");

		addActionToToolbar(toolbar,
				new ActionSwapSelectionMode("draw_voronoi_diagram",
						resources.getIcon("voronoi.png", iconWidth, iconHeight), model),
				"ProjectView.btnDrawVoronoiDiagram.tooltip");

		toolbar.addSeparator(new Dimension(5, 50));

		addActionToToolbar(
				toolbar,
				new ActionShowPotentialField("show_potentialField", resources.getIcon("potentialField.png", iconWidth,
						iconHeight), model),
				"ProjectView.btnShowPotentialfield.tooltip");

		addActionToToolbar(toolbar,
				new ActionVisualization("show_grid", resources.getIcon("grid.png", iconWidth, iconHeight), model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowGrid(!model.config.isShowGrid());
						model.notifyObservers();
					}


				}, "ProjectView.btnShowGrid.tooltip");

		addActionToToolbar(
				toolbar,
				new ActionVisualization("show_density", resources.getIcon("density.png", iconWidth, iconHeight),
						model) {
					@Override
					public void actionPerformed(ActionEvent e) {
						model.config.setShowDensity(!model.config.isShowDensity());
						model.notifyObservers();
					}


				}, "ProjectView.btnShowDensity.tooltip");


		// toolbar.addSeparator(new Dimension(5, 50));

		ActionRecording recordAction = new ActionRecording("record", resources.getIcon("record.png", iconWidth,
				iconHeight), renderer);
		JButton recordButton = addActionToToolbar(toolbar, recordAction, "PostVis.btnRecord.tooltip");
		recordAction.setButton(recordButton);

		toolbar.addSeparator(new Dimension(5, 50));
		ArrayList<Action> imgOptions = new ArrayList<>();
		AbstractAction pngImg = new ActionGeneratePNG(Messages.getString("ProjectView.btnPNGSnapshot.tooltip"), resources.getIcon("camera_png.png", iconWidth, iconHeight),
				renderer, model);
		AbstractAction svgImg = new ActionGenerateSVG(Messages.getString("ProjectView.btnSVGSnapshot.tooltip"), resources.getIcon("camera_svg.png", iconWidth, iconHeight),
				renderer, model);
		AbstractAction tikzImg = new ActionGenerateTikz(Messages.getString("ProjectView.btnTikZSnapshot.tooltip"), resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				renderer, model);
		// add new ImageGenerator Action ...

		imgOptions.add(pngImg);
		imgOptions.add(svgImg);
		imgOptions.add(tikzImg);
		// add Action to List ....

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

				;
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
				Preferences.userNodeForPackage(PostVisualisation.class).get("recentlyOpenedFiles", "").split(",");

		if (paths != null) {
			int i = 1;
			for (String path : paths) {
				mRecentFiles.add(new ActionOpenFile("[" + i + "]" + " " + path, null, model, path));
				i++;
			}
		}


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
	}

	private JMenuBar getMenu() {
		return menuBar;
	}

	public void loadOutputFile(final File trajectoryFile, final Scenario scenario) throws IOException {
		Player.getInstance(model).stop();
		model.init(IOOutput.readTrajectories(trajectoryFile.toPath(), scenario), scenario, trajectoryFile.getParent());
		model.notifyObservers();
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

		String[] paths =
				Preferences.userNodeForPackage(PostVisualisation.class).get("recentlyOpenedFiles", "").split(",");
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
}
