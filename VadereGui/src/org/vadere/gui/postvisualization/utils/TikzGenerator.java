package org.vadere.gui.postvisualization.utils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.state.scenario.*;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.geometry.shapes.VPoint;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.geom.PathIterator.*;

/**
 * Convert the (Java) scenario description into a TikZ representation.
 *
 * Usually, each (Java) scenario element is represented as a path via
 * @see PathIterator This PathSeparator must be converted into its TikZ
 * representation.
 *
 * For example, traversing a Java path with PathIterator returns two segments:
 *
 *   segment type = 0 (SEG_MOVETO) with coords: [1.0, 1.0, 0.0, 0.0, 0.0, 0.0]
 *   segment type = 3 (SEG_LINETO) with coords: [2.0, 2.0, 0.0, 0.0, 0.0, 0.0]
 *
 * This must be transformed to TikZ:
 *
 * (1.0,1.0) to (2.0,2.0)
 *
 * The TikZGenerator should also respect the GUI settings (e.g., enabled
 * elements, colors etc.).
 */
public class TikzGenerator {

	private final static Logger logger = LogManager.getLogger(TikzGenerator.class);
	private final SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;
	private final String[] translationTable;

	public TikzGenerator(final SimulationRenderer renderer,
						 final SimulationModel<? extends DefaultSimulationConfig> model) {
		this.renderer = renderer;
		this.model = model;
		this.translationTable = new String[SEG_CLOSE + 1];

		initializeTranslationTable(translationTable);
	}

	private void initializeTranslationTable(String[] translationTable) {
		// Map Java path commands to TikZ commands.
		translationTable[SEG_MOVETO] = "(%f,%f) ";
		translationTable[SEG_LINETO] = "to (%f,%f) ";
		translationTable[SEG_QUADTO] = ".. controls (%f,%f) .. (%f,%f) ";
		translationTable[SEG_CUBICTO] = ".. controls (%f,%f) and (%f,%f) .. (%f,%f) ";
		translationTable[SEG_CLOSE] = "";
	}

	public void generateTikz(final File file) {
		String tikzCodeColorDefinitions = generateTikzColorDefinitions(model);
		String tikzCodeDrawSettings = generateTikzDrawSettings(model);
		String tikzCodeScenarioElements = convertScenarioElementsToTikz();

		String tikzOutput = "" +
				"\\documentclass{standalone}\n" +
				"\\usepackage{tikz}\n\n" +
				tikzCodeColorDefinitions +
				tikzCodeDrawSettings +
				"\\begin{document}\n" +
                "% Change scaling to [x=1mm,y=1mm] if TeX reports \"Dimension too large\".\n" +
				"\\begin{tikzpicture}[x=1cm,y=1cm]\n" +
				tikzCodeScenarioElements +
				"\\end{tikzpicture}\n" +
				"\\end{document}\n";

		// TODO: maybe uses Java's resources notation (in general, writing the file should be done by the caller not here).
		try {
			file.createNewFile();
			Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
			out.write(tikzOutput);
			out.flush();
			logger.info("generate new TikZ: " + file.getAbsolutePath());
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		}
	}

	private String generateTikzColorDefinitions(SimulationModel<? extends DefaultSimulationConfig> model) {
		String colorDefinitions = "% Color Definitions\n";

		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";

		Color sourceColor = model.getConfig().getSourceColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "SourceColor", sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue());

