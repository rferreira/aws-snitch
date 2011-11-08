package org.ophion.snitch.util;

import org.junit.Test;

import java.util.Date;

/**
 * Date: Nov 7, 2011
 * Time: 11:03:28 PM
 */
public class AWSUtilsTest {
    @Test
    public void testGetStateTransitionDate() throws Exception {
        Date d = AWSUtils.getStateTransitionDate("User initiated (2011-11-08 04:54:46 GMT)");
        System.out.println(d);
    }
}
