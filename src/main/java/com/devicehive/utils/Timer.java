package com.devicehive.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * This class is used to log execution time of methods
 */
public class Timer {

    private long start;

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);

    /**
     * Creates new instance
     */
    public static Timer newInstance() {
        return new Timer();
    }

    private Timer() {
        start = Calendar.getInstance().getTimeInMillis();
    }

    /**
     * @return number of milliseconds, passed from creation, or reset call
     */
    public long click() {
        return Calendar.getInstance().getTimeInMillis() - start;
    }

    /**
     * reset counter
     */
    public long reset() {
        long end = Calendar.getInstance().getTimeInMillis() - start;
        start = Calendar.getInstance().getTimeInMillis();
        return end;
    }

    public void logMethodExecuted(String methodName) {
        long time = click();
        if (time > 800) {
            logger.warn("Execution of " + methodName + " took " + time + " milliseconds");
        }else{
            logger.debug("Execution of " + methodName + " took " + time + " milliseconds");
        }

    }

}
