package org.joget.scheduler;

import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.SetupManager;

public final class SchedulerSettings {

    public static final String THREAD_COUNT_KEY = "org.joget.scheduler.threadCount";
    public static final int DEFAULT_THREAD_COUNT = 5;
    public static final int MIN_THREAD_COUNT = 1;
    public static final int MAX_THREAD_COUNT = 100;

    private static volatile int effectiveThreadCount = DEFAULT_THREAD_COUNT;

    private SchedulerSettings() {
    }

    public static int getConfiguredThreadCount() {
        String value = getSetupManager().getSettingValue(THREAD_COUNT_KEY);
        if (value != null) {
            try {
                int threadCount = Integer.parseInt(value.trim());
                if (isValidThreadCount(threadCount)) {
                    return threadCount;
                }
            } catch (NumberFormatException e) {
                // Use the backward-compatible default.
            }
        }
        return DEFAULT_THREAD_COUNT;
    }

    public static void setConfiguredThreadCount(int threadCount) {
        if (!isValidThreadCount(threadCount)) {
            throw new IllegalArgumentException("Thread count must be between "
                    + MIN_THREAD_COUNT + " and " + MAX_THREAD_COUNT);
        }
        getSetupManager().updateSetting(THREAD_COUNT_KEY, String.valueOf(threadCount));
    }

    public static boolean isValidThreadCount(int threadCount) {
        return threadCount >= MIN_THREAD_COUNT && threadCount <= MAX_THREAD_COUNT;
    }

    public static int getEffectiveThreadCount() {
        return effectiveThreadCount;
    }

    public static void setEffectiveThreadCount(int threadCount) {
        effectiveThreadCount = threadCount;
    }

    protected static SetupManager getSetupManager() {
        return (SetupManager) AppUtil.getApplicationContext().getBean("setupManager");
    }
}
