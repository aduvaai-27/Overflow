package com.orderflow.util;

import javafx.scene.Node;
import javafx.scene.chart.XYChart;

import java.util.List;

/** Small helper so every bar chart in the app doesn't end up as one flat, monotone orange block. */
public class ChartUtil {

    private ChartUtil() { }

    /** A varied, easy-to-tell-apart palette used across the app's charts. */
    private static final List<String> PALETTE = List.of(
            "#3498db", // blue
            "#e67e22", // orange
            "#2ecc71", // green
            "#9b59b6", // purple
            "#e74c3c", // red
            "#1abc9c", // teal
            "#f1c40f", // yellow
            "#e84393", // pink
            "#34495e", // navy
            "#16a085"  // dark teal
    );

    /** Colors each bar in the series differently, cycling through the palette. */
    public static void colorizeCategorical(XYChart.Series<String, Number> series) {
        int i = 0;
        for (XYChart.Data<String, Number> data : series.getData()) {
            String color = PALETTE.get(i % PALETTE.size());
            applyColorWhenReady(data, color);
            i++;
        }
    }

    /** Colors each bar green if its value is >= 0, or red if it's negative (e.g. a profit/loss chart). */
    public static void colorizeByValue(XYChart.Series<String, Number> series, String positiveColor, String negativeColor) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            double value = data.getYValue().doubleValue();
            applyColorWhenReady(data, value >= 0 ? positiveColor : negativeColor);
        }
    }

    /**
     * A chart's bar Node isn't created immediately when data is added to the
     * series - JavaFX builds it during the next layout pass - so we style it
     * once it actually appears.
     */
    private static void applyColorWhenReady(XYChart.Data<String, Number> data, String hexColor) {
        Node node = data.getNode();
        if (node != null) {
            node.setStyle("-fx-bar-fill: " + hexColor + ";");
        }
        data.nodeProperty().addListener((obs, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-bar-fill: " + hexColor + ";");
            }
        });
    }
}
