package org.vadere.simulator.entrypoints;

import org.junit.Test;
import org.vadere.util.version.Version;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.*;

public class VersionTest {



    @Test
    public void test_UNDEFINED(){
        assertThat("undefined", equalTo(Version.UNDEFINED.label()));
        assertThat(-1, equalTo(Version.UNDEFINED.major()));
        assertThat(-1, equalTo(Version.UNDEFINED.minor()));
    }

    @Test
    public void test_NOT_A_RELEASE(){
        assertThat("not a release", equalTo(Version.NOT_A_RELEASE.label()));
        assertThat(-1, equalTo(Version.NOT_A_RELEASE.major()));
        assertThat(-1, equalTo(Version.NOT_A_RELEASE.minor()));
    }

    @Test
    public void test_V01(){
        assertThat("0.1", equalTo(Version.V0_1.label()));
        assertThat(0, equalTo(Version.V0_1.major()));
        assertThat(1, equalTo(Version.V0_1.minor()));
    }

    @Test
    public void test_V08(){
        assertThat("0.8", equalTo(Version.V0_8.label()));
        assertThat(0, equalTo(Version.V0_8.major()));
        assertThat(8, equalTo(Version.V0_8.minor()));
    }

}