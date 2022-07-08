package org.vadere.simulator.projects.dataprocessing.processor;

import org.apache.commons.lang3.tuple.Pair;
import org.vadere.annotation.factories.dataprocessors.DataProcessorClass;
import org.vadere.simulator.control.simulation.SimulationState;
import org.vadere.simulator.projects.dataprocessing.ProcessorManager;
import org.vadere.simulator.projects.dataprocessing.datakey.TimeGridKey;
import org.vadere.simulator.projects.dataprocessing.procesordata.PedestrianIdDuration;
import org.vadere.state.attributes.processor.AttributesAreaDensityGridCountingProcessor;
import org.vadere.state.attributes.processor.AttributesAreaDensityGridSpatialTimeProcessor;
import org.vadere.state.attributes.processor.AttributesProcessor;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VLine;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VRectangle;
import org.vadere.util.logging.Logger;

import java.util.*;

@DataProcessorClass(label = "AreaDensityGridSpatialTimeProcessor")
public class AreaDensityGridSpatialTimeProcessor extends DataProcessor<TimeGridKey, Double> {

    private double nextTime;

    private double timeWindowSize;

    private LinkedCellsGrid<PedestrianIdDuration> cellsElements;

    AttributesAreaDensityGridSpatialTimeProcessor attr;

    private static Logger logger = Logger.getLogger(AreaDensityGridSpatialTimeProcessor.class);

    public AreaDensityGridSpatialTimeProcessor() {
        super("gridDensity");
        setAttributes(new AttributesAreaDensityGridSpatialTimeProcessor());
    }

    @Override
    protected void doUpdate(SimulationState state) {
        if (state.getSimTimeInSec() <= nextTime + timeWindowSize) {
            cellsElements.clear();
            Collection<Pedestrian> peds = state.getTopography().getPedestrianDynamicElements().getElements();
            for (Pedestrian ped : peds) {
                LinkedList<FootStep> stepsInWindow = getFootstepsForTimeWindow(ped);
                computeTraversedCellsAndDurations(stepsInWindow, ped);
            }

            Map<int[], List<PedestrianIdDuration>> elementsByCell = cellsElements.getElementsByCell();
            for (Map.Entry<int[], List<PedestrianIdDuration>> result: elementsByCell.entrySet()) {
                double totalPedTimeInCell = result.getValue().stream()
                        .map(r -> r.getDuration())
                        .mapToDouble(d -> d)
                        .sum();
                VRectangle gridCell = cellsElements.getGridCellAsRectangle(result.getKey()[0], result.getKey()[1]);
                double cellDensity = totalPedTimeInCell / (gridCell.getArea() * timeWindowSize); // see duives 2015
                int time = (int) Math.round(nextTime + (timeWindowSize / 2));
                TimeGridKey key = new TimeGridKey(time,  gridCell.x, gridCell.y, attr.getCellSize());
                this.putValue(key, cellDensity);
            }
            nextTime += timeWindowSize;
        }
    }

    private void computeTraversedCellsAndDurations(LinkedList<FootStep> stepsInWindow, Pedestrian ped) {
        if (!stepsInWindow.isEmpty()) {
            // set start time of footstep to start of timewindow
            FootStep fs = stepsInWindow.get(0);
            double startTime = Math.max(fs.getStartTime(), nextTime);
            VPoint startPoint;
            if (startTime != fs.getStartTime()) {
                startPoint = footStepApproximatePointForTime(startTime, fs);
                fs = new FootStep(startPoint, fs.getEnd(), startTime, fs.getEndTime());
                stepsInWindow.set(0, fs);
            }

            int[] gridIdx = cellsElements.gridPos(fs.getStart());
            VRectangle currentCell = cellsElements.getGridCellAsRectangle(gridIdx[0], gridIdx[1]);
            double timeInCell = 0.0;
            for (int i = 1; i < stepsInWindow.size(); i++) {
                if (!currentCell.contains(fs.getStart())) {
                    logger.warn("next footstep did not start at end position of previous footstep.");
                    break;
                }
                Pair<VRectangle, Double> pair = computeCellForFootStep(fs, currentCell, timeInCell, gridIdx, ped);
                currentCell = pair.getLeft();
                timeInCell = pair.getRight();
                fs = stepsInWindow.get(i);
            }

//            if (stepsInWindow.size() > 1) {
//                // end position of start step is in window
//                int[] gridIdx = cellsElements.gridPos(fs.getEnd());
//                VRectangle currentCell = cellsElements.getGridCellAsRectangle(gridIdx[0], gridIdx[1]);
//                timeInCell = fs.getEndTime() - Math.max(fs.computeIntersectionTime(currentCell), nextTime);
//                // following footsteps
//                for (int i = 1; i < stepsInWindow.size(); i++) {
//                    fs = stepsInWindow.get(i);
//                    if (!currentCell.contains(fs.getStart())) {
//                        logger.warn("next footstep did not start at end position of previous footstep.");
//                        break;
//                    }
//                    if (currentCell.contains(fs.getEnd())) {
//                        //still same cell
//                        timeInCell += fs.duration();
//                    } else {
//                        //add rest time in cell
//                        timeInCell += fs.computeIntersectionTime(currentCell) - fs.getStartTime();
//                        //add pedestrian with total duration for cell
//                        cellsElements.addObject(new PedestrianIdDuration(ped.getId(), timeInCell, currentCell));
//                        // check for skipped cells during one footstep
//                        List<VRectangle> skipped = computeSkippedCells(gridIdx, cellsElements.gridPos(fs.getEnd()), currentCell);
//                        gridIdx = cellsElements.gridPos(fs.getEnd());
//                        currentCell = cellsElements.getGridCellAsRectangle(gridIdx[0], gridIdx[1]);
//                        if (!skipped.isEmpty()) {
//                            double skippedDuration = fs.computeIntersectionTime(currentCell) - fs.getStartTime();
//                            for (VRectangle intermediateCell : skipped) {
//                                //add every cell in between with a shared amount of time
//                                cellsElements.addObject(new PedestrianIdDuration(ped.getId(),
//                                        skippedDuration / skipped.size(), intermediateCell));
//                            }
//                        }
//                        //new cell duration of footstep entering
//                        timeInCell = fs.getEndTime() - fs.computeIntersectionTime(currentCell);
//                    }
//                }
//            }
        }
    }

