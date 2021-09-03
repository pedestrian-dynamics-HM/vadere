package org.vadere.state.psychology.perception.types;

import java.util.Objects;

/**
 * Class encodes some kind of information a pedestrian knows about.
 * The information is active from the time gien in {@link #time} and will be
 * forgotten at {@link #obsoleteAt} (or never if {@link #obsoleteAt} == -1
 */
public class KnowledgeItem extends Stimulus {

	private String informationId;
	private double obsoleteAt;

	public KnowledgeItem(String informationId) {
		super(0.0);
		this.informationId = informationId;
		this.obsoleteAt = -1; // never
	}

	public KnowledgeItem(double time, double obsoleteAt, String informationId) {
		super(time);
		this.informationId = informationId;
		this.obsoleteAt = obsoleteAt;
	}


	public KnowledgeItem(double time, double obsoleteAt, String informationId, int id) {
		super(time, id);
		this.informationId = informationId;
		this.obsoleteAt = obsoleteAt;
	}

	public KnowledgeItem(KnowledgeItem other) {
		super(other.time);
		this.informationId = other.informationId;
		this.obsoleteAt = other.obsoleteAt;
	}

	public String getInformationId() {
		return informationId;
	}


	public double getObsoleteAt() {
		return obsoleteAt;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KnowledgeItem that = (KnowledgeItem) o;
		return Double.compare(that.obsoleteAt, obsoleteAt) == 0 &&
				Objects.equals(informationId, that.informationId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(informationId, obsoleteAt);
	}

	@Override
	public Stimulus clone() {
		return new KnowledgeItem(this);
	}
}
