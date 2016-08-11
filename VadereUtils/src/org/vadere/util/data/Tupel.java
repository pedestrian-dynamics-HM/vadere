package org.vadere.util.data;

/**
 * @deprecated Use Pair from Apache Commons Lang instead.
 */
public class Tupel<V1, V2> {

	public final V1 v1;
	public final V2 v2;
	private boolean present;

	private Tupel(final V1 v1, final V2 v2, final boolean present) {
		if (present && (v1 == null || v2 == null))
			throw new IllegalArgumentException();
		this.v1 = v1;
		this.v2 = v2;
		this.present = present;
	}

	public static <V1, V2> Tupel<V1, V2> of(final V1 v1, final V2 v2) {
		return new Tupel(v1, v2, true);
	}

	public static <V1, V2> Tupel<V1, V2> empty() {
		return new Tupel(null, null, false);
	}

	public boolean isPresent() {
		return present;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj.getClass() != getClass()) {
			return false;
		} else {
			Tupel other = (Tupel) obj;
			return v1.equals(other.v1) && v2.equals(other.v2);
		}
	}

	@Override
	public int hashCode() {
		if (!isPresent()) {
			return 0;
		} else {
			return v1.hashCode() * 31 + v2.hashCode() * 31;
		}
	}
}
