package org.vadere.manager;

import org.junit.Test;

public class TraCISocketTest {

	@Test
	public void unisgnedChar(){
		int a = 130;
		byte b = (byte)a;
		byte c = (byte)(a - 256);

		System.out.printf("%032X  %d\n", a, a);
		System.out.printf("%032X  %d\n", b, b);
		System.out.printf("%032X  %d\n", c, c);
		System.out.println("xxx");
	}

}