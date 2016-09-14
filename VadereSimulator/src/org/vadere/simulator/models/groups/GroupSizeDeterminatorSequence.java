package org.vadere.simulator.models.groups;

import java.util.List;

public class GroupSizeDeterminatorSequence implements GroupSizeDeterminator {

	private final List<Integer> groupSizeSequence;
	private final boolean finiteSequence;
	private final GroupSizeDeterminator subsequentGroupSize;

	private int times;
	private int offset;

	private GroupSizeDeterminatorSequence(List<Integer> groupSizeSequence,
			GroupSizeDeterminator subsequentGroupSize, boolean finiteSequence) {

		this.finiteSequence = finiteSequence;
		this.groupSizeSequence = groupSizeSequence;
		this.subsequentGroupSize = subsequentGroupSize;
	}

	GroupSizeDeterminatorSequence(List<Integer> groupSizeSequence, int times,
			GroupSizeDeterminator subsequentGroupSize) {

		this(groupSizeSequence, subsequentGroupSize, true);

		this.times = times;
		offset = 0;
	}

	GroupSizeDeterminatorSequence(List<Integer> groupSizeSequence,
			GroupSizeDeterminator subsequentGroupSize) {

		this(groupSizeSequence, 1, subsequentGroupSize);
	}

	GroupSizeDeterminatorSequence(List<Integer> groupSizeSequence) {

		this(groupSizeSequence, null, false);
	}

	@Override
	public int nextGroupSize() {
		int result;

		if (finiteSequence && times < 1) {
			result = subsequentGroupSize.nextGroupSize();
		} else {
			result = groupSizeSequence.get(offset);

			offset++;

			if (offset >= groupSizeSequence.size()) {
				offset = 0;
				times--;
			}
		}

		return result;
	}

}
