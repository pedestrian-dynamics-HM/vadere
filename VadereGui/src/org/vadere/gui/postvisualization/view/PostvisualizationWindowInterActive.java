package org.vadere.gui.postvisualization.view;

import com.formdev.flatlaf.FlatLightLaf;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.control.ActionGeneratePoly;
import org.vadere.gui.components.control.simulation.*;
import org.vadere.gui.components.utils.Messages;
import org.vadere.gui.components.utils.ResourceStrings;
import org.vadere.gui.components.utils.Resources;
import org.vadere.gui.components.utils.SwingUtils;
import org.vadere.gui.components.view.DialogFactory;
import org.vadere.gui.postvisualization.control.*;
import org.vadere.gui.postvisualization.model.ContactData;
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
public class PostvisualizationWindowInterActive extends PostvisualizationWindow implements Observer, DropTargetListener {

    private JButton playButton;
    private JButton pauseButton;
    private JButton stopButton;
    private ButtonGroup playControlGroup;

    private static final Resources RESOURCE = Resources.getInstance("global");
    private static final int ICON_SIZE = (int)(VadereConfig.getConfig().getInt("ProjectView.icon.height.value")*VadereConfig.getConfig().getFloat("Gui.scale"));

    public PostvisualizationWindowInterActive() {
        super();
        playControlGroup = new ButtonGroup();
    }





