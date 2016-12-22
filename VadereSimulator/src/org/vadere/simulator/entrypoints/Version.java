package org.vadere.simulator.entrypoints;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

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

	public static Optional<Version> getPrevious(@NotNull final Version successorVersion) {
		Version prevVersion = null;

		for(Version version : values()) {
			if(successorVersion.equals(version)) {
				return Optional.ofNullable(prevVersion);
			}
			prevVersion = version;
		}

		return Optional.empty();
	}
	
	@Override
	public String toString() {
		return label();
	}

}
