package org.vadere.gui.postvisualization.utils;

import org.jetbrains.annotations.NotNull;
import org.vadere.gui.components.model.DefaultSimulationConfig;
import org.vadere.gui.components.model.SimulationModel;
import org.vadere.gui.components.view.DefaultRenderer;
import org.vadere.gui.components.view.SimulationRenderer;
import org.vadere.gui.postvisualization.model.PostvisualizationConfig;
import org.vadere.gui.postvisualization.model.PostvisualizationModel;
import org.vadere.gui.postvisualization.model.TableTrajectoryFootStep;
import org.vadere.state.psychology.cognition.GroupMembership;
import org.vadere.state.scenario.*;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.logging.Logger;
import org.vadere.util.voronoi.Face;
import org.vadere.util.voronoi.HalfEdge;
import org.vadere.util.voronoi.RectangleLimits;
import org.vadere.util.voronoi.VoronoiDiagram;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.awt.geom.PathIterator.*;

/**
 * Convert the (Java) scenario elements to a TikZ representation.
 *
 * The conversion is done by using a simple Java-to-TikZ translation table.
 * This generator loops over all scenario elements and converts them to TikZ code.
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

	public enum EXPORT_OPTION { EXPORT_WHOLE_TOPOGRAPHY, EXPORT_CURRENT_VIEWPORT };

	// Static Variables
	private final static Logger logger = Logger.getLogger(TikzGenerator.class);

	// Member Variables
	private final SimulationRenderer renderer;
	private final SimulationModel<? extends DefaultSimulationConfig> model;
	private final String[] translationTable;

	// Constructors
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

	// Methods
	public void generateTikz(final File file, EXPORT_OPTION exportOption) {
		String tikzCodeColorDefinitions = TikzStyleGenerator.generateTikzColorDefinitions(model.getConfig());
		String tikzCodeScenarioElements = convertScenarioElementsToTikz(exportOption);

		String tikzOutput = "" +
				"\\RequirePackage{luatex85}\n" +
				"\\documentclass{standalone}\n" +
				"\\usepackage{tikz}\n\n" +
				tikzCodeColorDefinitions +
				"\\begin{document}\n" +
                "% Change scaling to [x=1mm,y=1mm] if TeX reports \"Dimension too large\".\n" +
				"\\begin{tikzpicture}\n" +
				"[x=1cm,y=1cm,\n" +
				TikzStyleGenerator.generateTikzStyles(model.getConfig().getPedestrianTorso()) +
				"]\n" +
				tikzCodeScenarioElements +
				"\\end{tikzpicture}\n" +
				"\\end{document}\n";

		// TODO: Maybe uses Java's resources notation (in general, writing the file should be done by the caller not here).
		try {
			file.createNewFile();
			Writer out = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
			out.write(tikzOutput);
			out.flush();
			logger.info("generate new TikZ: " + file.getAbsolutePath());
		} catch (IOException e1) {
			logger.error(e1.getMessage());
			e1.printStackTrace();
		}
	}

	private String convertScenarioElementsToTikz(EXPORT_OPTION exportOption) {
		String generatedCode = "";

		DefaultSimulationConfig config = model.getConfig();
		Topography topography = model.getTopography();

		Rectangle2D exportBounds = getExportBounds(exportOption, topography);

		// Clip everything outside of the user-defined bound.
		generatedCode += "% Clipping\n";
		String clipTextPattern = "\\clip (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(Locale.US, clipTextPattern,
				exportBounds.getX(),
				exportBounds.getY(),
				exportBounds.getX() + exportBounds.getWidth(),
				exportBounds.getY() + exportBounds.getHeight());

		// Draw background elements first, then other scenario elements on top.
		generatedCode += "% Background\n";
		String groundTextPattern = (config.isShowGrid()) ? "\\draw[help lines] (%f,%f) grid (%f,%f);\n" : "\\fill[white] (%f,%f) rectangle (%f,%f);\n";
		generatedCode += String.format(Locale.US, groundTextPattern,
				topography.getBounds().x,
				topography.getBounds().y,
				topography.getBounds().x + topography.getBounds().width,
				topography.getBounds().y + topography.getBounds().height);

		generatedCode += createTikzCodeForScenarioElement(config.isShowTargetChangers(), topography.getTargetChangers(), "TargetChanger", "TargetChangerColor");
		generatedCode += createTikzCodeForScenarioElement(config.isShowSources(), topography.getSources(), "Source", "SourceColor");
		generatedCode += createTikzCodeForScenarioElement(config.isShowTargets(), topography.getTargets(), "Target", "TargetColor");
		generatedCode += createTikzCodeForScenarioElement(config.isShowAbsorbingAreas(), topography.getAbsorbingAreas(), "AbsorbingArea", "AbsorbingAreaColor");
		generatedCode += createTikzCodeForScenarioElement(config.isShowObstacles(), topography.getObstacles(), "Obstacle", "ObstacleColor");

		if (config.isShowAerosolClouds()) {
			generatedCode += "% Aerosol Clouds\n";
			for (AerosolCloud aerosolCloud : topography.getAerosolClouds()) {
				VPoint centroid = aerosolCloud.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (AerosolCloud%d) at (%f,%f); %% Centroid: AerosolCloud %d\n", aerosolCloud.getId(), centroid.x, centroid.y, aerosolCloud.getId());
				double normalizedPathogenConcentration = aerosolCloud.getPathogenConcentration() / model.getConfig().getAerosolCloudMaxPathogenConcentration();
				generatedCode += String.format(Locale.US, "\\fill[AerosolCloudColor,opacity=\\AerosolCloudOpacity*%f] %s;\n", normalizedPathogenConcentration, generatePathForScenarioElement(aerosolCloud));
			}
		} else {
			generatedCode += "% Aerosol clouds (not enabled in config)\n";
		}

		if (config.isShowStairs()) {
			generatedCode += "% Stairs\n";
			for (Stairs stair : topography.getStairs()) {
				VPoint centroid = stair.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (Stair%d) at (%f,%f); %% Centroid: Stair %d\n", stair.getId(), centroid.x, centroid.y, stair.getId());
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
				generatedCode += String.format(Locale.US, "\\coordinate (MeasurementArea%d) at (%f,%f); %% Centroid: MeasurementArea %d\n", measurementArea.getId(), centroid.x, centroid.y, measurementArea.getId());
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

        if (config.isShowWalkdirection()) {
        	List<Agent> aliveAgents = model.getAgents().stream().filter(agent -> model.isAlive(agent.getId())).collect(Collectors.toList());
            for (Agent agent : aliveAgents) {
				if (model instanceof PostvisualizationModel) {
					generatedCode += drawWalkingDirectionUsingTrajectoryFile(agent);
				} else {
					generatedCode += drawWalkingDirectionUsingFootstepHistory((Pedestrian)agent);
				}
            }
        }

        if (config.isShowPedestrianIds()) {
			generatedCode += "% Agent Ids\n";

			for (Agent agent : model.getAgents()) {
				generatedCode += String.format(Locale.US, "\\node[text=AgentIdColor] (PedestrianId%d) at (%f,%f) {\\textbf{%d}};\n",
						agent.getId(), agent.getPosition().x, agent.getPosition().y, agent.getId());
			}
		} else {
        	generatedCode += "% Agent Ids (not enabled in config)\n";
		}

        if (!topography.hasBoundary()){
			generatedCode += "% Topography Boundary\n";
        	int id=1;
			for(Obstacle obstacle : Topography.createObstacleBoundary(topography)) {
				VPoint centroid = obstacle.getShape().getCentroid();
				generatedCode += String.format(Locale.US, "\\coordinate (TopographyBoundary%d) at (%f,%f); %% Centroid: TopographyBoundary %d\n", id, centroid.x, centroid.y, obstacle.getId());
				generatedCode += String.format(Locale.US, "\\fill[ObstacleColor] %s;\n", generatePathForScenarioElement(obstacle));
				id++;
			}
		}

		if (model.getSelectedElement() != null) {
			generatedCode += "% Selected Elements\n";
			generatedCode += String.format(Locale.US, "\\draw[selected] %s;\n", generatePathForScenarioElement(model.getSelectedElement()));
		}

        return generatedCode;
	}

	private Rectangle2D getExportBounds(EXPORT_OPTION exportOption, Topography topography) {
		Rectangle2D exportBounds = new Rectangle2D.Double();

		if (exportOption == EXPORT_OPTION.EXPORT_WHOLE_TOPOGRAPHY) {
			exportBounds.setRect(topography.getBounds());
		} else {
			Rectangle2D viewportBound = model.getViewportBound();

			// When exporting the current viewport only, consider that the viewport is usually cut off
			// only horizontally or vertically. I.e, the dimension which is not cut off is filled up with a lot
			// of gray margin. Therefore, limit this dimension to the corresponding topography dimension.
			double width = Math.min(viewportBound.getWidth(), topography.getBounds().getWidth());
			double height = Math.min(viewportBound.getHeight(), topography.getBounds().getHeight());

			exportBounds.setRect(viewportBound.getX(), viewportBound.getY(), width, height);
		}

		return exportBounds;
	}

	private String createTikzCodeForScenarioElement(boolean enabled, List<? extends ScenarioElement> scenarioElements, String labelPrefix, String fillColor) {
		String generatedCode = String.format("%% %s(s) (not enabled in config)\n", labelPrefix);

		if (enabled) {
			generatedCode = String.format("%% %s(s)\n", labelPrefix);
			for (ScenarioElement element : scenarioElements) {
				VPoint centroid = element.getShape().getCentroid();

				generatedCode += String.format(Locale.US, "\\coordinate (%s%d) at (%f,%f); %% Centroid: %s%d\n",
						labelPrefix, element.getId(),
						centroid.x, centroid.y,
						labelPrefix, element.getId());

				generatedCode += String.format(Locale.US, "\\fill[%s] %s;\n", fillColor, generatePathForScenarioElement(element));
			}
		}

		return generatedCode;
	}

	private String generatePathForScenarioElement(ScenarioElement element) {
		return generatePathForShape(element.getShape());
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

	private String drawTrajectories(PostvisualizationConfig config, PostvisualizationModel model) {
		// Use a thread-safe string implementation because streams are used here.
		final StringBuffer generatedCode = new StringBuffer("");

		if (config.isShowTrajectories()) {
			Collection<Agent> agents = model.getAgents();

			TableTrajectoryFootStep trajectories = model.getTrajectories();
			Table dataFrame;

			if (model.config.isShowAllTrajectories()) {
				dataFrame = model.getAppearedPedestrians();
			} else {
				dataFrame = model.getAlivePedestrians();
			}

			Map<Integer, Agent> agentColors = new HashMap<>();
			agents.forEach(agent -> agentColors.put(agent.getId(), agent));

			for(Agent agent : agents) {
				StringBuffer trajectory = new StringBuffer("");
				Table slice = dataFrame.where(dataFrame.intColumn(trajectories.pedIdCol).isEqualTo(agent.getId()));

				for(Row row : slice) {
					boolean isLastStep = row.getDouble(trajectories.endTimeCol) > model.getSimTimeInSec();
					double startX = row.getDouble(trajectories.startXCol);
					double startY = row.getDouble(trajectories.startYCol);
					double endX = row.getDouble(trajectories.endXCol);
					double endY = row.getDouble(trajectories.endYCol);

					if(isLastStep) {
						VPoint interpolatedPos = FootStep.interpolateFootStep(startX, startY, endX, endY, row.getDouble(trajectories.startTimeCol), row.getDouble(trajectories.endTimeCol), model.getSimTimeInSec());
						endX = interpolatedPos.getX();
						endY = interpolatedPos.getY();
					}

					trajectory.append(String.format(Locale.US, "(%f,%f) to ", startX, startY));

					if(row.getRowNumber() == slice.rowCount()-1) {
						trajectory.append(String.format(Locale.US, "(%f,%f)", endX, endY));
					}
				}

				String coloredTrajectory = applyAgentColorToTrajectory(trajectory.toString(), Optional.ofNullable(agentColors.get(agent.getId())));
				generatedCode.append(String.format(Locale.US, "%% Trajectory Agent %d at simTimeInSec %f\n", agent.getId(), model.getSimTimeInSec()));
				generatedCode.append(coloredTrajectory);
			}
		}

		return generatedCode.toString();
	}

    private String applyAgentColorToTrajectory(String trajectory, Optional<Agent> agent) {
	    String colorString = "trajectory, draw=AgentColor";

		if (agent.get() instanceof Pedestrian) {
			Pedestrian pedestrian = (Pedestrian) agent.get();
			ScenarioElement selectedElement = model.getSelectedElement();

			if (selectedElement instanceof Pedestrian && selectedElement.getId() == pedestrian.getId()) {
				colorString = "selected";
			} else {
				Color pedestrianColor = renderer.getPedestrianColor(pedestrian);
				colorString = String.format(Locale.US, "trajectory, draw={rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
			}
		}

        return String.format(Locale.US, "\\draw[%s]\n%s;\n", colorString, trajectory);
    }

	/**
	 * Draw a small direction triangle which points in the direction of of the last step.
	 * If the agent did not move in the last simulation step, no triangle is drawn.
	 *
	 * @param agent
	 * @return		tikz code for small direction triangle.
	 */
	private String drawWalkingDirectionUsingTrajectoryFile(final Agent agent) {
		String tikzCode= "";

		PostvisualizationModel postVisModel = (PostvisualizationModel)model;

		// If step == 1, there is no previous one and we cannot derive a walking direction!
		// Otherwise, use the last footstep in our data table for the given agent.
		if (postVisModel.getStep() > 1) {
			TableTrajectoryFootStep trajectories = postVisModel.getTrajectories();
			Table dataFrame = postVisModel.getAppearedPedestrians();
			Table trajectorySlice = dataFrame
					.where(dataFrame.intColumn(trajectories.pedIdCol).isEqualTo(agent.getId()));

			if (trajectorySlice.rowCount() >= 1) {
				// New rows are added to the end of the table.
				Table latestFootstep = trajectorySlice.last(1);
				int row = 0;

				double startX = latestFootstep.doubleColumn(trajectories.startXCol).getDouble(row);
				double startY = latestFootstep.doubleColumn(trajectories.startYCol).getDouble(row);
				double endX = latestFootstep.doubleColumn(trajectories.endXCol).getDouble(row);
				double endY = latestFootstep.doubleColumn(trajectories.endYCol).getDouble(row);

				List<VPoint> trianglePoints = calculateTrianglePointsForWalkingDirection(startX, startY, endX, endY,
						agent.getPosition(), agent.getRadius());
				tikzCode = createWalkingDirectionTikzString(trianglePoints);
			}
		}
		return tikzCode;
	}

	private String drawWalkingDirectionUsingFootstepHistory(final Pedestrian agent) {
		String tikzCode= "";

		if (agent.getFootstepHistory().size() > 0) {
			FootStep footStep = agent.getFootstepHistory().getYoungestFootStep();

			List<VPoint> trianglePoints = calculateTrianglePointsForWalkingDirection(footStep.getStart().x, footStep.getStart().y,
					footStep.getEnd().x, footStep.getEnd().y,
					agent.getPosition(), agent.getRadius());
			tikzCode = createWalkingDirectionTikzString(trianglePoints);
		}

		return tikzCode;
	}

	private List<VPoint> calculateTrianglePointsForWalkingDirection(double startX, double startY, double endX, double endY, VPoint basePoint, double normalizeFactor) {
		Vector2D direction = new Vector2D(new VPoint(
				endX - startX,
				endY - startY));

		VPoint p1 = basePoint.add(direction.rotate(Math.PI/2).normalize(0.93 * normalizeFactor));
		VPoint p2 = basePoint.add(direction.rotate(-Math.PI/2).normalize(0.93 * normalizeFactor));
		VPoint p3 = basePoint.add(direction.normalize(1.8 * normalizeFactor));

		return Arrays.asList(p1, p2, p3);
	}

	private String createWalkingDirectionTikzString(List<VPoint> trianglePoints) {
		String tikzTemplate = "\\draw[walkdirection] (%f,%f) -- (%f,%f) -- (%f,%f);\n";

		return String.format(Locale.US, tikzTemplate,
				trianglePoints.get(0).x, trianglePoints.get(0).y,
				trianglePoints.get(2).x, trianglePoints.get(2).y,
				trianglePoints.get(1).x, trianglePoints.get(1).y);
	}

    @NotNull
    private String drawAgents(DefaultSimulationConfig config) {
	    String generatedCode = "";

        for (Agent agent : model.getAgents()) {
        	if (agent instanceof Pedestrian && (model.config.isShowFaydedPedestrians() || model.isAlive(agent.getId()))) {
		        if (model.getConfig().isShowGroups()) {
					generatedCode += drawAgentWithGroupSettings(agent);
				} else {
					generatedCode += drawAgentWithoutGroupSettings(agent);
				}
	        }
        }

        return generatedCode;
    }

	@NotNull
	private String drawAgentWithGroupSettings(Agent agent) {
		String generatedCode = "";

		try {
			Pedestrian pedestrian = (Pedestrian) agent;
			Color pedestrianColor = renderer.getPedestrianColor(agent);
			Shape pedestrianShape = renderer.getAgentRender().getShape(pedestrian);

			String colorString = String.format(Locale.US, "{rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
			generatedCode += String.format(Locale.US, "\\fill[pedestrian, group, fill=%s] %s;\n", colorString, generatePathForShape(pedestrianShape));
		} catch (ClassCastException cce) {
			logger.error("Error casting to Pedestrian");
			cce.printStackTrace();
			// Fall back to default rendering of agents.
			String agentTextPattern = "\\node[pedestrian] (Pedestrian%d) at (%f,%f) {};\n";
			generatedCode += String.format(Locale.US, agentTextPattern, agent.getId(), agent.getPosition().x, agent.getPosition().y);
		}

		return generatedCode;
	}

	@NotNull
	private String drawAgentWithoutGroupSettings(Agent agent) {
		String generatedCode = "";

		// Do not draw agents as path for performance reasons. Usually, agents have a circular shape.
		// generatedCode += String.format(Locale.US, "\\fill[AgentColor] %s\n", generatePathForScenarioElement(agent));
		try {
			String agentTextPattern = "\\node[pedestrian%s] (Pedestrian%d) at (%f,%f) {};\n";
			// Watch out: "pedestrianModifications", e.g. "fill=black" can overwrite TikZ style "pedestrian".
			// I.e., last "fill" wins in TikZ and previous ones are ignored!
			String pedestrianModifications = getPedestrianModifications((Pedestrian) agent);
			generatedCode += String.format(Locale.US, agentTextPattern, pedestrianModifications,  agent.getId(), agent.getPosition().x, agent.getPosition().y);
		} catch (ClassCastException cce) {
			logger.error("Error casting to Pedestrian");
			cce.printStackTrace();
			// Fall back to default rendering of agents.
			String agentTextPattern = "\\node[pedestrian] (Pedestrian%d) at (%f,%f) {};\n";
			generatedCode += String.format(Locale.US, agentTextPattern, agent.getId(), agent.getPosition().x, agent.getPosition().y);
		}

		return generatedCode;
	}

	private String getPedestrianModifications(Pedestrian pedestrian) {
		String agentModifications = "";

		Color pedestrianColor = renderer.getPedestrianColor(pedestrian);

		if (pedestrianColor.equals(model.getConfig().getPedestrianDefaultColor()) == false) {
			agentModifications += String.format(", fill={rgb,255: red,%d; green,%d; blue,%d}", pedestrianColor.getRed(), pedestrianColor.getGreen(), pedestrianColor.getBlue());
		}
		if (model.getConfig().isShowPedestrianInOutGroup()) {
			agentModifications += String.format(", draw=%s, line width=2", getTikzColorString(pedestrian.getGroupMembership()));
		}

		return agentModifications;
	}

	private String getTikzColorString(GroupMembership groupMembership) {
		Color color = model.getConfig().getGroupMembershipColor(groupMembership);

		return String.format("{rgb,255: red,%d; green,%d; blue,%d}", color.getRed(), color.getGreen(), color.getBlue());
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

	private String drawVoronoiDiagram(final VoronoiDiagram voronoiDiagram) {
		String voronoiDiagramAsTikz = "";

		synchronized (voronoiDiagram) {

			if (voronoiDiagram != null) {
				RectangleLimits limits = voronoiDiagram.getLimits();

				voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[voronoi] (%f,%f) rectangle (%f,%f);\n",
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

							voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[voronoi] (%f,%f) to (%f,%f);\n",
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
							voronoiDiagramAsTikz += String.format(Locale.US, "\\draw[voronoi] (%f,%f) to (%f,%f);\n",
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

}
