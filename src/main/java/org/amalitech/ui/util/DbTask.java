package org.amalitech.ui.util;

import javafx.concurrent.Task;

import java.util.concurrent.Callable;

public class DbTask<V> extends Task<V> {
    private final Callable<V> callable;

    public DbTask(Callable<V> callable) {
        this.callable = callable;
    }

    @Override
    protected V call() throws Exception {
        return callable.call();
    }
}
