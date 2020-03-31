package org.vadere.simulator.projects.migration;

import org.vadere.util.version.Version;

import java.util.Objects;

public class MigrationOptions {

	private final static String legacyExtensionDefault = "legacy";
	private final static String nonmigratabelExtensionDefault = "nonmigratable";

	private final String legacyExtension;
	private final String nonmigratabelExtension;
	private final Version baseVersion;
	private final boolean reapplyLatestMigrationFlag;
	private final boolean useDeprecatedAssistant = false;

	public static MigrationOptions defaultOptions() {
		return new MigrationOptions();
	}

	private MigrationOptions() {
		this.legacyExtension = legacyExtensionDefault;
		this.nonmigratabelExtension = nonmigratabelExtensionDefault;
		this.baseVersion = null;
		this.reapplyLatestMigrationFlag = false;
	}

	public static MigrationOptions reapplyFromVersion(Version baseVersion) {
		return new MigrationOptions(baseVersion, true);
	}

	public static MigrationOptions reapplyWithAutomaticVersionDiscorvery() {
		return new MigrationOptions(null, true);
	}

	private MigrationOptions(Version baseVersion, boolean reapplyLatestMigrationFlag) {
		this.legacyExtension = legacyExtensionDefault;
		this.nonmigratabelExtension = nonmigratabelExtensionDefault;
		this.baseVersion = baseVersion;
		this.reapplyLatestMigrationFlag = reapplyLatestMigrationFlag;
	}


	public String getLegacyExtension() {
		return legacyExtension;
	}

	public String getNonmigratabelExtension() {
		return nonmigratabelExtension;
	}

	public Version getBaseVersion() {
		return baseVersion;
	}

	public boolean isReapplyLatestMigrationFlag() {
		return reapplyLatestMigrationFlag;
	}

	@Override
	public String toString() {
		return "MigrationOptions{" +
				"legacyExtension='" + legacyExtension + '\'' +
				", nonmigratabelExtension='" + nonmigratabelExtension + '\'' +
				", baseVersion=" + baseVersion +
				", reapplyLatestMigrationFlag=" + reapplyLatestMigrationFlag +
				", useDeprecatedAssistant=" + useDeprecatedAssistant +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		MigrationOptions that = (MigrationOptions) o;
		return reapplyLatestMigrationFlag == that.reapplyLatestMigrationFlag &&
				useDeprecatedAssistant == that.useDeprecatedAssistant &&
				Objects.equals(legacyExtension, that.legacyExtension) &&
				Objects.equals(nonmigratabelExtension, that.nonmigratabelExtension) &&
				baseVersion == that.baseVersion;
	}

	@Override
	public int hashCode() {

		return Objects.hash(legacyExtension, nonmigratabelExtension, baseVersion, reapplyLatestMigrationFlag, useDeprecatedAssistant);
	}

	public boolean isUseDeprecatedAssistant() {

		return useDeprecatedAssistant;
	}

}