		Color targetColor = model.getConfig().getTargetColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "TargetColor", targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());

		Color obstacleColor = model.getConfig().getObstacleColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "ObstacleColor", obstacleColor.getRed(), obstacleColor.getGreen(), obstacleColor.getBlue());

		Color stairColor = model.getConfig().getStairColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "StairColor", stairColor.getRed(), stairColor.getGreen(), stairColor.getBlue());

		Color agentColor = model.getConfig().getPedestrianDefaultColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "AgentColor", agentColor.getRed(), agentColor.getGreen(), agentColor.getBlue());

		colorDefinitions += "\n";

		return colorDefinitions;
	}

	private String generateTikzDrawSettings(SimulationModel<? extends DefaultSimulationConfig> model) {
		// Generate TeX variables for common draw settings like agent radius and
		// use them later on when generating TikZ code. These settins can be used
		// by TikZ users to adapt the drawing quickly.
		String drawSettings = "% Draw Settings\n";

		double agentRadius = model.getConfig().getPedestrianTorso() / 2.0;

		drawSettings += String.format(Locale.US,"\\newcommand{\\AgentRadius}{%f}\n", agentRadius);
		drawSettings += String.format(Locale.US,"\\newcommand{\\LineWidth}{%d}\n", 1);

		drawSettings += "\n";

		return drawSettings;
	}

		private String convertScenarioElementsToTikz() {
		String generatedCode = "";

		DefaultSimulationConfig config = model.getConfig();
		Topography topography = model.getTopography();

		// Clip everything outside of topography bound.
		generatedCode += "% Clipping\n";
		String clipTextPattern = "\\clip (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(Locale.US, clipTextPattern,
				topography.getBounds().x,
				topography.getBounds().y,
				topography.getBounds().x + topography.getBounds().width,
				topography.getBounds().y + topography.getBounds().height);

		// Draw background elements first, then other scenario elements.
		generatedCode += "% Ground\n";
		String groundTextPattern = (config.isShowGrid()) ? "\\draw[help lines] (%f,%f) grid (%f,%f);\n" : "\\fill[white] (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(Locale.US, groundTextPattern,
				topography.getBounds().x,
				topography.getBounds().y,
				topography.getBounds().x + topography.getBounds().width,
				topography.getBounds().y + topography.getBounds().height);

		if (config.isShowSources()) {
			generatedCode += "% Sources\n";
			for (Source source : topography.getSources()) {
				generatedCode += String.format(Locale.US, "\\fill[SourceColor] %s;\n", generatePathForScenarioElement(source));
			}
		} else {
			generatedCode += "% Sources (not enabled in config)\n";
		}

		if (config.isShowTargets()) {
			generatedCode += "% Targets\n";
			for (Target target : topography.getTargets()) {
				generatedCode += String.format(Locale.US, "\\fill[TargetColor] %s;\n", generatePathForScenarioElement(target));
			}
		} else {
			generatedCode += "% Targets (not enabled in config)\n";
		}

		if (config.isShowObstacles()) {
			generatedCode += "% Obstacles\n";
			for (Obstacle obstacle : topography.getObstacles()) {
				generatedCode += String.format(Locale.US, "\\fill[ObstacleColor] %s;\n", generatePathForScenarioElement(obstacle));
			}
		} else {
			generatedCode += "% Obstacles (not enabled in config)\n";
		}

		if (config.isShowStairs()) {
			generatedCode += "% Stairs\n";
			for (Stairs stair : topography.getStairs()) {
				generatedCode += String.format(Locale.US, "\\fill[black] %s;\n", generatePathForScenarioElement(stair));
				generatedCode += String.format(Locale.US, "\\fill[StairColor] %s;\n", generatePathForStairs(stair));
			}
		} else {
			generatedCode += "% Stairs (not enabled in config)\n";
		}

        if (config.isShowTrajectories()) {
            generatedCode += "% Trajectories\n";

            if (model instanceof PostvisualizationModel) {
                generatedCode += drawTrajectories((PostvisualizationModel)model);
            } else {
                generatedCode += String.format(Locale.US, "%% Passed model %s does not contain trajectories\n", model.getClass().getSimpleName());
            }
        } else {
            generatedCode += "% Trajectories (not enabled in config)\n";
        }

        if (config.isShowPedestrians()) {
            generatedCode += "% Agents\n";
            generatedCode += drawAgents(config);
        } else {
            generatedCode = "% Agents (not enabled in config)\n";
        }

        return generatedCode;
	}

    private String drawTrajectories(PostvisualizationModel model) {
	    // Use a thread-safe string implementation because streams are used here.
        final StringBuffer generatedCode = new StringBuffer("");

	    Stream<Trajectory> trajectoryStream = model.getAlivePedestrians();
        Step currentTimeStep = model.getStep().orElseGet(null);

        if (currentTimeStep != null) {
            trajectoryStream.forEach(trajectory -> {
                Stream<VPoint> trajectoryPoints = trajectory.getPositionsReverse(currentTimeStep);

                // Use a newline ("to\n") for joining because TeX could possibly choke up on long lines.
                String trajectoryAsTikzString = trajectoryPoints
                        .map(point -> String.format(Locale.US, "(%f,%f)", point.x, point.y))
                        .collect(Collectors.joining(" to\n"));

                String coloredTrajectory = applyAgentColorToTrajectory(trajectoryAsTikzString, trajectory.getAgent(currentTimeStep));

                int pedestrianId = trajectory.getPedestrianId();
                Optional<Step> trajectoryEndStep = trajectory.getEndStep();
                String trajectoryEndStepAsString = (trajectoryEndStep.isPresent()) ? "" + trajectoryEndStep.get().toString() : "unknown end step" ;
                String currentTimeStepAsString = currentTimeStep.toString();

                generatedCode.append(String.format(Locale.US, "%% Trajectory Agent %d @ step %s of %s\n", pedestrianId, currentTimeStepAsString, trajectoryEndStepAsString));
                generatedCode.append(coloredTrajectory);
            });
        }


	    return generatedCode.toString();
    }

    private String applyAgentColorToTrajectory(String trajectory, Optional<Agent> agent) {
	    String colorString = "AgentColor";

	    if (agent.get() instanceof Pedestrian) {
		    Color pedestrianColor;
	        Pedestrian pedestrian = (Pedestrian)agent.get();
	        if(model.config.isShowGroups()) {
		        pedestrianColor = renderer.getAgentRender().getGroupColor(pedestrian);
	        }
	        else {
		        pedestrianColor = renderer.getPedestrianColor(pedestrian);
	        }

            colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
        }

        return String.format(Locale.US, "\\draw[draw=%s]\n%s;\n", colorString, trajectory);
    }

    @NotNull
    private String drawAgents(DefaultSimulationConfig config) {
	    String generatedCode = "";

        for (Agent agent : model.getAgents()) {
        	if(agent instanceof Pedestrian) {
		        if (model.getConfig().isShowGroups()) {
			        try {
				        Pedestrian pedestrian = (Pedestrian) agent;
				        Color pedestrianColor = renderer.getAgentRender().getGroupColor(pedestrian);
				        Shape pedestrianShape = renderer.getAgentRender().getShape(pedestrian);

				        String colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
				        generatedCode += String.format(Locale.US, "\\fill[fill=%s] %s;\n", colorString, generatePathForShape(pedestrianShape));
			        } catch (ClassCastException cce) {
				        logger.error("Error casting to Pedestrian");
				        cce.printStackTrace();

				        // Fall back to default rendering of agents.
				        String agentTextPattern = "\\fill[AgentColor] (%f,%f) circle [radius=\\AgentRadius];\n";
				        generatedCode += String.format(Locale.US, agentTextPattern, agent.getPosition().x, agent.getPosition().y);
			        }
		        } else {
			        Pedestrian pedestrian = (Pedestrian) agent;
			        Color pedestrianColor = renderer.getPedestrianColor(pedestrian);
			        String colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
			        // Do not draw agents as path for performance reasons. Usually, agents have a circular shape.
			        // generatedCode += String.format(Locale.US, "\\fill[AgentColor] %s\n", generatePathForScenarioElement(agent));
			        String agentTextPattern = "\\fill[fill=%s] (%f,%f) circle [radius=\\AgentRadius];\n";
			        generatedCode += String.format(Locale.US, agentTextPattern, colorString, agent.getPosition().x, agent.getPosition().y);
		        }

		        if (model.isElementSelected() && model.getSelectedElement().equals(agent)) {
			        String agentTextPattern = "\\draw[magenta] (%f,%f) circle [radius=\\AgentRadius];\n";
			        generatedCode += String.format(Locale.US, agentTextPattern, agent.getPosition().x, agent.getPosition().y);
		        }
	        }
        }

        return generatedCode;
    }

    private String generatePathForScenarioElement(ScenarioElement element) {
		String generatedPath = "";

		AffineTransform noTransformation = new AffineTransform();
		PathIterator pathIterator = element.getShape().getPathIterator(noTransformation);

		while (!pathIterator.isDone()) {
			float[] coords = new float[6];
			int type = pathIterator.currentSegment(coords);

			generatedPath += convertJavaToTikzPath(type, coords);
			pathIterator.next();
		}

		return generatedPath.trim();
	}

	private String generatePathForStairs(Stairs element) {
		String generatedPath = "";

		AffineTransform noTransformation = new AffineTransform();
		Shape shape = DefaultRenderer.getStairShapeWithThreads(element);
		PathIterator pathIterator = shape.getPathIterator(noTransformation);

		while (!pathIterator.isDone()) {
			float[] coords = new float[6];
			int type = pathIterator.currentSegment(coords);

			generatedPath += convertJavaToTikzPath(type, coords);
			pathIterator.next();
		}

		return generatedPath.trim();
	}

    private String generatePathForShape(Shape shape) {
        String generatedPath = "";

        AffineTransform noTransformation = new AffineTransform();
        PathIterator pathIterator = shape.getPathIterator(noTransformation);

        while (!pathIterator.isDone()) {
            float[] coords = new float[6];
            int type = pathIterator.currentSegment(coords);

            generatedPath += convertJavaToTikzPath(type, coords);
            pathIterator.next();
        }

        return generatedPath.trim();
    }

	private String convertJavaToTikzPath(int type, float[] coords) {
		if (type < SEG_MOVETO || type > SEG_CLOSE) {
			throw new IllegalStateException(String.format(Locale.US, "Cannot process path segment type: %d (coordinates: %s)", type, Arrays.toString(coords)));
		}

		String convertedPath = translationTable[type];

		if (type == SEG_MOVETO) {
			convertedPath = String.format(Locale.US, convertedPath, coords[0], coords[1]);
		} else if (type == SEG_LINETO) {
			convertedPath = String.format(Locale.US, convertedPath, coords[0], coords[1]);
		} else if (type == SEG_QUADTO) {
			convertedPath = String.format(Locale.US, convertedPath, coords[0], coords[1], coords[2], coords[3]);
		} else if (type == SEG_CUBICTO) {
			convertedPath = String.format(Locale.US, convertedPath, coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
		}

		return convertedPath;
	}
}
