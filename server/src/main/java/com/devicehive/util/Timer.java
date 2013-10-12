package com.devicehive.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

/**
 * This class is used to log execution time of methods
 */
public class Timer {

    private long start;

    private static final Logger logger = LoggerFactory.getLogger(Timer.class);

    private static final int MAX_METHOD_EXECUTION_TIME = 50000; // 50ms

    /**
     * Creates new instance
     */
    public static Timer newInstance() {
        return new Timer();
    }

    private Timer() {
        start = System.nanoTime();
    }

    /**
     * @return number of milliseconds, passed from creation, or reset call
     */
    public long click() {
        return System.nanoTime() - start;
    }

    /**
     * reset counter
     */
    public long reset() {
        long end = Calendar.getInstance().getTimeInMillis() - start;
        start = Calendar.getInstance().getTimeInMillis();
        return end;
    }

    /**
     * logs message like "Execution of methodName  took 100 milliseconds"
     * if execution time is less or equal MAX_METHOD_EXECUTION_TIME will log with debug priority,
     * Will log with warning priority otherwise
     *
     * @param methodName executed method name
     */
    public void logMethodExecuted(String methodName) {
        long time = click() / 1000;
        if (time > MAX_METHOD_EXECUTION_TIME) {
            logger.warn("Execution of {} took {} microseconds", methodName, time);
        } else {
            logger.info("Execution of {} took {} microseconds", methodName, time);
        }

    }

}
