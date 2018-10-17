package org.vadere.simulator.utils.scenariochecker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public abstract class AbstractScenarioCheckerMessageFormatter implements  ScenarioCheckerMessageFormatter{

	protected ScenarioCheckerMessageType currentType;
	protected StringBuilder sb;

	public AbstractScenarioCheckerMessageFormatter(){
		this.sb = new StringBuilder();
	}

	@Override
	public String formatMessages(PriorityQueue<ScenarioCheckerMessage> scenarioCheckerMessages) {
		sb.setLength(0);

		ScenarioCheckerMessage[] messages = new ScenarioCheckerMessage[scenarioCheckerMessages.size()];
		scenarioCheckerMessages.toArray(messages);
		Arrays.sort(messages);

		for (ScenarioCheckerMessage msg : messages) {
			if (isNewType(msg)){
				writeHeader(msg);
			}
			writeMsg(msg);
		}

		return sb.toString();
	}

	/**
	 * default comparator used for PriorityQueue. It sorts messages from Errors to Warnings.
	 * @return
	 */
	protected  Comparator<ScenarioCheckerMessage> getComparator(){
		return ScenarioCheckerMessage::compareTo;
	}

	protected boolean isNewType(ScenarioCheckerMessage msg){
		if (currentType == null || !currentType.equals(msg.getMsgType())){
			currentType = msg.getMsgType();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * If msg is the first message of its {@link org.vadere.state.types.ScenarioElementType} write
	 * some header or headline.
	 * @param msg current Message
	 */
	protected abstract void writeHeader(ScenarioCheckerMessage msg);


	/**
	 * Write msg to StringBuilder
	 * @param msg current Message
	 */
	protected abstract void writeMsg(ScenarioCheckerMessage msg);
}
