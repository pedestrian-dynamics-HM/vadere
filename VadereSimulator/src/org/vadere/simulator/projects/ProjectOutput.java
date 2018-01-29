package org.vadere.simulator.projects;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.vadere.simulator.projects.io.IOOutput;
import org.vadere.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Represents all run simulations within this project.
 *
 * @author Stefan Schuhbäck
 */
public class ProjectOutput {
	private final VadereProject project;
	private ConcurrentMap<String, SimulationOutput> simulationOutputs;


	public ProjectOutput(final VadereProject project) {
		this.project = project;
		this.simulationOutputs = IOOutput.getSimulationOutputs(project);
	}

	/**
	 * Returns cached output directories. This function does not check if cached {@link SimulationOutput}
	 * is marked dirty. It is assumed {@link #update()} was called prior to this function.
	 *
	 * @return	list of output directories corresponding to cached {@link SimulationOutput}
	 */
	public List<File> getAllOutputDirs() {
		Path out = project.getOutputDir();
		List<File> outputs = simulationOutputs.keySet().stream()
				.map(k -> out.resolve(k).toFile())
				.collect(Collectors.toList());
		return outputs;
	}

	/**
	 * This function does not check if cached {@link SimulationOutput}
	 * is dirty. It is assumed {@link #update()} was called prior to this function.
	 *
	 * @param scenario		Prior runs to this {@link Scenario}
	 * @return				List of prior simulation runs matching selected {@link Scenario}
	 */
	public List<File> listSelectedOutputDirs(final Scenario scenario) {
		List<File> out = new ArrayList<>();
		try {
			final String hash1 = scenario.getScenarioStore().hashOfJsonRepresentation();
			for (Map.Entry<String, SimulationOutput> entry : simulationOutputs.entrySet()) {
				if (!entry.getValue().isDirty()) {
					String hash2 = entry.getValue().getScenarioHash();
					if (hash1.equals(hash2))
						out.add(project.getOutputDir().resolve(entry.getKey()).toFile());
				}
			}
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return out;
	}

	/**
	 * If the output directory is present it is marked dirty and will be re-check
	 * in the next call of {@link #update()}
	 *
	 * @param dirName	directory name of {@link SimulationOutput}
	 */
	synchronized public void markDirty(String dirName) {
		getSimulationOutput(dirName).ifPresent(SimulationOutput::setDirty);
	}

	public Optional<SimulationOutput> getSimulationOutput(String dirName) {
		return Optional.ofNullable(simulationOutputs.get(dirName));
	}

	public Scenario getScenario(String dirName){
		return getSimulationOutput(dirName).get().getSimulatedScenario();
	}

	/**
	 * re-check dirty {@link SimulationOutput} and add new valid output dirs to {@link ProjectOutput}
	 */
	public void update() {
		try {
			Files.walkFileTree(project.getOutputDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
						throws IOException {

					String dirName = dir.toFile().getName();

					//ignore corrupt directory and the subtree
					if (dir.endsWith(IOUtils.CORRUPT_DIR))
						return FileVisitResult.SKIP_SUBTREE;

					//ignore output directory but continue with subtree
					if (dir.endsWith(IOUtils.OUTPUT_DIR))
						return FileVisitResult.CONTINUE;

					Optional<SimulationOutput> outDir = getSimulationOutput(dirName);

					// only re-check existing SimulationOutput if they are dirty
					if (outDir.isPresent() && outDir.get().isDirty()) {
						Optional<SimulationOutput> newSim = IOOutput.getSimulationOutput(project, dir.toFile());
						if (newSim.isPresent()){
							simulationOutputs.put(dirName, newSim.get());
						} else {
							simulationOutputs.remove(dirName);
						}
					} else {
						// if new directory try to read it or move it corrupt if not valid.
						Optional<SimulationOutput> newSim = IOOutput.getSimulationOutput(project, dir.toFile());
						newSim.ifPresent(out -> simulationOutputs.put(dir.toFile().getName(), out));
					}

					return FileVisitResult.SKIP_SUBTREE;
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
