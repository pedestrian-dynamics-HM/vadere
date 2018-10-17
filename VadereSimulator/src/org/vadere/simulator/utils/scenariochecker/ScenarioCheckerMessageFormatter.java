package org.vadere.simulator.utils.scenariochecker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public interface ScenarioCheckerMessageFormatter {

	/**
	 * Return a String representation of the given Messages in the order
	 * of the given PriorityQueue Errors -> Warnings
	 *
	 * @param msg List of Messages
	 * @return String representation of Messages;
	 */
	String formatMessages(PriorityQueue<ScenarioCheckerMessage> msg);



	/**
	 * Return a String representation of the given Messages in the order
	 * of the given PriorityQueue. Only return messages matching any of the Types
	 * given by typeFilter
	 *
	 * @param msg List of Messages
	 * @param typeFilter only include messages of the given types.
	 * @return String representation of Messages;
	 */
	default String formatMessages(PriorityQueue<ScenarioCheckerMessage> msg,
								  ScenarioCheckerMessageType... typeFilter){
		Set<ScenarioCheckerMessageType> typeSet = new HashSet<>(Arrays.asList(typeFilter));
		return formatMessages(msg.stream()
				.filter(m -> typeSet.contains(m.getMsgType()))
				.collect(Collectors.toCollection(PriorityQueue::new))
		);
	}
}
