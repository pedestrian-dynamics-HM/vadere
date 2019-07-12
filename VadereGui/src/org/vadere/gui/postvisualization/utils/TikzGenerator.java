package org.vadere.gui.postvisualization.utils;


import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationConfig;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.state.scenario.*;
import org.vadere.state.simulation.Step;
import org.vadere.state.simulation.Trajectory;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.HalfEdge;
import org.vadere.util.voronoi.RectangleLimits;
import org.vadere.util.voronoi.VoronoiDiagram;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.awt.geom.PathIterator.SEG_CLOSE;
import static java.awt.geom.PathIterator.SEG_CUBICTO;
import static java.awt.geom.PathIterator.SEG_LINETO;
import static java.awt.geom.PathIterator.SEG_MOVETO;
import static java.awt.geom.PathIterator.SEG_QUADTO;

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

	private final static Logger logger = Logger.getLogger(TikzGenerator.class);
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
				"\\begin{tikzpicture}\n" +
				"[x=1cm,y=1cm,\n" +
				addStyles() +
				"]\n" +
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

	private String addStyles(){
		StringBuilder sb = new StringBuilder();
		sb.append("trajectory/.style={},\n");
		sb.append("pedestrian/.style={fill=AgentColor},\n"); // default color
		sb.append("walkdirection/.style={},\n");
		sb.append("ped_circle/.style={circle, minimum size=\\AgentDiameter cm, inner sep=0pt},\n");
		sb.append("selected/.style={},\n");
		sb.append("group/.style={},\n");
		return sb.toString();
	}

	private String generateTikzColorDefinitions(SimulationModel<? extends DefaultSimulationConfig> model) {
		String colorDefinitions = "% Color Definitions\n";

		String colorTextPattern = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";

		Color sourceColor = model.getConfig().getSourceColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "SourceColor", sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue());

		Color targetColor = model.getConfig().getTargetColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "TargetColor", targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());

		Color absorbingAreaColor = model.getConfig().getAbsorbingAreaColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "AbsorbingAreaColor", absorbingAreaColor.getRed(), absorbingAreaColor.getGreen(), absorbingAreaColor.getBlue());

		Color obstacleColor = model.getConfig().getObstacleColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "ObstacleColor", obstacleColor.getRed(), obstacleColor.getGreen(), obstacleColor.getBlue());

		Color stairColor = model.getConfig().getStairColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "StairColor", stairColor.getRed(), stairColor.getGreen(), stairColor.getBlue());

		Color measurementAreaColor = model.getConfig().getMeasurementAreaColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "MeasurementAreaColor", measurementAreaColor.getRed(), measurementAreaColor.getGreen(), measurementAreaColor.getBlue());

		Color agentColor = model.getConfig().getPedestrianDefaultColor();
		colorDefinitions += String.format(Locale.US, colorTextPattern, "AgentColor", agentColor.getRed(), agentColor.getGreen(), agentColor.getBlue());

		colorDefinitions += String.format(Locale.US, colorTextPattern, "AgentIdColor", 255, 127, 0); // This orange color is hard-coded in "DefaultRenderer".

		colorDefinitions += "\n";

		return colorDefinitions;
	}

	private String generateTikzDrawSettings(SimulationModel<? extends DefaultSimulationConfig> model) {
		// Generate TeX variables for common draw settings like agent radius and
		// use them later on when generating TikZ code. These settins can be used
		// by TikZ users to adapt the drawing quickly.
		String drawSettings = "% Draw Settings\n";

		double agentRadius = model.getConfig().getPedestrianTorso() / 2.0;
		double opacityBetweenZeroAndOne = model.getConfig().getMeasurementAreaAlpha() / 255.0;

		drawSettings += String.format(Locale.US,"\\newcommand{\\AgentRadius}{%f}\n", agentRadius);
		drawSettings += String.format(Locale.US,"\\newcommand{\\AgentDiameter}{%f}\n", 2*agentRadius);
		drawSettings += String.format(Locale.US,"\\newcommand{\\LineWidth}{%d}\n", 1);
		drawSettings += String.format(Locale.US,"\\newcommand{\\MeasurementAreaOpacity}{%f}\n", opacityBetweenZeroAndOne);

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
				VPoint centroid = source.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (src_%d) at (%f,%f); %%centroid coordinate for source %d\n", source.getId(), centroid.x, centroid.y, source.getId());
				generatedCode += String.format(Locale.US, "\\fill[SourceColor] %s;\n", generatePathForScenarioElement(source));
			}
		} else {
			generatedCode += "% Sources (not enabled in config)\n";
		}

		if (config.isShowTargets()) {
			generatedCode += "% Targets\n";
			for (Target target : topography.getTargets()) {
				VPoint centroid = target.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (trg_%d) at (%f,%f); %%centroid coordinate for target %d\n", target.getId(), centroid.x, centroid.y, target.getId());
				generatedCode += String.format(Locale.US, "\\fill[TargetColor] %s;\n", generatePathForScenarioElement(target));
			}
		} else {
			generatedCode += "% Targets (not enabled in config)\n";
		}

		if (config.isShowAbsorbingAreas()) {
			generatedCode += "% Absorbing Areas\n";
			for (AbsorbingArea absorbingArea : topography.getAbsorbingAreas()) {
				VPoint centroid = absorbingArea.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (absorb_%d) at (%f,%f); %%centroid coordinate for absorbingArea %d\n", absorbingArea.getId(), centroid.x, centroid.y, absorbingArea.getId());
				generatedCode += String.format(Locale.US, "\\fill[AbsorbingAreaColor] %s;\n", generatePathForScenarioElement(absorbingArea));
			}
		} else {
			generatedCode += "% Absorbing Areas (not enabled in config)\n";
		}

		if (config.isShowObstacles()) {
			generatedCode += "% Obstacles\n";
			for (Obstacle obstacle : topography.getObstacles()) {
				VPoint centroid = obstacle.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (obs_%d) at (%f,%f); %%centroid coordinate for obstacle %d\n", obstacle.getId(), centroid.x, centroid.y, obstacle.getId());
				generatedCode += String.format(Locale.US, "\\fill[ObstacleColor] %s;\n", generatePathForScenarioElement(obstacle));
			}
		} else {
			generatedCode += "% Obstacles (not enabled in config)\n";
		}

		if (config.isShowStairs()) {
			generatedCode += "% Stairs\n";
			for (Stairs stair : topography.getStairs()) {
				VPoint centroid = stair.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (str_%d) at (%f,%f); %%centroid coordinate for stair %d\n", stair.getId(), centroid.x, centroid.y, stair.getId());
				generatedCode += String.format(Locale.US, "\\fill[black] %s;\n", generatePathForScenarioElement(stair));
				generatedCode += String.format(Locale.US, "\\fill[StairColor] %s;\n", generatePathForStairs(stair));
			}
		} else {
			generatedCode += "% Stairs (not enabled in config)\n";
		}

		if (config.isShowMeasurementArea()) {
			generatedCode += "% Measurement Areas\n";
			for (MeasurementArea measurementArea : topography.getMeasurementAreas()) {
				VPoint centroid = measurementArea.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (mrmtA_%d) at (%f,%f); %%centroid coordinate for measurementArea %d\n", measurementArea.getId(), centroid.x, centroid.y, measurementArea.getId());
				generatedCode += String.format(Locale.US, "\\fill[MeasurementAreaColor,opacity=\\MeasurementAreaOpacity] %s;\n", generatePathForScenarioElement(measurementArea));
			}
		} else {
			generatedCode += "% Measurement Areas (not enabled in config)\n";
		}

		if (model.isVoronoiDiagramVisible() && model.isVoronoiDiagramAvailable()) {
			generatedCode += "% Voronoi Diagram\n";
			generatedCode += drawVoronoiDiagram(model.getVoronoiDiagram());
		} else {
			generatedCode += "% Voronoi Diagram (not enabled in config)\n";
		}

        if (config.isShowTrajectories()) {
            generatedCode += "% Trajectories\n";

            if (model instanceof PostvisualizationModel) {
                generatedCode += drawTrajectories((PostvisualizationConfig) config, (PostvisualizationModel)model);
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
            generatedCode += "% Agents (not enabled in config)\n";
        }

        if (config.isShowPedestrianIds()) {
			generatedCode += "% Agent Ids\n";

			for (Agent agent : model.getAgents()) {
				generatedCode += String.format(Locale.US, "\\node[text=AgentIdColor] (pedIdNode_id%d) at (%f,%f) {\\textbf{%d}};\n",
						agent.getId(), agent.getPosition().x, agent.getPosition().y, agent.getId());
			}
		} else {
        	generatedCode += "% Agent Ids (not enabled in config)\n";
		}

        if (!topography.hasBoundary()){
        	int id=1;
			for(Obstacle obstacle : Topography.createObstacleBoundary(topography)) {
				VPoint centroid = obstacle.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (bound_%d) at (%f,%f); %%centroid coordinate for obstacle %d\n", id, centroid.x, centroid.y, obstacle.getId());
				generatedCode += String.format(Locale.US, "\\fill[ObstacleColor] %s;\n", generatePathForScenarioElement(obstacle));
				id++;
			}
		}

        return generatedCode;
	}

    private String drawTrajectories(PostvisualizationConfig config, PostvisualizationModel model) {
	    // Use a thread-safe string implementation because streams are used here.
        final StringBuffer generatedCode = new StringBuffer("");

	    Stream<Trajectory> trajectoryStream = config.isShowAllTrajOnSnapshot() ? model.getAppearedPedestrians() : model.getAlivePedestrians();
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

        return String.format(Locale.US, "\\draw[trajectory, draw=%s]\n%s;\n", colorString, trajectory);
    }

	/**
	 * Draw a small direction triangle which points in the direction of of the last step.
	 * The previous step is not always the step one time step earlier. If the agent did not
	 * move in the last time step.
	 *
	 * @param agent
	 * @return		tikz code for small direction triangle.
	 */
	private String drawWalkingDirection(final Agent agent){
		String tikzCode= "";
		PostvisualizationModel postVisModel = (PostvisualizationModel)model;
		Step currentTimeStep = postVisModel.getStep().orElse(null);
		// ensure their is a current timeStep and its not the first. (If the first there is no previous)
		if (currentTimeStep != null && currentTimeStep.getStepNumber() > 1){
			Step previousTimeStep = new Step(currentTimeStep.getStepNumber()-1);
			Trajectory trajectory = postVisModel.getTrajectory(agent.getId());
			if (trajectory != null){
				Agent currAgent = trajectory.getAgent(currentTimeStep).orElse(null);
				Agent prevAgent = trajectory.getAgent(previousTimeStep).orElse(null);
				while (currAgent != null && prevAgent !=null && currAgent.getPosition().equals(prevAgent.getPosition())){
					previousTimeStep = new Step(previousTimeStep.getStepNumber() -1);
					prevAgent = trajectory.getAgent(previousTimeStep).orElse(null);
				}
				if (currAgent != null && prevAgent !=null){
					Vector2D direction = new Vector2D(new VPoint(
							currAgent.getPosition().x - prevAgent.getPosition().x,
							currAgent.getPosition().y - prevAgent.getPosition().y));
					direction.normalize(1);
					double r = currAgent.getRadius();
					VPoint p1 = currAgent.getPosition().add(direction.rotate(Math.PI/2).normalize(0.93*r));
					VPoint p2 = currAgent.getPosition().add(direction.rotate(-Math.PI/2).normalize(0.93*r));
					VPoint p3 = currAgent.getPosition().add(direction.normalize(1.8*r));
					String directionStr = "\\draw[walkdirection] (%f,%f) -- (%f,%f) -- (%f,%f);\n";
					tikzCode = String.format(Locale.US, directionStr, p1.x, p1.y, p3.x, p3.y, p2.x, p2.y);
				}
			}
		}
		return tikzCode;
	}

    @NotNull
    private String drawAgents(DefaultSimulationConfig config) {
	    String generatedCode = "";

        for (Agent agent : model.getAgents()) {
        	if(agent instanceof Pedestrian) {

				if(config.isShowWalkdirection() && model instanceof PostvisualizationModel){
					generatedCode += drawWalkingDirection(agent);
				}

		        if (model.getConfig().isShowGroups()) {
			        try {
				        Pedestrian pedestrian = (Pedestrian) agent;
				        Color pedestrianColor = renderer.getAgentRender().getGroupColor(pedestrian);
				        Shape pedestrianShape = renderer.getAgentRender().getShape(pedestrian);

				        String colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
				        generatedCode += String.format(Locale.US, "\\fill[pedestrian, group, fill=%s] %s;\n", colorString, generatePathForShape(pedestrianShape));
			        } catch (ClassCastException cce) {
				        logger.error("Error casting to Pedestrian");
				        cce.printStackTrace();

				        // Fall back to default rendering of agents.
				        String agentTextPattern = "\\node[ped_circle, pedestrian, fill=AgentColor] (%f,%f) (ped_%d) at (%f,%f) {};\n";
				        generatedCode += String.format(Locale.US, agentTextPattern, agent.getId(), agent.getPosition().x, agent.getPosition().y);
			        }
		        } else {
			        Pedestrian pedestrian = (Pedestrian) agent;
			        Color pedestrianColor = renderer.getPedestrianColor(pedestrian);

			        if (pedestrianColor.equals(model.getConfig().getPedestrianDefaultColor())){
						String agentTextPattern = "\\node[ped_circle, pedestrian] (ped_%d) at (%f,%f) {};\n";
						generatedCode += String.format(Locale.US, agentTextPattern, agent.getId(), agent.getPosition().x, agent.getPosition().y);
					} else {
			        	// override default color
						String colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
						// Do not draw agents as path for performance reasons. Usually, agents have a circular shape.
						// generatedCode += String.format(Locale.US, "\\fill[AgentColor] %s\n", generatePathForScenarioElement(agent));
						String agentTextPattern = "\\node[ped_circle, pedestrian, fill=%s] (ped_%d) at (%f,%f) {};\n";
						generatedCode += String.format(Locale.US, agentTextPattern, colorString,  agent.getId(), agent.getPosition().x, agent.getPosition().y);
					}
		        }

		        if (model.isElementSelected() && model.getSelectedElement().equals(agent)) {
			        String agentTextPattern = "\\draw[ped_circle, pedestrian, selected, magenta] (ped_%d) at (%f,%f) {};\n";
			        generatedCode += String.format(Locale.US, agentTextPattern,  agent.getId(), agent.getPosition().x, agent.getPosition().y);
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

	private String drawVoronoiDiagram(final VoronoiDiagram voronoiDiagram) {
		String voronoiDiagramAsTikz = "";

		synchronized (voronoiDiagram) {

			if (voronoiDiagram != null) {
				RectangleLimits limits = voronoiDiagram.getLimits();

				voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[black, line width=\\LineWidth] (%f,%f) rectangle (%f,%f);\n",
						limits.xLow, limits.yLow,
						limits.xHigh, limits.yLow);
			}

			if (voronoiDiagram != null && voronoiDiagram.getFaces() != null) {

				for (Face face : voronoiDiagram.getFaces()) {

					boolean go = true;
					boolean closed = false;
					HalfEdge last = face.getOuterComponent();
					HalfEdge next = last.getNext();
					HalfEdge outerComponent = last;

					while (go) {
						if (next == null || last.getOrigin() == null) {
							go = false;
							closed = true;
						} else {

							voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[black, line width=\\LineWidth] (%f,%f) to (%f,%f);\n",
									last.getOrigin().x, last.getOrigin().y,
									next.getOrigin().x, next.getOrigin().y);

							if (next == outerComponent) {
								go = false;
							} else {
								last = next;
								next = next.getNext();
							}
						}
					}

					last = outerComponent;
					next = last.getPrevious();

					go = true;

					while (go && !closed) {
						if (next == null || next.getOrigin() == null) {
							go = false;
						} else {
							voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[black, line width=\\LineWidth] (%f,%f) to (%f,%f);\n",
									last.getOrigin().x, last.getOrigin().y,
									next.getOrigin().x, next.getOrigin().y);

							if (next == outerComponent) {
								go = false;
							} else {
								last = next;
								next = next.getPrevious();
							}
						}
					}
				}
			}
		}

		return voronoiDiagramAsTikz;
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
