package org.vadere.simulator.models.dsm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SubModelBuilder;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.models.potential.fields.PotentialFieldAgent;
import org.vadere.simulator.models.potential.fields.PotentialFieldObstacle;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.state.attributes.models.AttributesDSM;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElement;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.simulation.FootStep;
import org.vadere.util.geometry.shapes.VPoint;
import org.vadere.util.geometry.shapes.VShape;
import org.vadere.util.logging.Logger;
import org.vadere.util.reflection.DynamicClassInstantiator;

import java.io.File;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

import static org.vadere.state.util.StateJsonConverter.getLocomotionHash;

/**
 * @author Kevin Becker
 */
@ModelClass(isMainModel = true)
public class DatabasedStepsModel implements MainModel {

    private final static Logger logger = Logger.getLogger(DatabasedStepsModel.class);
    private AttributesAgent attributesPedestrian;
    private Random random;
    private Domain domain;
    private final List<Model> models = new LinkedList<>();
    private TrajectoryBuffer trajectoryBuffer;
    private boolean canExtractStepsFromFile;
    private MainModel fallbackMainModel;


    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.attributesPedestrian = attributesPedestrian;

        AttributesDSM attributesDSM = Model.findAttributes(attributesList, AttributesDSM.class);

        if (attributesDSM.getTrajectoryFile() != null
                && attributesDSM.getTrajectoryFile().endsWith(".traj")) {
            this.canExtractStepsFromFile = true;
        } else {
            File dir = new File(attributesDSM.getTrajectoryFile());
            if (attributesDSM.getTrajectoryFile() != null && dir.isDirectory()) {
                String locomotionHash = "123";//getLocomotionHash(domain.getTopography(),);
                String trajFileName = "postvis" + locomotionHash + ".traj";
                File trajFile = new File(dir, trajFileName);
                if (trajFile.exists()) {
                    attributesDSM.setTrajectoryFile(trajFile.getAbsolutePath());
                    this.canExtractStepsFromFile = true;
                } else {
                    this.canExtractStepsFromFile = false;
                }
            } else {
                logger.error("Variable trajectoryFile must be a .traj file or directory");
                throw new IllegalArgumentException("Invalid argument for trajectoryFile");
            }
        }

