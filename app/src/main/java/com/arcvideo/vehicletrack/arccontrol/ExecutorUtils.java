package com.arcvideo.vehicletrack.arccontrol;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorUtils {
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);

    public static ExecutorService getExecutor() {
        return executor;
    }

    public static void execute(Runnable command) {
        executor.execute(command);
    }
}
