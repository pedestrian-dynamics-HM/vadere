package org.vadere.state.psychology.information;

import org.vadere.state.psychology.cognition.SelfCategory;
import org.vadere.state.psychology.perception.types.KnowledgeItem;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Simple Knowledge Store to manage {@link KnowledgeItem}s.
 */
public class KnowledgeBase {

	private ArrayList<KnowledgeItem> knowledge;
	private InformationState informationState;

	public KnowledgeBase() {
		this.knowledge = new ArrayList<>();
		this.informationState = InformationState.NO_INFORMATION;
	}

	/**
	 * remove Information no longer usable at given time.
	 */
	public void updateObsolete(double current_time){
		knowledge.removeIf(i-> i.getObsoleteAt() > 0 && current_time > i.getObsoleteAt());
	}

	/**
	 *  True if KnowledgeBase contains this information.
	 */
	public boolean knowsAbout(String informationId){
		return knowledge.stream().anyMatch(i->i.getInformationId().equals(informationId));
	}
	public boolean knowsAbout(Pattern informationIdPattern){
		return knowledge.stream().anyMatch(i->informationIdPattern.matcher(i.getInformationId()).matches());
	}

	public void addInformation(KnowledgeItem info){
		knowledge.add(info);
		setInformationState(InformationState.INFORMATION_RECEIVED);
	}

	public void removeInformation(String informationId){
		knowledge.removeIf(i->i.getInformationId().equals(informationId));
	}

	public void removeInformation(Pattern informationIdPattern){
		knowledge.removeIf(i-> informationIdPattern.matcher(i.getInformationId()).matches());
	}

	public ArrayList<KnowledgeItem> getKnowledge() {
		return knowledge;
	}


	public InformationState getInformationState() {
		return informationState;
	}

	public void setInformationState(InformationState informationState) {
		this.informationState = informationState;
	}
}
