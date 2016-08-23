package org.vadere.simulator.projects.dataprocessing_mtp;

import org.vadere.simulator.control.SimulationState;
import org.vadere.simulator.models.Model;
import org.vadere.util.io.IOUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessorManager {

	private Model model;
	private Map<Integer, Processor<?, ?>> processorMap;
	private Map<Integer, AttributesProcessor> attributesMap;

	private List<LogFile<?>> logFiles;

	public ProcessorManager(List<Processor<?, ?>> processors, List<AttributesProcessor> attributesProcessor, List<LogFile<?>> logFiles) {
		this.logFiles = logFiles;

		this.attributesMap = new LinkedHashMap<>();
		for (AttributesProcessor att : attributesProcessor)
			this.attributesMap.put(att.getProcessorId(), att);

		this.processorMap = new LinkedHashMap<>();
		for (Processor<?, ?> proc : processors)
			this.processorMap.put(proc.getId(), proc);

		processors.forEach(proc -> proc.init(this.attributesMap.get(proc.getId()), this));
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public void initLogFiles() {
		logFiles.forEach(logfile -> logfile.init(this));
	}

	public Processor<?, ?> getProcessor(int id) {
		return this.processorMap.containsKey(id) ? this.processorMap.get(id) : null;
	}

	public List<LogFile<?>> getLogFiles() {
		return logFiles;
	}

	public Model getModel() {
		return this.model;
	}

	public void preLoop(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.preLoop(state));
	}

	public void update(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.update(state));
	}

	public void postLoop(final SimulationState state) {
		this.processorMap.values().forEach(proc -> proc.postLoop(state));
	}

	public Set<Integer> getProcessorIds() {
		return this.processorMap.keySet();
	}

	public Set<Integer> getAttributesProcessorIds() {
		return this.attributesMap.keySet();
	}

	public AttributesProcessor getAttributes(int processorId) {
		return this.attributesMap.get(processorId);
	}

	public void setLogPath(String directory) {
		String dateString = new SimpleDateFormat(IOUtils.DATE_FORMAT).format(new Date());
		this.logFiles.forEach(logfile -> logfile.setFileName(IOUtils.getPath(directory, String.format("%s_%s", dateString, logfile.getFileName())).toString()));
	}

	public void writeLog() {
        this.logFiles.forEach(logfile -> logfile.write());
    }
}
