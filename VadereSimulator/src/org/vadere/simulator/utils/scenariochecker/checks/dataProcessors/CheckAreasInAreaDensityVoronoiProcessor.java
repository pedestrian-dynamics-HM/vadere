package org.vadere.simulator.utils.scenariochecker.checks.dataProcessors;

import org.vadere.simulator.projects.Scenario;
import org.vadere.simulator.projects.dataprocessing.flags.UsesMeasurementArea;
import org.vadere.simulator.projects.dataprocessing.processor.AreaDensityVoronoiProcessor;
import org.vadere.simulator.projects.dataprocessing.processor.DataProcessor;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerMessage;
import org.vadere.simulator.utils.scenariochecker.ScenarioCheckerReason;
import org.vadere.simulator.utils.scenariochecker.checks.AbstractScenarioCheck;
import org.vadere.state.attributes.processor.AttributesAreaDensityVoronoiProcessor;
import org.vadere.state.scenario.MeasurementArea;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

public class CheckAreasInAreaDensityVoronoiProcessor extends AbstractScenarioCheck {

    @Override
    public PriorityQueue<ScenarioCheckerMessage> runScenarioCheckerTest(Scenario scenario) {
        PriorityQueue<ScenarioCheckerMessage> ret = new PriorityQueue<>();
        List<DataProcessor<?, ?>> processors
                = scenario.getDataProcessingJsonManager().getDataProcessors();

        List <DataProcessor> areaVoronoiProc = processors.stream().filter(dataProcessor -> dataProcessor.getClass().equals(AreaDensityVoronoiProcessor.class)).collect(Collectors.toList());
        if(!areaVoronoiProc.isEmpty()){

            // get all measurement areas
            List<MeasurementArea> measurementAreas =  scenario.getTopography().getMeasurementAreas();

            for(int i = 0; i < areaVoronoiProc.size(); i++) {
                AreaDensityVoronoiProcessor voronoiProcessor = (AreaDensityVoronoiProcessor) areaVoronoiProc.get(i);
                AttributesAreaDensityVoronoiProcessor voronoiAtt = (AttributesAreaDensityVoronoiProcessor) voronoiProcessor.getAttributes();
                int voronoiMeasId = voronoiAtt.getVoronoiMeasurementAreaId();
                int voronoiDiagramId = voronoiAtt.getMeasurementAreaId();

                MeasurementArea voronoiMeasArea = scenario.getTopography().getMeasurementArea(voronoiMeasId);
                MeasurementArea voronoiDiagramArea = scenario.getTopography().getMeasurementArea(voronoiDiagramId);
                if (!voronoiMeasArea.isRectangular() || !voronoiDiagramArea.isRectangular()){
                    ret.add(msgBuilder.dataProcessorAttrError()
                            .reason(ScenarioCheckerReason.MEASUREMENT_AREA_NOT_RECTANGULAR,
                                   String.format(" [Processor id: %d]", voronoiProcessor.getId())).build());
                }

                if(!voronoiDiagramArea.getShape().contains((Rectangle2D)voronoiMeasArea.getShape())){
                    ret.add(msgBuilder.dataProcessorAttrError()
                            .reason(ScenarioCheckerReason.AREAS_DENSITY_VORONOI_PROCESSOR_MISMATCH,
                                    String.format(" [Processor id: %d]", voronoiProcessor.getId())).build());

                }

            }
        }

        return ret;
    }
}