        if (this.canExtractStepsFromFile) {
            final SubModelBuilder subModelBuilder = new SubModelBuilder(attributesList, domain, attributesPedestrian, random);
            subModelBuilder.buildSubModels(attributesDSM.getSubmodels());
            subModelBuilder.addBuildedSubModelsToList(models);
            models.add(this);

            this.trajectoryBuffer = new TrajectoryBuffer(attributesDSM.getTrajectoryFile(), attributesDSM.getBufferedLines());
        } else {
            List<Attributes> attributesListWithoutDSM = attributesList.stream()
                    .filter(attr -> !(attr instanceof AttributesDSM))
                    .collect(Collectors.toList());
            if (attributesDSM != null) {
                attributesListWithoutDSM.addAll(attributesDSM.getAttributesFallbackModel());
            }
            // attributesList = attributesDSM.getAttributesFallbackModel() + (attributesList - attributesDSM)

            boolean hasDuplicateSubtypes = attributesListWithoutDSM.stream()
                    .collect(Collectors.groupingBy(Object::getClass, Collectors.counting()))
                    .values().stream()
                    .anyMatch(count -> count > 1);
            if (hasDuplicateSubtypes) {
                logger.error("There are duplicate Attributes types in the list of Attributes.");
                throw new IllegalArgumentException("There are duplicate Attributes types in the list of Attributes.");
            }

            DynamicClassInstantiator<MainModel> instantiator = new DynamicClassInstantiator<>();
            this.fallbackMainModel = instantiator.createObject(attributesDSM.getFallbackMainModel());
            this.fallbackMainModel.initialize(attributesListWithoutDSM, domain, attributesPedestrian, random);

            if (!attributesDSM.getSubmodels().isEmpty()) {
                logger.warn("The submodels List in AttributesDSM is not empty. However, only the submodels list of the FallbackMainModel is relevant.");
            }
        }

    }

    @Override
    public void preLoop(double simTimeInSec) {
        if (this.canExtractStepsFromFile) {
            trajectoryBuffer.read();
        }
        else {
            this.fallbackMainModel.preLoop(simTimeInSec);
        }
    }

    @Override
    public void postLoop(double simTimeInSec) {
        if (this.canExtractStepsFromFile) {
            trajectoryBuffer.closeReader();
        }
        else {
            this.fallbackMainModel.postLoop(simTimeInSec);
        }
    }

    @Override
    public void update(double simTimeInSec) {
        if (this.canExtractStepsFromFile) {
            clearFootSteps();

            DSMStep nextStep;
            while ((nextStep = trajectoryBuffer.getNextStep()) != null && nextStep.getStartTime() < simTimeInSec) {

                Pedestrian pedestrian = domain.getTopography().getPedestrianDynamicElements().getElement(nextStep.getPedestrianId());

                VPoint startPosition = pedestrian.getPosition();
                VPoint endPosition = nextStep.getEndPosition();
                pedestrian.setPosition(endPosition);

                LinkedList<Integer> nextTarget = new LinkedList<>();
                nextTarget.add(nextStep.getTargetId());
                pedestrian.setTargets(nextTarget);

                FootStep currentFootstep = new FootStep(startPosition, endPosition, nextStep.getStartTime(), nextStep.getEndTime());
                pedestrian.getTrajectory().add(currentFootstep);

                if (!startPosition.equals(nextStep.getStartPosition())) {
                    logger.warn("Agent spawn positions did not match with the input trajectories");
                }

                synchronized (domain.getTopography()) {
                    domain.getTopography().moveElement(pedestrian, startPosition);
                }

                trajectoryBuffer.removeStep();
            }
            trajectoryBuffer.read();
        }
        else {
            this.fallbackMainModel.update(simTimeInSec);
        }
    }

    private void clearFootSteps() {
        domain.getTopography().getPedestrianDynamicElements().getElements().forEach(Pedestrian::clearFootSteps);
    }

    @Override
    public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Class<T> type) {
        if (this.canExtractStepsFromFile) {
            return createElement(position, id, this.attributesPedestrian, type);
        }
        else {
            return this.fallbackMainModel.createElement(position, id, type);
        }
    }

    @Override
    public <T extends DynamicElement> DynamicElement createElement(VPoint position, int id, Attributes attr, Class<T> type) {
        if (this.canExtractStepsFromFile) {
            AttributesAgent aAttr = (AttributesAgent) attr;

            if (!Pedestrian.class.isAssignableFrom(type))
                throw new IllegalArgumentException("DBS cannot initialize " + type.getCanonicalName());

            AttributesAgent pedAttributes = new AttributesAgent(aAttr, registerDynamicElementId(domain.getTopography(), id));

            return createElement(position, pedAttributes);
        }
        else {
            return this.fallbackMainModel.createElement(position, id, attr, type);
        }
    }

    @Override
    public VShape getDynamicElementRequiredPlace(@NotNull VPoint position) {
        if (this.canExtractStepsFromFile) {
            return createElement(position, new AttributesAgent(attributesPedestrian, -1)).getShape();
        }
        else {
            return this.fallbackMainModel.getDynamicElementRequiredPlace(position);
        }
    }

    private Pedestrian createElement(VPoint position, @NotNull final AttributesAgent attributesAgent) {
        Pedestrian pedestrian = new Pedestrian(attributesAgent, random);
        pedestrian.setPosition(position);
        return pedestrian;
    }

    @Override
    public List<Model> getSubmodels() {
        if (this.canExtractStepsFromFile) {
            return models;
        }
        else {
            return fallbackMainModel.getSubmodels();
        }
    }

    @Override
    public SourceControllerFactory getSourceControllerFactory() {
        if (this.canExtractStepsFromFile) {
            return MainModel.super.getSourceControllerFactory();
        }
        else {
            return fallbackMainModel.getSourceControllerFactory();
        }
    }

    public IPotentialFieldTarget getPotentialFieldTarget() {
        try {
            Method method = fallbackMainModel.getClass().getMethod("getPotentialFieldTarget");
            return (IPotentialFieldTarget) method.invoke(fallbackMainModel);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get PotentialFieldTarget", e);
        }
    }

    public PotentialFieldObstacle getPotentialFieldObstacle() {
        try {
            Method method = fallbackMainModel.getClass().getMethod("getPotentialFieldObstacle");
            return (PotentialFieldObstacle) method.invoke(fallbackMainModel);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get getPotentialFieldObstacle", e);
        }
    }

    public PotentialFieldAgent getPotentialFieldAgent() {
        try {
            Method method = fallbackMainModel.getClass().getMethod("getPotentialFieldAgent");
            return (PotentialFieldAgent) method.invoke(fallbackMainModel);
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to get getPotentialFieldAgent", e);
        }
    }


}
