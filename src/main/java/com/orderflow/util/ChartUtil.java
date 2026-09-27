package com.orderflow.util;

import javafx.scene.Node;
import javafx.scene.chart.XYChart;

import java.util.List;

/** Small helper so every bar chart in the app doesn't end up as one flat, monotone block. */
public class ChartUtil {

    private ChartUtil() { }

    /** A varied, easy-to-tell-apart palette, tuned to sit with the app's indigo/slate identity instead of clashing against it. */
    private static final List<String> PALETTE = List.of(
            "#4f46e5", // indigo (brand)
            "#0ea5e9", // sky blue
            "#14b8a6", // teal
            "#f59e0b", // amber
            "#ec4899", // rose
            "#8b5cf6", // violet
            "#10b981", // emerald
            "#64748b"  // slate gray
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
