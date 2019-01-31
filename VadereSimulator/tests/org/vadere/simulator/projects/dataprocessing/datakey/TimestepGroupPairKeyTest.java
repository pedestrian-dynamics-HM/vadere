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
		assertThat(k1.equals(k2), equalTo(false));
	}

	@Test
	public void compareTest2(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(10,1,1,1);
		assertThat(k2.compareTo(k1), greaterThan(0));
		assertThat(k1.equals(k2), equalTo(false));

	}

	@Test
	public void compareTest3(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,1,1,1);
		assertThat(k1.compareTo(k2), equalTo(0));
		assertThat(k1.equals(k2), equalTo(true));

	}

	@Test
	public void compareTest4(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,10,1,1);
		assertThat(k1.compareTo(k2), lessThan(0));
		assertThat(k1.equals(k2), equalTo(false));

	}

	@Test
	public void compareTest5(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,1,1,1);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,1,1,2);
		assertThat(k1.compareTo(k2), lessThan(0));
		assertThat(k1.equals(k2), equalTo(false));

	}

	@Test
	public void compareTest6(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(2,3,3,2);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(2,3,2,3);
		assertThat(k1.compareTo(k2), equalTo(0));
		assertThat(k1.equals(k2), equalTo(true));

	}

	@Test
	public void compareTest7(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(2,2,2,3);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(2,3,2,3);
		assertThat(k1.compareTo(k2), lessThan(0));
		assertThat(k1.equals(k2), equalTo(false));

	}


	@Test
	public void compareTest8(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(2,4,3,2);
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(2,3,2,3);
		assertThat(k1.compareTo(k2), greaterThan(0));
		assertThat(k1.equals(k2), equalTo(false));

	}


	@Test
	public void printTest1(){
		TimestepGroupPairKey k1 = new TimestepGroupPairKey(1,2,3,4);
		GroupPairOutputFile file1 = new GroupPairOutputFile();
		assertThat(file1.toStrings(k1), equalTo(new String[]{"1", "2", "3", "4"}));
		TimestepGroupPairKey k2 = new TimestepGroupPairKey(1,2,4,3);
		GroupPairOutputFile file2 = new GroupPairOutputFile();
		assertThat(file2.toStrings(k2), equalTo(new String[]{"1", "2", "4", "3"}));
		assertThat(k1.equals(k2), equalTo(true));
		assertThat(k2.equals(k1), equalTo(true));
	}

}