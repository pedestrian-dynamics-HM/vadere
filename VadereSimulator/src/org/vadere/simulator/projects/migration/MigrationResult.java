package org.vadere.simulator.projects.migration;

import java.util.Objects;

public class MigrationResult {
	public int total;
	public int upToDate;
	public int legacy;
	public int notmigratable;

	public MigrationResult() {
	}

	public MigrationResult(int total, int upToDate, int legacy, int notmigratable) {
		this.total = total;
		this.upToDate = upToDate;
		this.legacy = legacy;
		this.notmigratable = notmigratable;
	}

	public MigrationResult(int total) {
		this.total = total;
	}

	boolean checkTotal() {
		return (upToDate + legacy + notmigratable) == total;
	}

	public MigrationResult add(MigrationResult other) {
		this.total = this.total + other.total;
		this.upToDate = this.upToDate + other.upToDate;
		this.legacy = this.legacy + other.legacy;
		this.notmigratable = this.notmigratable + other.notmigratable;
		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MigrationResult result = (MigrationResult) o;
		return total == result.total &&
				upToDate == result.upToDate &&
				legacy == result.legacy &&
				notmigratable == result.notmigratable;
	}

	@Override
	public int hashCode() {

		return Objects.hash(total, upToDate, legacy, notmigratable);
	}

	@Override
	public String toString() {
		return "MigrationResult{" +
				"total=" + total +
				", upToDate=" + upToDate +
				", legacy=" + legacy +
				", notmigratable=" + notmigratable +
				'}';
	}
}
