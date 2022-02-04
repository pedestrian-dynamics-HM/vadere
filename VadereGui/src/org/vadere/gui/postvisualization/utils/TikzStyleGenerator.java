package org.vadere.gui.postvisualization.utils;

import org.vadere.gui.components.model.DefaultSimulationConfig;

import java.awt.*;
import java.util.Locale;

/**
 * A helper class to generate TikZ styles for the LaTeX preamble and the "\begin{tikzpicture}[...]" command.
 *
 * For Instance:
 * <pre>
 * % Preamble
 * \definecolor{SourceColor}{RGB}{1,2,3}
 * </pre>
 *
 * or:
 *
 * <pre>
 * \begin{tikzpicture}[trajectory/.style={line width=1}]
 * </pre>
 */
public class TikzStyleGenerator {
    
    // Methods
    public static String generateTikzColorDefinitions(DefaultSimulationConfig simulationConfig) {
        String colorDefinitions = "% Color Definitions\n";

        String tikzColorTemplate = "\\definecolor{%s}{RGB}{%d,%d,%d}\n";
        
        Color sourceColor = simulationConfig.getSourceColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "SourceColor", sourceColor.getRed(), sourceColor.getGreen(), sourceColor.getBlue());

        Color targetColor = simulationConfig.getTargetColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "TargetColor", targetColor.getRed(), targetColor.getGreen(), targetColor.getBlue());

        Color targetChangerColor = simulationConfig.getTargetChangerColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "TargetChangerColor", targetChangerColor.getRed(), targetChangerColor.getGreen(), targetChangerColor.getBlue());

        Color absorbingAreaColor = simulationConfig.getAbsorbingAreaColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "AbsorbingAreaColor", absorbingAreaColor.getRed(), absorbingAreaColor.getGreen(), absorbingAreaColor.getBlue());

        Color obstacleColor = simulationConfig.getObstacleColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "ObstacleColor", obstacleColor.getRed(), obstacleColor.getGreen(), obstacleColor.getBlue());

        Color stairColor = simulationConfig.getStairColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "StairColor", stairColor.getRed(), stairColor.getGreen(), stairColor.getBlue());

        Color measurementAreaColor = simulationConfig.getMeasurementAreaColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "MeasurementAreaColor", measurementAreaColor.getRed(), measurementAreaColor.getGreen(), measurementAreaColor.getBlue());

        Color aerosolCloudColor = simulationConfig.getAerosolCloudColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "AerosolCloudColor", aerosolCloudColor.getRed(), aerosolCloudColor.getGreen(), aerosolCloudColor.getBlue());

        Color agentColor = simulationConfig.getPedestrianDefaultColor();
        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "AgentColor", agentColor.getRed(), agentColor.getGreen(), agentColor.getBlue());

        colorDefinitions += String.format(Locale.US, tikzColorTemplate, "AgentIdColor", 255, 127, 0); // This orange color is hard-coded in "DefaultRenderer".
        colorDefinitions += "\n";

        double opacityBetweenZeroAndOne = simulationConfig.getMeasurementAreaAlpha() / 255.0;
        colorDefinitions += String.format(Locale.US,"\\newcommand{\\MeasurementAreaOpacity}{%f}\n", opacityBetweenZeroAndOne);

        double aerosolCloudOpacityBetweenZeroAndOne = simulationConfig.getAerosolCloudAlphaMax() / 255.0;
        colorDefinitions += String.format(Locale.US,"\\newcommand{\\AerosolCloudOpacity}{%f}\n", aerosolCloudOpacityBetweenZeroAndOne);

        colorDefinitions += "\n";

        return colorDefinitions;
    }

    public static String generateTikzStyles(double pedestrianRadius) {
        String tikzStyles = "";

        tikzStyles += "trajectory/.style={line width=1},\n";
        tikzStyles += String.format("pedestrian/.style={circle, fill=AgentColor, minimum size=%f cm},\n", pedestrianRadius);
        tikzStyles += "walkdirection/.style={black, line width=1},\n";
        tikzStyles += "selected/.style={draw=magenta, line width=2},\n";
        tikzStyles += "group/.style={},\n";
        tikzStyles += "voronoi/.style={black, line width=1}\n";

        return tikzStyles;
    }
    
}
