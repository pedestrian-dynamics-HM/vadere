package org.vadere.simulator.entrypoints;

/** Versions in strict order from oldest to newest. */
public enum Version {

	UNDEFINED("undefined"),
	NOT_A_RELEASE("not a release"),
	V0_1("0.1"),
	V0_2("0.2");

	private String label;

	Version(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}

	public static Version fromString(String versionStr) {
		for (Version v : values()) {
			if (v.label.equals(versionStr))
				return v;
		}
		return null;
	}

	public static Version latest() {
		return values()[values().length - 1];
	}
	
	@Override
	public String toString() {
		return label();
	}

}
