package org.vadere.simulator.control.psychology.cognition.models;

import org.apache.commons.math3.distribution.EnumeratedIntegerDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.util.Precision;
import org.vadere.state.attributes.models.psychology.cognition.AttributesCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesProbabilisticCognitionModel;
import org.vadere.state.attributes.models.psychology.cognition.AttributesRouteChoiceDefinition;
import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.information.InformationState;
import org.vadere.state.psychology.perception.types.*;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.state.simulation.FootstepHistory;
import org.vadere.util.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class ProbabilisticCognitionModel extends AProbabilisticModel {

    private static Logger logger = Logger.getLogger(ProbabilisticCognitionModel.class);

    private RandomGenerator rng;

    public AttributesProbabilisticCognitionModel getAttributes() {
        return attributesProbabilisticCognitionModel;
    }

    public void setAttributes(AttributesCognitionModel attributesProbabilisticCognitionModel) {
        this.attributesProbabilisticCognitionModel = (AttributesProbabilisticCognitionModel) attributesProbabilisticCognitionModel;
    }

    private AttributesProbabilisticCognitionModel attributesProbabilisticCognitionModel;
    private Topography topography;

    @Override
    public void initialize(Topography topography, Random random) {
        this.rng = new JDKRandomGenerator(random.nextInt());
        this.attributesProbabilisticCognitionModel = new AttributesProbabilisticCognitionModel();
        this.topography = topography;

        checkModelAttributes();

    }


    @Override
    public void update(Collection<Pedestrian> pedestrians) {

        for (Pedestrian pedestrian : pedestrians) {

            Stimulus stimulus = pedestrian.getMostImportantStimulus();

            if (stimulus instanceof InformationStimulus) {

                InformationStimulus information = (InformationStimulus) pedestrian.getMostImportantStimulus();
                String instruction = information.getInformation();

                if (pedestrian.getKnowledgeBase().getKnowledge().size() == 0) {

                    AttributesRouteChoiceDefinition attr = getFilteredAttributes(instruction);
                    LinkedList<Integer> newTarget = getNewTarget(attr.getTargetIds(), attr.getTargetProbabilities());

                    setNewTarget(pedestrian, instruction, newTarget, false);

                    for (Pedestrian member : pedestrian.getPedGroupMembers()){
                        setNewTarget(member, instruction, newTarget, true);
                    }

                } else {
                    tryToReachTarget(pedestrian);
                }

            } else {
                tryToReachTarget(pedestrian);
            }

        }
    }

    private void setNewTarget(Pedestrian pedestrian, String instruction, LinkedList<Integer> newTarget, boolean isGroupMember) {
        //System.out.println("Set target " + newTarget + "\t for pedestrian \t" + pedestrian.getId() + "\t with group id = " + pedestrian.getGroupIds().getFirst() + "\t set as group-member: " + isGroupMember);

        pedestrian.setTargets(newTarget);
        pedestrian.setNextTargetListIndex(0);

        pedestrian.getKnowledgeBase().addInformation(new KnowledgeItem(instruction));
        pedestrian.setSelfCategory(SelfCategory.CHANGE_TARGET);
    }

    private void tryToReachTarget(Pedestrian pedestrian) {
        if (pedestrianCannotMove(pedestrian)) {
            pedestrian.setSelfCategory(SelfCategory.COOPERATIVE);
        } else {
            pedestrian.setSelfCategory(SelfCategory.TARGET_ORIENTED);
        }
    }

    public AttributesRouteChoiceDefinition getFilteredAttributes(String information) {

        List<AttributesRouteChoiceDefinition> routeChoices = attributesProbabilisticCognitionModel.getRouteChoices();
        List<AttributesRouteChoiceDefinition> filteredAttributes = routeChoices
                .stream()
                .filter(routeChoice -> routeChoice.getInstruction().equals(information))
                .collect(Collectors.toList());

        if (filteredAttributes.size() == 0) {
            throw new RuntimeException("Instruction >" + information + "< not found in " + AttributesProbabilisticCognitionModel.class.getSimpleName() + " definition.");
        } else if (filteredAttributes.size() > 1) {
            throw new RuntimeException("Multiple attribute definitions found for instruction >" + information + "<. Instruction must be unique.");
        }

        return filteredAttributes.get(0);

    }

    public LinkedList<Integer> getNewTarget(LinkedList<Integer> targetIdList, LinkedList<Double> targetProbabilityList) {

        int[] targetIds = targetIdList.stream().mapToInt(i -> i).toArray();
        double[] targetProbabilities = targetProbabilityList.stream().mapToDouble(i -> i).toArray();

        EnumeratedIntegerDistribution dist = new EnumeratedIntegerDistribution(rng, targetIds, targetProbabilities);
        Integer newTargetId = dist.sample();

        LinkedList<Integer> newTarget = new LinkedList<>();
        newTarget.add(newTargetId);

        return newTarget;
    }


    // checks
    private void checkModelAttributes() {
        List<AttributesRouteChoiceDefinition> routeChoices = attributesProbabilisticCognitionModel.getRouteChoices();
        for (AttributesRouteChoiceDefinition r : routeChoices) {
            checkIfTargetIdsValid(r.getTargetIds());
            checkIfProbabilitiesValid(r.getTargetProbabilities());
        }
    }

    private void checkIfProbabilitiesValid(final List<Double> targetProbabilities) {

        double sum = targetProbabilities.stream().reduce(0.0, Double::sum);
        if (!Precision.equals(sum, 1.0)) {
            throw new IllegalArgumentException("Sum of probabilities must be 1.0." + sum + ". Got: " + targetProbabilities + ".");
        }
    }

    private void checkIfTargetIdsValid(LinkedList<Integer> targetIds) {

        List<Integer> targetIdsTopography = topography.getTargetIds();

        if (!targetIdsTopography.containsAll(targetIds)) {
            throw new RuntimeException("Targets defined in topography and targets defined in " + AttributesProbabilisticCognitionModel.class.getSimpleName() + " mismatch.");

        }
    }

    protected boolean pedestrianCannotMove(Pedestrian pedestrian) {
        boolean cannotMove = false;

        FootstepHistory footstepHistory = pedestrian.getFootstepHistory();
        int requiredFootSteps = 2;

        if (footstepHistory.size() >= requiredFootSteps
                && footstepHistory.getAverageSpeedInMeterPerSecond() <= 0.05) {
            cannotMove = true;
        }

        return cannotMove;
    }



}
