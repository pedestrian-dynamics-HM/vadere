package org.vadere.simulator.io;

import static org.junit.Assert.assertEquals;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;

public class TestSha1Hash {

	@Before
	public void setUp() throws Exception {}

	@Test
	public void test() {
		String sha1 = DigestUtils.sha1Hex("aff");

		assertEquals("0c05aa56405c447e6678b7f3127febde5c3a9238", sha1);
	}

}
