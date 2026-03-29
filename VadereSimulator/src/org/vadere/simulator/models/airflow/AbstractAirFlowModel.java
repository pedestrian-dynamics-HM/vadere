package org.vadere.simulator.models.airflow;

import org.vadere.simulator.models.Model;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.airflow.AttributesAirFlowModel;
import org.vadere.state.attributes.models.airflow.AttributesInOutLet;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.AirFlow;
import org.vadere.simulator.context.VadereContext;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public abstract class AbstractAirFlowModel implements Model {

    protected AirFlow airFlow;

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        String scenarioPath = VadereContext.getCtx(domain.getTopography()).getString("scenarioPath");

        AttributesAirFlowModel attributesAirFlowModel = Model.findAttributes(attributesList, AttributesAirFlowModel.class);
        Rectangle2D.Double contentRect = domain.getTopography().getContentRect();
        double xmin = contentRect.getMinX();
        double ymin = contentRect.getMinY();
        double xmax = contentRect.getMaxX();
        double ymax = contentRect.getMaxY();
        if (attributesAirFlowModel != null) {
            xmin = Math.max(xmin, attributesAirFlowModel.getBounds().getXmin());
            ymin = Math.max(ymin, attributesAirFlowModel.getBounds().getYmin());
            xmax = Math.min(xmax, attributesAirFlowModel.getBounds().getXmax());
            ymax = Math.min(ymax, attributesAirFlowModel.getBounds().getYmax());
        }
        this.airFlow = new AirFlow(scenarioPath, "", xmin, ymin, xmax, ymax);
        domain.getTopography().setAirFlow(airFlow);
    }

    @Override
    public void preLoop(double simTimeInSec) {
        setupAirFlow();
    }

    @Override
    public void postLoop(double simTimeInSec) {
        // ignore
    }

    @Override
    public void update(double simTimeInSec) {
        // ignore
    }

    public abstract void setupAirFlow();

}
