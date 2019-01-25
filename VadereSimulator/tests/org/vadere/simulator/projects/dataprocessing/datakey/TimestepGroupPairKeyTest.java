package org.vadere.simulator.projects.dataprocessing.datakey;

import org.junit.Test;
import org.vadere.simulator.projects.dataprocessing.outputfile.GroupPairOutputFile;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

public class TimestepGroupPairKeyTest {


	@Test
	public void compareTest1(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(10,1,1,1);
		assertThat(k1.compareTo(k2), lessThan(0));
	}

	@Test
	public void compareTest2(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(10,1,1,1);
		assertThat(k2.compareTo(k1), greaterThan(0));
	}

	@Test
	public void compareTest3(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,1,1,1);
		assertThat(k1.compareTo(k2), equalTo(0));
	}

	@Test
	public void compareTest4(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,10,1,1);
		assertThat(k1.compareTo(k2), lessThan(0));
	}

	@Test
	public void compareTest5(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,1,1,2);
		assertThat(k1.compareTo(k2), lessThan(0));
	}

	@Test
	public void printTest1(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,2,3,4);
		GroupPairOutputFile file = new GroupPairOutputFile();
		assertThat(file.toStrings(k1), equalTo(new String[]{"1", "2", "3", "4"}));

	}

	@Test
	public void printTest2(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,2,4,3);
		GroupPairOutputFile file = new GroupPairOutputFile();
		assertThat(file.toStrings(k1), equalTo(new String[]{"1", "2", "3", "4"}));

	}

}