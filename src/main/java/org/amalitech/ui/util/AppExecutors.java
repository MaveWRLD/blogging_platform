package org.amalitech.ui.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {
    private static final ExecutorService DB = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "db-task-thread");
        t.setDaemon(true);
        return t;
    });

    private AppExecutors() {}

    public static ExecutorService db() {
        return DB;
    }

    public static ExecutorService getDbExecutor() {
        return DB;
    }
}
