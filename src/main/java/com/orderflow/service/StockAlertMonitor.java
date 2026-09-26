package com.orderflow.service;

import com.orderflow.dao.ProductDAO;
import javafx.application.Platform;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.IntConsumer;

public class StockAlertMonitor {

    private final ProductDAO productDAO = new ProductDAO();
    private ScheduledExecutorService executor;

    public void start(IntConsumer onUpdate, int intervalSeconds) {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread t = new Thread(runnable, "stock-alert-monitor-thread");
            t.setDaemon(true);
            return t;
        });

        executor.scheduleAtFixedRate(() -> {

            int lowStockCount = productDAO.countLowStock();

            Platform.runLater(() -> onUpdate.accept(lowStockCount));

        }, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
