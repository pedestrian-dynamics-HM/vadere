package org.vadere.simulator.projects.dataprocessing.outputfile;

import org.vadere.annotation.factories.outputfiles.OutputFileClass;
import org.vadere.simulator.projects.dataprocessing.datakey.BonnMotionKey;

import java.util.List;

/**
 * Write BonnMotion trajectory file. This file does not contain headers or key columns see {@link
 * org.vadere.simulator.projects.dataprocessing.processor.BonnMotionTrajectoryProcessor}.
 *
 * Therefore the {@link #printHeader()} and {@link #addkeysToLine(List, String[])} are overwritten.
 *
 * @author Stefan Schuhbäck
 */
@OutputFileClass(dataKeyMapping = BonnMotionKey.class)
public class BonnMotionTrajectoryFile extends OutputFile<BonnMotionKey> {

	public BonnMotionTrajectoryFile() {
		super(BonnMotionKey.getHeader());
	}

	@Override
	void printHeader() {
		// do nothing. This File does not need the header.
	}

	@Override
	List<String> addkeysToLine(List<String> fields, String[] keyFieldArray) {
		// do nothing. This File does not need the key value.
		return fields;
	}
}
