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

	private OutputDirWatcher watcher;

	public ProjectOutput(VadereProject project) {
		this.project = project;

		this.simulationOutputs = IOOutput.getSimulationOutputs(project);


		// add watched directories manually to ensure only valid  output directories
		// and the root output directory are added.
		// TODO exceptions?
		try {
			OutputDirWatcherBuilder builder = new OutputDirWatcherBuilder();
			builder.initOutputDirWatcher(project);
			builder.register(project.getOutputDir());
			builder.addDefaultEventHandler();
			Path out = project.getOutputDir();
			List<Path> outputs = simulationOutputs.keySet().stream()
					.map(k -> out.resolve(k))
					.collect(Collectors.toList());
			builder.register(outputs);
			this.watcher = builder.build();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	public List<File> getAllOutputDirs() {
		System.out.println("No FS Touch!");
		Path out = project.getOutputDir();
		// Keys of concurrentMap are the output directories for each simulation run.
		// Resolve them against the project output dir gets you the needed paths/files.
		List<File> outputs = simulationOutputs.keySet().stream()
				.map(k -> out.resolve(k).toFile())
				.collect(Collectors.toList());
		return outputs;
	}

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

	synchronized public void reloadData() {

	}

	synchronized public void markDirty(String outputDir) {
		Optional.ofNullable(this.simulationOutputs.get(outputDir)).ifPresent(SimulationOutput::setDirty);
	}

	public ConcurrentMap<String, SimulationOutput> getSimulationOutputs() {
		return simulationOutputs;
	}

	public void setSimulationOutputs(ConcurrentMap<String, SimulationOutput> simulationOutputs) {
		this.simulationOutputs = simulationOutputs;
	}

	public Optional<SimulationOutput> getSimulationOutput(String dirName) {
		return Optional.ofNullable(simulationOutputs.get(dirName));
	}

	public void cleanOutputDirs() {
		Iterator<Map.Entry<String, SimulationOutput>> iter = this.simulationOutputs.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, SimulationOutput> entry = iter.next();
			if (entry.getValue().isDirty()) {
				Optional<SimulationOutput> updated =
						IOOutput.getSimulationOutput(project, entry.getValue().getOutputDir());
				if (updated.isPresent()) {
					entry.setValue(updated.get());
				} else {
					//existing dir went invalid.
					iter.remove();
				}
			}
		}

	}

	public void update() {
		List<File> existingOutputDirs = getAllOutputDirs();
		try {
			Files.walkFileTree(project.getOutputDir(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
						throws IOException {

					if (dir.endsWith(IOUtils.CORRUPT_DIR)) {
						return FileVisitResult.SKIP_SUBTREE;
					}

					if (!existingOutputDirs.contains(dir.toFile())) {
						Optional<SimulationOutput> newSim = IOOutput.getSimulationOutput(project, dir.toFile());
						newSim.ifPresent(out -> simulationOutputs.put(dir.toFile().getName(), out));
					}

					return FileVisitResult.CONTINUE;
				}

			});
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
