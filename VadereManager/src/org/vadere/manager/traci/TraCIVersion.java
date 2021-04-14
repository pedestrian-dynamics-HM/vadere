package org.vadere.manager.traci;

import org.vadere.state.traci.TraCIException;

public enum TraCIVersion {
	V20_0_1(20, 0, 1),
	V20_0_2(20, 0, 2); // allow cache transfer

	private static final String versionStringTemplate = "VadereTraCI-%d.%d.%d This is a TraCI Server implementing only a small subset of TraCI Version %d";

	public int traciBaseVersion;
	public int vadereApiMajor;
	public int vadereApiMinor;

	TraCIVersion(int traciBaseVersion, int vadereApiMajor, int vadereApiMinor) {
		this.traciBaseVersion = traciBaseVersion;
		this.vadereApiMajor = vadereApiMajor;
		this.vadereApiMinor = vadereApiMinor;
	}

	public static TraCIVersion valueOf(int ordinalId) {
		if (ordinalId < 0 || ordinalId >= values().length)
			throw new TraCIException("given ordinalId is outside of this Enum.");
		return values()[ordinalId];
	}

	public boolean greaterOrEqual(TraCIVersion v) {
		return ordinal() >= v.ordinal();
	}

	public String getVersionString() {
		return String.format(versionStringTemplate, traciBaseVersion, vadereApiMajor, vadereApiMinor, traciBaseVersion);
	}
}
