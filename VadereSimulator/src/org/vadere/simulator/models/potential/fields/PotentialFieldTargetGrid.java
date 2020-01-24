package org.vadere.simulator.models.potential.fields;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.models.potential.solver.calculators.EikonalSolver;
import org.vadere.simulator.models.potential.solver.calculators.cartesian.AGridEikonalSolver;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.models.AttributesFloorField;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.Agent;
import org.vadere.state.scenario.Topography;
import org.vadere.util.data.cellgrid.CellGrid;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.Vector2D;
import org.vadere.util.math.InterpolationUtil;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PotentialFieldTargetGrid extends PotentialFieldTarget implements IPotentialFieldTargetGrid {

    public PotentialFieldTargetGrid(@NotNull final Domain domain,
                                    @NotNull final AttributesAgent attributesPedestrian,
                                    @NotNull final AttributesFloorField attributesPotential) {
        super(domain, attributesPedestrian, attributesPotential);
    }

    @Override
    public Map<Integer, CellGrid> getCellGrids() {
        Map<Integer, CellGrid> map = new HashMap<>();

        for (Map.Entry<Integer, EikonalSolver> entry : eikonalSolvers.entrySet()) {
            Integer targetId = entry.getKey();
            EikonalSolver eikonalSolver = entry.getValue();

            if(eikonalSolver instanceof AGridEikonalSolver){
                map.put(targetId, ((AGridEikonalSolver)eikonalSolver).getCellGrid());
            }
        }

        return map;
    }

    @Override
    public Vector2D getTargetPotentialGradient(VPoint pos, Agent ped) {
        double[] gradient = { 0.0, 0.0 };

        if (ped.hasNextTarget()) {
            InterpolationUtil.getGradientMollified(getCellGrids().get(ped.getNextTargetId()), new double[]{pos.getX(), pos.getY()}, gradient, 0.1);
        }

        return new Vector2D(gradient[0], gradient[1]);
    }
}
