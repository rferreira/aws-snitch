package org.ophion.snitch.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date: Nov 7, 2011
 * Time: 10:32:09 PM
 */
public class AWSUtils {

    static final String STATE_TRANSITION_FORMAT = "yyyy-MM-dd hh:mm:ss z";
    /**
     * Parses something that looks like this User initiated (2011-11-08 04:54:46 GMT)
     * @param s
     * @return
     */
    public static Date getStateTransitionDate(String s) throws ParseException {
        DateFormat f = new SimpleDateFormat(STATE_TRANSITION_FORMAT);
        return f.parse(s.substring( s.indexOf('(') + 1, s.indexOf(')')));

    }
}
