package org.vadere.state.psychology;

import org.vadere.state.psychology.perception.types.InformationStimulus;
import org.vadere.state.psychology.perception.types.Stimulus;

import java.util.ArrayList;
import java.util.regex.Pattern;

public class KnowledgeBase {

	private ArrayList<InformationStimulus> knowledge;

	public KnowledgeBase() {
		this.knowledge = new ArrayList<>();
	}

	/**
	 * remove Information no longer usable at given time.
	 */
	public void update_obsolete(double current_time){
		knowledge.removeIf(i-> i.getObsolete_at() > 0 && current_time > i.getObsolete_at());
	}

	/**
	 *  True if KnowledgeBase contains this information.
	 */
	public boolean knows_about(String information){
		return knowledge.stream().anyMatch(i->i.getInformation().equals(information));
	}
	public boolean knows_about(Pattern information){
		return knowledge.stream().anyMatch(i->information.matcher(i.getInformation()).matches());
	}


	public boolean knows_about(String information, Class<? extends Stimulus> clz){
		return knowledge.stream().anyMatch(i->i.getInformation().equals(information));
	}

	public void add_information(InformationStimulus info){
		knowledge.add(info);
	}

	public void remove_information(String info){
		knowledge.removeIf(i->i.getInformation().equals(info));
	}


}
