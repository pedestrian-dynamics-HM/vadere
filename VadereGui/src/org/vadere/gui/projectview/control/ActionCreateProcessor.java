package org.vadere.gui.projectview.control;

import javax.swing.*;

import org.vadere.gui.projectview.view.ChooseNameDialog;
import org.vadere.gui.projectview.view.ChooseProcessorTypeDialog;
import org.vadere.gui.projectview.view.OutputProcessorsView;
import org.vadere.simulator.projects.dataprocessing.processors.*;
import org.vadere.simulator.projects.dataprocessing.writer.ProcessorWriter;
import org.vadere.state.attributes.processors.AttributesWriter;

import java.awt.event.ActionEvent;

public class ActionCreateProcessor extends AbstractAction {

	private static final long serialVersionUID = 4844916448893626628L;
	private OutputProcessorsView processorView;

	public ActionCreateProcessor(final OutputProcessorsView processorView) {
		this.processorView = processorView;
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final ChooseProcessorTypeDialog dialog = new ChooseProcessorTypeDialog(processorView.getOwner());
		dialog.setVisible(true);

		String simpleProcessorName = dialog.getSelectedProcessorSimpleName();

		ProcessorFactory procFactory = ProcessorFactory.getInstance();

		Processor processor = null;
		if (!dialog.isAborted()) {
			if (simpleProcessorName.equals(PedestrianDensityProcessor.class.getSimpleName())
					|| simpleProcessorName.equals(PedestrianFlowProcessor.class.getSimpleName())) {
				final ChooseNameDialog nDialog = new ChooseNameDialog(processorView.getOwner(),
						"Select DensityProcessor", procFactory.getDensityProcessorNames().toArray(new String[] {}));

				if (!nDialog.isAborted()) {
					String densityProcessorName = nDialog.getSelectedName();
					if (densityProcessorName != null) {
						DensityProcessor densityProcessor = (DensityProcessor) procFactory
								.createProcessor(procFactory.toProcessorType(densityProcessorName));
						if (simpleProcessorName.equals(PedestrianFlowProcessor.class.getSimpleName())) {
							processor = ProcessorFactory.getInstance().createPedestrianFlowProcessor(densityProcessor);
						} else {
							processor =
									ProcessorFactory.getInstance().createPedestrianDensityProcessor(densityProcessor);
						}
					}
				}

			} else if (simpleProcessorName.equals(CombineProcessor.class.getSimpleName())) {
				processor = procFactory.createCombineProcessor(new String[] {});
			} else {
				processor = ProcessorFactory.getInstance().createProcessor(dialog.getSelectedProcessorSimpleName());
			}

			if (processor != null) {
				processorView.addProcessorWriter(new ProcessorWriter(processor, new AttributesWriter()));
			}
		}
	}
}
