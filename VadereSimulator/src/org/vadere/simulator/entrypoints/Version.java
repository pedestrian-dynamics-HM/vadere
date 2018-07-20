package org.vadere.simulator.entrypoints;

import org.jetbrains.annotations.NotNull;
import org.vadere.simulator.projects.migration.incidents.VersionBumpIncident;

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

	private static int versionId(Version curr){
		Version[] versions = values();
		for ( int i = 0 ; i < versions.length ; i++){
			if (curr.equals(versions[i])){
				return  i;
			}
		}
		throw new IllegalArgumentException("Value not in Version Enumeration " + curr.toString());
	}

	public static Version nextVersion(Version curr){
		int nextId = versionId(curr) == (values().length -1) ? versionId(curr) : versionId(curr) + 1;
		return values()[nextId];
	}

	public static Version[] listToLatest (Version v){
		int start = versionId(v) == (values().length -1) ? versionId(v) : versionId(v) + 1;
		int end = values().length;
		Version[] ret = new Version[end-start];
		System.arraycopy(values(), start, ret, 0, end - start );
		return ret;

	}

	public boolean equalOrSamller(Version test){
		return versionId(this) <= versionId(test);
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
