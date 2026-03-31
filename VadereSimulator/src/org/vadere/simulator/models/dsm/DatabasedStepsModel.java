package org.vadere.simulator.models.dsm;

import org.jetbrains.annotations.NotNull;
import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.context.VadereContext;
import org.vadere.simulator.control.factory.SourceControllerFactory;
import org.vadere.simulator.models.MainModel;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.SubModelBuilder;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

import static org.vadere.state.util.StateJsonConverter.getLocomotionHash;

/**
 *  The DSM either reads steps from a given .traj file, finds a postvis_[hash].traj file with a corresponding hash
 *  or uses the fallbackMainModel to create a new postvis_[hash].traj file.
 *  The advantage of this model is that, for multiple simulation runs, we do not have to (expensively) recompute the
 *  floor field for each run but just read exisiting .traj file, if the locomotion of agents did not change
 *  (but this only works for parameters that don't affect the locomotion like for the AirTransmissionModel)
 *  The model with hash function usage cannot (yet) be used together with the psychology layer, if
 *  a feature of the psychology layer affects the movement pattern.
 * @author Kevin Becker, Sophia Wagner
 */
@ModelClass(isMainModel = true)
public class DatabasedStepsModel implements MainModel {

    private final static Logger logger = Logger.getLogger(DatabasedStepsModel.class);
    private AttributesAgent attributesPedestrian;
    private Random random;
    private Domain domain;
    private AttributesDSM attributesDSM;
    private final List<Model> models = new LinkedList<>();
    private TrajectoryBuffer trajectoryBuffer;
    private boolean canExtractStepsFromFile;
    private MainModel fallbackMainModel;
    public static String outputPath = "outputPath";
    public static String scenarioPath;
    public static String simulationSeedName = "simulationSeed";
    protected long simulationSeed;
    private String locomotionHash = "";
    private String contextId;



    @Override
    public void initialize(List<Attributes> attributesList, Domain domain, AttributesAgent attributesPedestrian, Random random) {
        this.domain = domain;
        this.random = random;
        this.attributesPedestrian = attributesPedestrian;
        this.outputPath = VadereContext.getCtx(domain.getTopography()).getString(outputPath);
        this.scenarioPath = VadereContext.getCtx(domain.getTopography()).getString("scenarioPath");
        this.simulationSeed = VadereContext.getCtx(domain.getTopography()).getLong(simulationSeedName);
        attributesDSM = Model.findAttributes(attributesList, AttributesDSM.class);
        this.locomotionHash = getLocomotionHash(this.domain.getTopography(), this.simulationSeed, this.attributesDSM.getAttributesFallbackModel());

        this.canExtractStepsFromFile = checkIfCanExtractStepsFromFile();
        if (this.canExtractStepsFromFile) {
            final SubModelBuilder subModelBuilder = new SubModelBuilder(attributesList, domain, attributesPedestrian, random);
            subModelBuilder.buildSubModels(attributesDSM.getSubmodels());
            subModelBuilder.addBuildedSubModelsToList(models);
            models.add(this);
            this.trajectoryBuffer = new TrajectoryBuffer(attributesDSM.getTrajectoryFileOrFolder(), attributesDSM.getBufferedLines());
        } else {
            initializeFallbackMainModel(attributesList);
        }
    }

    protected boolean checkIfCanExtractStepsFromFile() {
        if (attributesDSM.getTrajectoryFileOrFolder() == null) {
            setDefaultTrajectoryPath();
        }
        if (attributesDSM.getTrajectoryFileOrFolder().endsWith(".traj")) {
            return true;
        }
        File dir = new File(attributesDSM.getTrajectoryFileOrFolder());
        if (!dir.isDirectory()) {
            logger.error("Variable trajectoryFileOrFolder must be a .traj file or directory");
            throw new IllegalArgumentException("Invalid argument for trajectoryFile");
        }
        String trajFileName = getContextId() + "_" + locomotionHash + ".traj";
        File trajFile = new File(dir, trajFileName);
        if (trajFile.exists()) {
            attributesDSM.setTrajectoryFileOrFolder(trajFile.getAbsolutePath());
            return true;
        } else {
            return false;
        }
    }

    private void setDefaultTrajectoryPath() {
        logger.info("Variable trajectoryFileOrFolder is null. Using default cache folder.");
        File scenarioFile = new File(this.scenarioPath);
        File cacheDir = new File(scenarioFile.getParent(), "cache");
        if (!cacheDir.exists()) {
            boolean success = cacheDir.mkdirs();
            if (!success) {
                logger.warn("Could not create default cache directory: " + cacheDir.getAbsolutePath());
            }
        }
        attributesDSM.setTrajectoryFileOrFolder(cacheDir.getAbsolutePath());
    }