    private Pair<VRectangle, Double> computeCellForFootStep(FootStep fs, VRectangle cell, double timeInCell,
                                                 int[] gridIdx, Pedestrian ped) {
        if (cell.contains(fs.getEnd())) {
            // still same cell
            timeInCell += Math.min(fs.getEndTime(), nextTime + timeWindowSize) - fs.getStartTime();
        } else {

            double exitingTime = fs.computeIntersectionTime(cell);
            //add rest time in cell
            timeInCell += Math.min(exitingTime, nextTime + timeWindowSize) - fs.getStartTime();
            //add pedestrian with total duration for cell
            cellsElements.addObject(new PedestrianIdDuration(ped.getId(), timeInCell, cell));

            if (exitingTime < nextTime + timeWindowSize) {
                // check for skipped cells during one footstep
                List<VRectangle> skipped = computeSkippedCells(gridIdx, cellsElements.gridPos(fs.getEnd()), cell);
                gridIdx = cellsElements.gridPos(fs.getEnd());
                cell = cellsElements.getGridCellAsRectangle(gridIdx[0], gridIdx[1]); //next cell for ped
                double newCellEnteringTime = fs.computeIntersectionTime(cell);

                if (!skipped.isEmpty()) {
                    double skippedDuration = Math.min(newCellEnteringTime, nextTime + timeWindowSize)
                            - exitingTime;
                    for (VRectangle intermediateCell : skipped) {
                        //add every cell in between with a shared amount of time
                        cellsElements.addObject(new PedestrianIdDuration(ped.getId(),
                                skippedDuration / skipped.size(), intermediateCell));
                    }
                }

                //new cell duration of footstep entering
                if (newCellEnteringTime < nextTime + timeWindowSize) {
                    timeInCell = Math.min(fs.getEndTime(), nextTime + timeWindowSize) - newCellEnteringTime;
                } else {
                    timeInCell = 0.0;
                }
            }
        }
        return Pair.of(cell, timeInCell);
    }

    private VPoint footStepApproximatePointForTime(double time, FootStep footStep) {
        assert time < footStep.getEndTime();
        double ratio = (time - footStep.getStartTime()) / footStep.duration();

        VLine line = new VLine(footStep.getStart(), footStep.getEnd());
        return line.midPoint(-0.5 + ratio);
    }

    private LinkedList<FootStep> getFootstepsForTimeWindow(Pedestrian pedestrian) {
        Iterator<FootStep> it = pedestrian.getTrajectory().getFootSteps()
                .descendingIterator();
        LinkedList<FootStep> stepsInWindow = new LinkedList<>();
        while (it.hasNext()) {
            FootStep footStep = it.next();
            stepsInWindow.addFirst(footStep);
            if (footStep.getStartTime() <= nextTime) {
                if (footStep.getEndTime() <= nextTime) {
                    // latest step before time Window
                    stepsInWindow.removeFirst();
                }
                break;
            }
            if (nextTime + timeWindowSize <= footStep.getEndTime()) {
                if (stepsInWindow.size() > 1) {
                    stepsInWindow.removeLast();
                }
            }
        }
        return stepsInWindow;
    }

    private List<VRectangle> computeSkippedCells(int[] currentIdx, int[] newIdx, VRectangle currentCell) {
        int iXDiff = currentIdx[0] - newIdx[0];
        int iYDiff = currentIdx[1] - newIdx[1];
        List<VRectangle> skippedCellsCenters = new ArrayList<>();
        double cellWidthX = currentCell.getWidth() * Integer.signum(iXDiff);
        double cellWidthY = currentCell.getHeight() * Integer.signum(iYDiff);
        for (int i = 1; i < Math.abs(iXDiff); i++) {
            for (int j = 1; j < Math.abs(iYDiff); j++) {
                double intermediateX = currentCell.x + i * cellWidthX;
                double intermediateY = currentCell.y + j * cellWidthY;
                skippedCellsCenters.add(new VRectangle(intermediateX, intermediateY, currentCell.width, currentCell.height));
            }
        }
        return skippedCellsCenters;
    }

    @Override
    public void preLoop(SimulationState state) {
        super.preLoop(state);
        cellsElements = new LinkedCellsGrid<>(
                state.getTopography().getBounds().x,
                state.getTopography().getBounds().y,
                state.getTopography().getBounds().width,
                state.getTopography().getBounds().height,
                attr.getCellSize());
    }


    @Override
    public void init(ProcessorManager manager) {
        super.init(manager);
        this.nextTime = 0.0;
        attr = (AttributesAreaDensityGridSpatialTimeProcessor) getAttributes();
        this.timeWindowSize = attr.getT();
    }

    @Override
    public AttributesProcessor getAttributes() {
        if (super.getAttributes() == null) {
            setAttributes(new AttributesAreaDensityGridSpatialTimeProcessor());
        }
        return super.getAttributes();
    }
}
