package org.vadere.simulator.control.psychology.cognition.models;

import org.vadere.state.attributes.models.psychology.cognition.AttributesChangeTargetScriptedCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.ChangeTargetScripted;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * The {@link ChangeTargetScriptedCognitionModel} changes the target id of agents. The stimulus
 * {@link org.vadere.state.psychology.perception.types.ChangeTargetScripted} describes at which
 * simulation time agents should change their target.
 *
 * The target id is changed directly here at cognition layer and on locomotion layer an agent just performs a step.
 */
public class ChangeTargetScriptedCognitionModel implements ICognitionModel {

    private Topography topography;
    private AttributesChangeTargetScriptedCognitionModel attributes;

    @Override
    public void initialize(Topography topography, Random random) {
        this.topography = topography;
        this.attributes = new AttributesChangeTargetScriptedCognitionModel();
    }

    @Override
    public void update(Collection<Pedestrian> pedestrians) {
        // Watch out: "allMatch()" returns true on empty list!
        if (pedestrians.stream().allMatch(pedestrian -> pedestrian.getMostImportantStimulus() instanceof ChangeTargetScripted)) {

            if (pedestrians.isEmpty() == false) {
                ChangeTargetScripted changeTargetScripted = (ChangeTargetScripted) pedestrians.iterator().next().getMostImportantStimulus();
                changeTargetsAccordingToStimulus(changeTargetScripted, pedestrians);
            }

        }

        pedestrians.stream().forEach(pedestrian -> pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED));
    }

    @Override
    public void setAttributes(AttributesCognitionModel attributes) {
        this.attributes = (AttributesChangeTargetScriptedCognitionModel) attributes;

    }

    @Override
    public AttributesChangeTargetScriptedCognitionModel getAttributes() {
        return attributes;
    }

    private void changeTargetsAccordingToStimulus(ChangeTargetScripted changeTargetScripted, Collection<Pedestrian> pedestrians) {
        int activeIndex = getActiveIndexFromStimulusList(changeTargetScripted);

        if (activeIndex >= 0) {
            Integer nextTarget = changeTargetScripted.getNewTargetIds().get(activeIndex);
            Integer totalAgentsToChange = changeTargetScripted.getTotalAgentsToChangeTarget().get(activeIndex);
            LinkedList<Integer> originalTargetIds = changeTargetScripted.getOriginalTargetIds();
            boolean changeRemainingPedestrians = changeTargetScripted.getChangeRemainingPedestrians();

            changeTargetsOfPedestrians(pedestrians, nextTarget, totalAgentsToChange, originalTargetIds, changeRemainingPedestrians);
        }
    }

    private int getActiveIndexFromStimulusList(ChangeTargetScripted changeTargetScripted) {
        LinkedList<Double> simTimesToChangeTarget = changeTargetScripted.getSimTimesToChangeTarget();
        double allowedTimeDelta = changeTargetScripted.getAllowedTimeDelta();
        double currentSimTimeInSec = changeTargetScripted.getTime();

        int activeIndex = -1;

        for (int i = 0; i < simTimesToChangeTarget.size(); i++) {
            Double currentSimTimeFromList = simTimesToChangeTarget.get(i);
            double timeDelta = currentSimTimeInSec - currentSimTimeFromList;

            // Use "timeDelta >= 0" to ensure that target change does not happen before the definition from list.
            if (timeDelta >= 0 && timeDelta <= allowedTimeDelta) {
                activeIndex = i;
                break;
            }
        }

        return activeIndex;
    }

    private void changeTargetsOfPedestrians(Collection<Pedestrian> pedestrians, int nextTarget, int totalAgentsToChange, LinkedList<Integer> originalTargets, boolean changeRemainingPedestrians) {
        List<Pedestrian> originalPedsToChange = pedestrians.stream()
                .filter(pedestrian -> originalTargets.contains(pedestrian.getNextTargetId()) == true)
                .collect(Collectors.toList());
        List<Pedestrian> remainingPedsToChange = pedestrians.stream()
                .filter(pedestrian -> originalTargets.contains(pedestrian.getNextTargetId()) == false)
                .collect(Collectors.toList());

        boolean nextTargetIsAgent = false;

        List<Pedestrian> originalPedsToChangeSubList = (totalAgentsToChange <= originalPedsToChange.size()) ? originalPedsToChange.subList(0, totalAgentsToChange) : originalPedsToChange;

        originalPedsToChangeSubList.stream().forEach(pedestrian -> pedestrian.setSingleTarget(nextTarget, nextTargetIsAgent));

        if (changeRemainingPedestrians) {
            remainingPedsToChange.stream().forEach(pedestrian -> pedestrian.setSingleTarget(nextTarget, nextTargetIsAgent));
        }
    }
}
