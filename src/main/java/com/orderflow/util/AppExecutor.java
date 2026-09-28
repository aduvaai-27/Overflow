package com.orderflow.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Topic: Java Multithreading and Concurrency (thread pool, Executor framework).
 *
 * One shared fixed-size thread pool for background work (network calls,
 * heavy report queries). Reusing 4 worker threads is cheaper than creating
 * a new Thread for every job, and it caps how many run at once.
 * Worker names come from an AtomicInteger, a thread-safe counter.
 */
public final class AppExecutor {

    private static final int POOL_SIZE = 4;
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(1);

    private static final ExecutorService POOL = Executors.newFixedThreadPool(POOL_SIZE, runnable -> {
        Thread t = new Thread(runnable, "orderflow-pool-" + THREAD_COUNTER.getAndIncrement());
        t.setDaemon(true); // never keeps the app alive after the window closes
        return t;
    });

    private AppExecutor() {}

    /** Runs the job on one of the pool's worker threads. */
    public static void execute(Runnable job) {
        POOL.execute(job);
    }

    /** Called once when the application closes. */
    public static void shutdown() {
        POOL.shutdownNow();
    }
}
