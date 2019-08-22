package org.vadere.state.attributes;

import static org.junit.Assert.*;

import org.junit.Test;
import org.vadere.state.util.StateJsonConverter;

import java.io.IOException;

public class TestPojoJsonDeserialization {

	private static final String incompleteJson = "{\"b\":5}";
	private static final String completeJson = "{\"a\":4,\"b\":5}";

	@Test
	public void testEmptyDeserializationWithImplicitDefaultCtor() throws IOException {
		// Important: After deserialization with incomplete JSON,
		// the init values (for a and b) in POJOs take effect!
		assertTestPojoEquals(new TestPojoWithImplicitDefaultConstructor(), "{}", TestPojoWithImplicitDefaultConstructor.class);
	}

	@Test
	public void testIncompleteDeserialization() throws IOException{
		// Important: After deserialization with incomplete JSON,
		// the init values (for a and b) in POJOs take effect!
		assertTestPojoEquals(new TestPojoWithDefaultConstructor(1, 5), incompleteJson, TestPojoWithDefaultConstructor.class);
	}

	@Test
	public void testCompleteDeserialization() throws IOException {
		assertTestPojoEquals(new TestPojoWithDefaultConstructor(4, 5), completeJson, TestPojoWithDefaultConstructor.class);
	}

	@Test
	public void testIncompleteDeserializationWithoutDefaultCtor() {
		try {
			assertTestPojoEquals(new TestPojoWithoutDefaultConstructor(0, 5), incompleteJson, TestPojoWithoutDefaultConstructor.class);
		} catch (IOException e) {
			//Test successful
		}
	}

	@Test
	public void testCompleteDeserializationWithoutDefaultCtor() {
		try {
			assertTestPojoEquals(new TestPojoWithoutDefaultConstructor(0, 5), completeJson, TestPojoWithoutDefaultConstructor.class);
		} catch (IOException e) {
			//Test successful
		}
	}

	private void assertTestPojoEquals(Object pojo, String json, Class<?> clazz) throws IOException {
		assertEquals(pojo, StateJsonConverter.deserializeObjectFromJson(json, clazz));
	}

	public static class TestPojoWithDefaultConstructor {
		int a = 1;
		int b = 2;
		@SuppressWarnings("unused")
		private TestPojoWithDefaultConstructor() { }
		public TestPojoWithDefaultConstructor(int a, int b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public boolean equals(Object obj) {
			// auto-generated
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestPojoWithDefaultConstructor other = (TestPojoWithDefaultConstructor) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			return true;
		}
	}

	public static class TestPojoWithImplicitDefaultConstructor {
		int a = 1;
		int b = 2;
		@Override
		public boolean equals(Object obj) {
			// auto-generated
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestPojoWithImplicitDefaultConstructor other = (TestPojoWithImplicitDefaultConstructor) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			return true;
		}
	}

	public static class TestPojoWithoutDefaultConstructor {
		int a = 1;
		int b = 2;
		public TestPojoWithoutDefaultConstructor(int a, int b) {
			this.a = a;
			this.b = b;
		}
		@Override
		public boolean equals(Object obj) {
			// auto-generated
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestPojoWithoutDefaultConstructor other = (TestPojoWithoutDefaultConstructor) obj;
			if (a != other.a)
				return false;
			if (b != other.b)
				return false;
			return true;
		}
	}

}
