package org.vadere.util.data;

/**
 * @deprecated Use Triple from Apache Commons Lang instead.
 */
public class Tripel<V1, V2, V3> {
	public final V1 v1;
	public final V2 v2;
	public final V3 v3;
	private final boolean present;

	private Tripel(final V1 v1, final V2 v2, final V3 v3, boolean present) {
		if (present && (v1 == null || v2 == null || v3 == null)) {
			throw new IllegalArgumentException("some values are null, which is not allowed");
		}
		this.v1 = v1;
		this.v2 = v2;
		this.v3 = v3;
		this.present = present;
	}

	public static <V1, V2, V3> Tripel<V1, V2, V3> of(final V1 v1, final V2 v2, final V3 v3) {
		return new Tripel<>(v1, v2, v3, true);
	}

	public static Tripel empty() {
		return new Tripel(null, null, null, false);
	}

	public boolean isPresent() {
		return present;
	}

	@Override
	public int hashCode() {
		if (!isPresent()) {
			return 0;
		} else {
			return v1.hashCode() * 31 + v2.hashCode() * 31 + v3.hashCode() * 31;
		}
	}
}
