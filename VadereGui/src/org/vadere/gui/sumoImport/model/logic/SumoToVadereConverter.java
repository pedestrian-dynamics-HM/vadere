package org.vadere.gui.sumoImport.model.logic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.jetbrains.annotations.NotNull;
import org.vadere.state.scenario.ScenarioElement;
import org.vadere.util.geometry.GeometryUtils;
import org.vadere.util.importSumo.fileParsers.SumoObject;
import org.vadere.gui.topographycreator.model.TopographyBuilder;
import org.vadere.state.attributes.scenario.AttributesObstacle;
import org.vadere.state.attributes.scenario.AttributesTopography;
import org.vadere.state.scenario.Obstacle;
import org.vadere.state.util.StateJsonConverter;
import org.vadere.util.geometry.shapes.VPolygon;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.*;

public class SumoToVadereConverter {
    private static final Logger logger = Logger.getLogger(SumoToVadereConverter.class);

    public SumoToVadereConverter() {
    }

    public String ConvertToVadereJson(List<SumoObject> allObstacles, List<ScenarioElement> additionalScenarioElements) throws JsonProcessingException {
        TopographyBuilder builder = new TopographyBuilder();
        addObstacles(builder, allObstacles);
        addAdditionalScenarioElements(additionalScenarioElements, builder);

        VRectangle bounds = builder.calculateBoundsFromElements();
        AttributesTopography attributes = builder.getAttributes();
        attributes.setBounds(bounds);

        return StateJsonConverter.serializeTopography(builder.build());
    }

    private static void addAdditionalScenarioElements(List<ScenarioElement> additionalScenarioElements, TopographyBuilder builder) {
        for (ScenarioElement additionalScenarioElement : additionalScenarioElements) {
            builder.addElement(additionalScenarioElement);
        }
    }

    private void addObstacles(TopographyBuilder builder, List<SumoObject> obstacles) {
        for (SumoObject sumoObject : obstacles) {
            int vadereId = sumoObject.getVadereId();
            VPolygon vPolygon = GeometryUtils.toVaderePolygon(sumoObject.getPolygon());
            Obstacle obstacle = toObstacle(vPolygon, vadereId);
            builder.addObstacle(obstacle);
        }
    }

    @NotNull
    private static Obstacle toObstacle(VPolygon vPolygon, int vadereId) {
        AttributesObstacle attributesObstacle = new AttributesObstacle();
        attributesObstacle.setShape(vPolygon);
        attributesObstacle.setId(vadereId);
        attributesObstacle.setVisible(true);
        Obstacle obstacle = new Obstacle(attributesObstacle);
        return obstacle;
    }
}
