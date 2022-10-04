package org.vadere.util.version;

import org.jetbrains.annotations.NotNull;
import org.vadere.util.logging.Logger;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Optional;
import java.util.Scanner;

/**
 * Vadere versions in strict order from oldest to newest.
 *
 * In Vadere, each release consists of a semantic version with "major.minor"
 * and the corresponding commit hash from the version control system.
 *
 * The commit hash from version control is stored in {@link #VERSION_CONTROL_INFO_FILE}
 * when compiling Vadere (during Maven's "generate-resources" phase).
 */
public enum Version {

	UNDEFINED("undefined"),
	NOT_A_RELEASE("not a release"),
	V0_1(0, 1),
	V0_2(0, 2),
	V0_3(0, 3),
	V0_4(0, 4),
	V0_5(0, 5),
	V0_6(0, 6),
	V0_7(0, 7),
	V0_8(0, 8),
	V0_9(0,9),
	V0_10(0,10),
	V1_0(1,0),
	V1_1(1,1),
	V1_2(1,2),
	V1_3(1,3),
	V1_4(1,4),
	V1_5(1,5),
	V1_6(1,6),
	V1_7(1,7),
	V1_8(1, 8),
	V1_9(1, 9),
	V1_10(1, 10),
	V1_11(1, 11),
	V1_12(1, 12),
	V1_13(1, 13),
	V1_14(1,14),
	V1_15(1,15),
	V1_16(1,16),
	V2_0(2,0),
	V2_1(2,1),
	V2_2(2,2),
	V2_3(2,3),
	V2_5(2,4),
	;


	private static final Logger logger = Logger.getLogger(Version.class);
	private static final String VERSION_CONTROL_INFO_FILE = "/current_commit_hash.txt";

	private final String label;
	private final int major;
	private final int minor;

	Version(String label) {
		this.major = -1;
		this.minor = -1;
		this.label = label;
	}

	Version(int major, int minor){
		this.major = major;
		this.minor = minor;
		this.label = major + "." + minor;
	}

	public static String getVersionControlCommitHash() {
		String commitHash = readFileFromJavaResourceFolder(VERSION_CONTROL_INFO_FILE);

		if (commitHash == null) {
			commitHash = "No version control commit hash available!";
			logger.warn("No version control commit hash available!");
		}

		return commitHash;
	}

	private static String readFileFromJavaResourceFolder(String resource) {
		final InputStream in = Version.class.getResourceAsStream(resource);
		if (in != null) {
			try (final Scanner scanner = new Scanner(in)) {
				if (scanner.hasNext()) {
					return scanner.next();
				}
			}
		}
		return null;
	}

	public static String releaseNumber() {
		return latest().label();
	}

	public String label() {
		return label;
	}

	public int major() { return major;}

	public int minor() { return  minor;}

	public String label(char replaceSpaceWith) {
		return label.replace(' ', replaceSpaceWith);
	}

	public static Version fromString(String versionStr) {
		versionStr = versionStr.replace('_', ' ');
		for (Version v : values()) {
			if (v.label.equals(versionStr))
				return v;
		}
		return null;
	}

	private static int versionId(Version curr) {
		Version[] versions = values();
		for (int i = 0; i < versions.length; i++) {
			if (curr.equals(versions[i])) {
				return i;
			}
		}
		throw new IllegalArgumentException("Value not in Version Enumeration " + curr.toString());
	}

	public static String[] stringValues() {
		return Arrays.stream(values()).map(v -> v.label().replace(' ', '_')).toArray(String[]::new);
	}

	public static String[] stringValues(Version startFrom, boolean descending) {
		int min = startFrom.ordinal();
		String[] values = Arrays.stream(values()).filter(v -> v.ordinal() >= min).map(v -> v.label().replace(' ', '_')).toArray(String[]::new);
		if (descending){
 		    int length = values.length;
		    for (int i = 0; i < length/2; i++){
		        int j = length -1 - i;
		        String tmp = values[i];
		        values[i] = values[j];
		        values[j] = tmp;


            }
        }
		return values;
    }

	public Version nextVersion() {
		int nextId = versionId(this) == (values().length - 1) ? versionId(this) : versionId(this) + 1;
		return values()[nextId];
	}

	public Version previousVersion() {
		int nextId = versionId(this) == 0 ? versionId(this) : versionId(this) - 1;
		return values()[nextId];
	}

	public static Version[] listVersionFromTo(Version from, Version to){
		int start = versionId(from) == (values().length - 1) ? versionId(from) : versionId(from) + 1;
		int end =  versionId(to);
		Version[] ret = new Version[(end - start) + 1];
		Version[] values = values();
		System.arraycopy(values, start, ret, 0, (end - start)+1);
		return ret;
	}

	public static Version[] listToLatest(Version v) {
		int start = versionId(v) == (values().length - 1) ? versionId(v) : versionId(v) + 1;
		int end = values().length;
		Version[] ret = new Version[end - start];
		System.arraycopy(values(), start, ret, 0, end - start);
		return ret;

	}

	public boolean equalOrSmaller(Version test) {
		return this.ordinal() <= test.ordinal();
	}

	public boolean equalOrBigger(Version test) {
		return this.ordinal() >= test.ordinal();
	}

	public static Version latest() {
		return values()[values().length - 1];
	}

	public static Optional<Version> getPrevious(@NotNull final Version successorVersion) {
		Version prevVersion = null;

		for (Version version : values()) {
			if (successorVersion.equals(version)) {
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