    public void init(boolean loadTopo, String dirPath) {

        super.init(loadTopo, dirPath);

        int iconHeight = VadereConfig.getConfig().getInt("ProjectView.icon.height.value");
        int iconWidth = VadereConfig.getConfig().getInt("ProjectView.icon.width.value");

        PostvisualizationRenderer renderer = getRenderer();

        // Player controls
        playButton = addActionToToolbar(toolbar,
                new ActionPlay("play", RESOURCE.getIconSVG("transport_play",ICON_SIZE,ICON_SIZE), model),
                "PostVis.btnPlay.tooltip");
        pauseButton = addActionToToolbar(toolbar,
                new ActionPause("pause", RESOURCE.getIconSVG("transport_pause",ICON_SIZE,ICON_SIZE), model),
                "PostVis.btnPause.tooltip");
        stopButton = addActionToToolbar(toolbar,
                new ActionStop("stop", RESOURCE.getIconSVG("transport_stop",ICON_SIZE,ICON_SIZE), model),
                "PostVis.btnStop.tooltip");


        playControlGroup.add(playButton);
        playControlGroup.add(pauseButton);
        playControlGroup.add(stopButton);

        stopButton.setSelected(true);
        //TODO check here

        toolbar.addSeparator(new Dimension(5, 50));

        // Pedestrian-related options
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_pedestrian", RESOURCE.getIconSVG("show_pedestrians",ICON_SIZE,ICON_SIZE),
                        model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowPedestrians(!model.config.isShowPedestrians());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowPedestrian.tooltip");
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_trajectory",
                        RESOURCE.getIconSVG("trajectories",ICON_SIZE,ICON_SIZE), model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowTrajectories(!model.config.isShowTrajectories());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowTrajectories.tooltip");
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_direction",
                        RESOURCE.getIconSVG("walking_direction",ICON_SIZE,ICON_SIZE), model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowWalkdirection(!model.config.isShowWalkdirection());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowWalkingDirection.tooltip");
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_groups",
                        RESOURCE.getIconSVG("group",ICON_SIZE,ICON_SIZE), model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowGroups(!model.config.isShowGroups());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowGroupInformation.tooltip");
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_contacts",RESOURCE.getIconSVG("contacts",ICON_SIZE,ICON_SIZE),
                        model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
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
                        RESOURCE.getIconSVG("aerosol",ICON_SIZE,ICON_SIZE),
                        model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
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
                        RESOURCE.getIconSVG("voronoi",ICON_SIZE,ICON_SIZE), model),
                "ProjectView.btnDrawVoronoiDiagram.tooltip");
        addToggleActionToToolbar(toolbar,
                new ActionVisualization("show_grid", RESOURCE.getIconSVG("grid",ICON_SIZE,ICON_SIZE), model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowGrid(!model.config.isShowGrid());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowGrid.tooltip");
        addToggleActionToToolbar(
                toolbar,
                new ActionVisualization("show_density", RESOURCE.getIconSVG("density",ICON_SIZE,ICON_SIZE),
                        model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        model.config.setShowDensity(!model.config.isShowDensity());
                        model.notifyObservers();
                    }
                }, "ProjectView.btnShowDensity.tooltip");
        addToggleActionToToolbar(
                toolbar,
                new ActionShowPotentialField("show_potentialField", RESOURCE.getIconSVG("potential",ICON_SIZE,ICON_SIZE), model),
                "ProjectView.btnShowPotentialfield.tooltip");
        toolbar.addSeparator(new Dimension(5, 50));

        // Recording and snapshot options
        ActionRecording recordAction = new ActionRecording("record", RESOURCE.getIconSVG("record",ICON_SIZE,ICON_SIZE), renderer);


        JButton recordButton = addActionToToolbar(toolbar, recordAction, "PostVis.btnRecord.tooltip");
        recordAction.setButton(recordButton);

        ArrayList<Action> imgOptions = new ArrayList<>();
        AbstractAction pngImg = new ActionGeneratePNG(Messages.getString("ProjectView.btnPNGSnapshot.tooltip"),RESOURCE.getIconSVG("camera_png", ICON_SIZE,ICON_SIZE),
                renderer, model);
        AbstractAction svgImg = new ActionGenerateSVG(Messages.getString("ProjectView.btnSVGSnapshot.tooltip"),RESOURCE.getIconSVG("camera_svg", ICON_SIZE,ICON_SIZE),
                renderer, model);
        AbstractAction tikzImg = new ActionGenerateTikz(Messages.getString("ProjectView.btnTikZSnapshot.tooltip"), RESOURCE.getIconSVG("camera_tikz", ICON_SIZE,ICON_SIZE),
                renderer, model);
        AbstractAction inetImg = new ActionGenerateINETenv(Messages.getString("ProjectView.btnINETSnapshot.tooltip"), RESOURCE.getIconSVG("camera_tikz", ICON_SIZE,ICON_SIZE),
                renderer, model);

        AbstractAction polyImg = new ActionGeneratePoly(model);

        imgOptions.add(pngImg);
        imgOptions.add(svgImg);
        imgOptions.add(tikzImg);
        imgOptions.add(inetImg);
        imgOptions.add(polyImg);

        ActionVisualizationMenu imgDialog = new ActionVisualizationMenu(
                "camera_menu",
                RESOURCE.getIconSVG("camera", ICON_SIZE,ICON_SIZE),
                model, null, imgOptions);
        addActionMenuToToolbar(toolbar, imgDialog, Messages.getString("ProjectView.btnSnapshot.tooltip"));

        toolbar.add(Box.createHorizontalGlue());

        addActionToToolbar(
                toolbar,
                new ActionVisualization("settings", RESOURCE.getIconSVG("settings",ICON_SIZE,ICON_SIZE), model) {
                    @Override
                    public void actionPerformed(ActionEvent e1) {
                        DialogFactory.createSettingsDialog(model).setVisible(true);
                    }

                }, "ProjectView.btnSettings.tooltip");


        JMenu mFile = new JMenu(Messages.getString("PostVis.menuFile.title"));
        JMenu mEdit = new JMenu(Messages.getString("PostVis.menuSettings.title"));


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

    @Override
    public void update(java.util.Observable o, Object arg) {
        SwingUtilities.invokeLater(() -> {
            if (model.hasOutputChanged()) {
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

    private void buildKeyboardShortcuts() {
        Action spaceKeyReaction = new ActionVisualization("Typed Space Key Reaction", model) {
            boolean isRunning = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                (isRunning ? pauseButton : playButton).getAction().actionPerformed(null);
                isRunning = !isRunning;
            }
        };
        addKeyboardShortcut("SPACE", "Typed Space", spaceKeyReaction);
        addKeyboardShortcut("BACK_SPACE", "Typed Backspace", stopButton.getAction());
    }

    private void addKeyboardShortcut(String key, String actionKey, Action action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), actionKey);
        getActionMap().put(actionKey, action);
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

    public static void start() {
        FlatLightLaf.setup();

        EventQueue.invokeLater(() -> {
            JFrame frame = new JFrame();
            PostvisualizationWindowInterActive postVisWindow = new PostvisualizationWindowInterActive();
            postVisWindow.init(true, "./");
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
        try {
            dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
            List<File> fileList = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

            // This is a robust solution, but user should be warned if multiple files are dropped.
            for (File file : fileList) {
                openScenarioAndTrajectoryFile(file);
            }
        } catch (Exception ex) {
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