    private void initializeFallbackMainModel(List<Attributes> attributesList) {
        List<Attributes> attributesListWithoutDSM = attributesList.stream()
                .filter(attr -> !(attr instanceof AttributesDSM))
                .collect(Collectors.toList());
        if (attributesDSM != null) {
            attributesListWithoutDSM.addAll(attributesDSM.getAttributesFallbackModel());
        }

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
            logger.warn("The submodels list in AttributesDSM is not empty but will be ignored. Only the submodels list of the FallbackMainModel is relevant.");
        }
    }


    @Override
    public void preLoop(double simTimeInSec) {
        trajectoryBuffer.read();
    }

    @Override
    public void postLoop(double simTimeInSec) {
        trajectoryBuffer.closeReader();
    }

    @Override
    public void update(double simTimeInSec) {
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
            pedestrian.setNextTargetListIndex(0);

            FootStep currentFootstep = new FootStep(startPosition, endPosition, nextStep.getStartTime(), nextStep.getEndTime());
            pedestrian.getTrajectoryOfSimulationStep().add(currentFootstep);

            if (!startPosition.equals(nextStep.getStartPosition())) {
                logger.warn("Agent spawn positions did not match with the input trajectories");
                // this happens since the random seed is different in the DatabasedStepsModel than in the other
                // locomotion model although the simulation seed is the same. This leads to little jumps after an agent
                // is spawned. However, we ignore this since we only use the DatabasedStepsModel only observing
                // different parameters in non-locomotion models.
            }

            synchronized (domain.getTopography()) {
                domain.getTopography().moveElement(pedestrian, startPosition);
            }

            trajectoryBuffer.removeStep();
        }
        trajectoryBuffer.read();
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
        AttributesAgent aAttr = (AttributesAgent) attr;

        if (!Pedestrian.class.isAssignableFrom(type))
            throw new IllegalArgumentException("DBS cannot initialize " + type.getCanonicalName());

        AttributesAgent pedAttributes = new AttributesAgent(aAttr, registerDynamicElementId(domain.getTopography(), id));

        return createElement(position, pedAttributes);
    }

    private Pedestrian createElement(VPoint position, @NotNull final AttributesAgent attributesAgent) {
        Pedestrian pedestrian = new Pedestrian(attributesAgent, random);
        pedestrian.setPosition(position);
        return pedestrian;
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

    protected void setLocomotionHash(String locomotionHash) {
        this.locomotionHash = locomotionHash;
    }

    protected void setAttributesDSM(AttributesDSM attributesDSM) {
        this.attributesDSM = attributesDSM;
    }

    protected boolean canExtractStepsFromFile() {
        return this.canExtractStepsFromFile;
    }

    protected String getContextId() {
        if (contextId != null) {
            return contextId;
        }
        return domain.getTopography().getContextId();
    }

    protected void setContextId(String contextId) {
        this.contextId = contextId;
    }

    @Override
    public void postProcessorUpdate() {
        if (this.canExtractStepsFromFile) {
            deleteTrajectoryFile();
        } else {
            copyTrajectoryFile();
        }
    }

    private void copyTrajectoryFile() {
        try {
            Path sourcePath = Paths.get(outputPath, "postvis.traj");
            if (!Files.exists(sourcePath)) {
                logger.warn("Source trajectory file not found: " + sourcePath);
                return;
            }

            String targetFileName = getContextId() + "_" + locomotionHash + ".traj";
            File targetDir = new File(attributesDSM.getTrajectoryFileOrFolder());
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }

            Path targetPath = Paths.get(targetDir.getAbsolutePath(), targetFileName);
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            logger.info("Trajectory file copied to: " + targetPath);
        } catch (IOException e) {
            logger.error("Failed to copy trajectory file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteTrajectoryFile() {
        // When running in read-only mode, the simulator still generates a default
        // postvis.traj file. We delete it here to save disk space since we already
        // have the cached version we are reading from. This is particularly important
        // for UQ analyses.
        if (!attributesDSM.isDeletePostvisFile()) {
            return;
        }
        try {
            Path sourcePath = Paths.get(outputPath, "postvis.traj");
            if (Files.exists(sourcePath)) {
                Files.delete(sourcePath);
                logger.info("Deleted source trajectory file: " + sourcePath);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete source trajectory file: " + e.getMessage());
        }
    }
}
