package org.vadere.gui.onlinevisualization.view;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;

import org.vadere.gui.components.control.IViewportChangeListener;
import org.vadere.gui.components.control.JViewportChangeListener;
import org.vadere.gui.components.control.PanelResizeListener;
import org.vadere.gui.components.control.ViewportChangeListener;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.ScenarioElementView;
import org.vadere.gui.components.view.ScenarioScrollPane;
import org.vadere.gui.components.view.SimulationInfoPanel;
import org.vadere.gui.onlinevisualization.control.ActionGeneratePNG;
import org.vadere.gui.onlinevisualization.control.ActionGenerateSVG;
import org.vadere.gui.onlinevisualization.control.ActionGenerateTikz;
import org.vadere.gui.onlinevisualization.control.ActionShowPotentialField;
import org.vadere.gui.onlinevisualization.model.OnlineVisualizationModel;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

public class OnlineVisualisationWindow extends JPanel implements Observer {

	private static final long serialVersionUID = 3522170593760789565L;
	private static final Resources resources = Resources.getInstance("global");
	private ScenarioElementView jsonPanel;
	private JToolBar toolbar;
	private SimulationInfoPanel infoPanel;

	public OnlineVisualisationWindow(final MainPanel mainPanel, final OnlineVisualizationModel model) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int windowHeight = screenSize.height - 250;

		CellConstraints cc = new CellConstraints();
		FormLayout spiltLayout = new FormLayout("2dlu, default:grow(0.75), 2dlu, default:grow(0.25), 2dlu", // col
				"2dlu, default, 2dlu, fill:default:grow, 2dlu, default, 2dlu"); // rows

		JScrollPane scrollPane = new ScenarioScrollPane(mainPanel, model);
		model.addScaleChangeListener(mainPanel);
		mainPanel.addComponentListener(new PanelResizeListener(model));
		mainPanel.setScrollPane(scrollPane);
		scrollPane.getViewport()
				.addChangeListener(new JViewportChangeListener(model, scrollPane.getVerticalScrollBar()));

		IViewportChangeListener viewportChangeListener = new ViewportChangeListener(model, scrollPane);
		model.addViewportChangeListener(viewportChangeListener);

		jsonPanel = new ScenarioElementView(model);
		jsonPanel.setEditable(false);

		model.addObserver(mainPanel);
		model.addSelectScenarioElementListener(jsonPanel);

		this.toolbar = new JToolBar("OnlineVisualizationToolbar");
		toolbar.setFloatable(false);
		// toolbar.setBorderPainted(false);
		toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);
		toolbar.setAlignmentY(Component.TOP_ALIGNMENT);
		int toolbarSize = Integer.parseInt(resources.getProperty("Toolbar.size"));
		toolbar.setPreferredSize(new Dimension(toolbarSize, toolbarSize));

		infoPanel = new SimulationInfoPanel(model);

		model.addObserver(this);
		model.addObserver(infoPanel);

		setLayout(spiltLayout);


		int iconHeight = Integer.valueOf(resources.getProperty("View.icon.height.value"));
		int iconWidth = Integer.valueOf(resources.getProperty("View.icon.width.value"));


		AbstractAction paintArrowAction = new AbstractAction("paintArrowAction",
				resources.getIcon("walking_direction.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 14131313L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowWalkdirection(!model.config.isShowWalkdirection());
				model.notifyObservers();
			}
		};

		AbstractAction paintPedestriansAction = new AbstractAction("paintPedestrianAction",
				resources.getIcon("pedestrian.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 14131313L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowPedestrians(!model.config.isShowPedestrians());
				model.notifyObservers();
			}
		};

		AbstractAction paintGridAction = new AbstractAction("paintGridAction",
				resources.getIcon("grid.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowGrid(!model.config.isShowGrid());
				model.notifyObservers();
			}
		};

		AbstractAction paintTrajectories = new AbstractAction("paintTrajectoriesAction",
				resources.getIcon("trajectories.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowTrajectories(!model.config.isShowTrajectories());
				model.notifyObservers();
			}
		};

		AbstractAction paintDensity = new AbstractAction("paintDensityAction",
				resources.getIcon("density.png", iconWidth, iconHeight)) {

			private static final long serialVersionUID = 1123132132L;

			@Override
			public void actionPerformed(ActionEvent e) {
				model.config.setShowDensity(!model.config.isShowDensity());
				model.notifyObservers();
			}
		};

		ActionGeneratePNG generatePNG = new ActionGeneratePNG(
				"generatePNG",
				resources.getIcon("camera_png.png", iconWidth, iconHeight),
				new OnlinevisualizationRenderer(model),
				model);


		ActionGenerateSVG generateSVG = new ActionGenerateSVG(
				"generateSVG",
				resources.getIcon("camera_svg.png", iconWidth, iconHeight),
				new OnlinevisualizationRenderer(model),
				model);

		ActionGenerateTikz generateTikz = new ActionGenerateTikz(
				"generateTikz",
				resources.getIcon("camera_tikz.png", iconWidth, iconHeight),
				new OnlinevisualizationRenderer(model),
				model);

        ActionShowPotentialField showPotentialField = new ActionShowPotentialField(
                "showPotentialField",
                resources.getIcon("potentialField.png", iconWidth, iconHeight),
                model);

		mainPanel.addRendererChangeListener(generatePNG);
		mainPanel.addRendererChangeListener(generateSVG);
		mainPanel.addRendererChangeListener(generateTikz);
		mainPanel.addRendererChangeListener(showPotentialField);


		SwingUtils.addActionToToolbar(toolbar, paintPedestriansAction,
				Messages.getString("View.btnShowPedestrian.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintTrajectories,
				Messages.getString("View.btnShowTrajectories.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintArrowAction,
				Messages.getString("View.btnShowWalkingDirection.tooltip"));

		toolbar.addSeparator();

		SwingUtils.addActionToToolbar(toolbar, paintGridAction, Messages.getString("View.btnShowGrid.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, paintDensity, Messages.getString("View.btnShowDensity.tooltip"));

		toolbar.addSeparator();

		SwingUtils.addActionToToolbar(toolbar, generatePNG, Messages.getString("PostVis.btnPNGSnapshot.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, generateSVG, Messages.getString("PostVis.btnSVGSnapshot.tooltip"));
		SwingUtils.addActionToToolbar(toolbar, generateTikz, Messages.getString("PostVis.btnTikzSnapshot.tooltip"));
        SwingUtils.addActionToToolbar(toolbar, showPotentialField, Messages.getString("OnlineVis.btnShowPotentialfield.tooltip"));

		add(toolbar, cc.xyw(2, 2, 3));
		add(scrollPane, cc.xy(2, 4));
		scrollPane.setPreferredSize(new Dimension(1, windowHeight));
		add(jsonPanel, cc.xy(4, 4));
		add(infoPanel, cc.xyw(2, 6, 3));
	}

	@Override
	public void update(Observable o, Object arg) {
		repaint();
	}
}
