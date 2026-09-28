package com.orderflow.service;

import com.orderflow.dao.ProductDAO;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

/**
 * Topic covered: "Java Multithreading and Concurrency".
 *
 * This class runs a background task on a SEPARATE thread from the JavaFX
 * Application Thread (the UI thread). Every few seconds it queries the
 * database for how many products are low on stock and reports the result
 * back to the UI.
 *
 * We must never touch JavaFX controls from a background thread directly,
 * so the result is delivered through Platform.runLater(), which schedules
 * the update to run safely on the JavaFX Application Thread.
 */
public class StockAlertMonitor implements BackgroundService {

    private final ProductDAO productDAO = new ProductDAO();
    private ScheduledExecutorService executor;

    /**
     * Starts the monitor. onUpdate is called (on the UI thread) every time
     * a new low-stock count is available.
     */
    public void start(IntConsumer onUpdate, int intervalSeconds) {
        executor = Executors.newScheduledThreadPool(2, runnable -> {
            Thread t = new Thread(runnable, "stock-alert-monitor-thread");
            t.setDaemon(true); // dies automatically when the app closes
            return t;
        });

        executor.scheduleAtFixedRate(() -> {
            // --- runs on the background thread ---
            int lowStockCount = productDAO.countLowStock();

            // --- hand the result back to the JavaFX UI thread ---
            Platform.runLater(() -> onUpdate.accept(lowStockCount));

        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
