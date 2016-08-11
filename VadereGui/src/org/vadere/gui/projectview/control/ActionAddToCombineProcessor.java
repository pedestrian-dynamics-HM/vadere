package org.vadere.gui.projectview.control;

import javax.swing.*;

import org.vadere.gui.projectview.view.ChooseNameDialog;
import org.vadere.gui.projectview.view.OutputProcessorsView;
import org.vadere.simulator.projects.dataprocessing.processors.*;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;

import java.awt.event.ActionEvent;
import java.util.List;

// unused!
public class ActionAddToCombineProcessor extends AbstractAction {

	private static final long serialVersionUID = 6484255913225706069L;
	private final OutputProcessorsView processorView;

	public ActionAddToCombineProcessor(final OutputProcessorsView processorView) {
		this.processorView = processorView;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		ProcessorWriter selectedWriter = processorView.getSelectedProcessorWriter();

		if (selectedWriter.getProcessor() instanceof CombineProcessor) {
			CombineProcessor combineProc = (CombineProcessor) selectedWriter.getProcessor();

			final ProcessorFactory procFactory = ProcessorFactory.getInstance();

			final ChooseNameDialog nDialog = new ChooseNameDialog(processorView.getOwner(), "Select Processor",
					procFactory.getForEachPedestrianPositionProcessorNames().toArray(new String[] {}));

			if (!nDialog.isAborted()) {
				String procName = nDialog.getSelectedName();

				ForEachPedestrianPositionProcessor processor = null;

				if (procName.equals(PedestrianDensityProcessor.class.getSimpleName())) {
					final ChooseNameDialog densityDialog =
							new ChooseNameDialog(processorView.getOwner(), "Select DensityProcessor",
									procFactory.getDensityProcessorNames().toArray(new String[] {}));
					String densityProcessorName = densityDialog.getSelectedName();
					if (densityProcessorName != null) {
						DensityProcessor densityProcessor = (DensityProcessor) procFactory
								.createProcessor(procFactory.toProcessorType(densityProcessorName));
						processor = ProcessorFactory.getInstance().createPedestrianDensityProcessor(densityProcessor);
					}
				} else {
					processor = (ForEachPedestrianPositionProcessor) procFactory
							.createProcessor(procFactory.toProcessorType(procName));
				}

				if (processor != null) {
					List<ForEachPedestrianPositionProcessor> procList = combineProc.getProcessorList();
					procList.add(processor);
					selectedWriter.setProcessor(new CombineProcessor(procList));
				}
			}
		}
	}
}
