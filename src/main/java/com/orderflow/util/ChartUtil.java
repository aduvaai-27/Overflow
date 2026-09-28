package com.orderflow.util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.util.List;

/** Small helper so every bar chart in the app doesn't end up as one flat, monotone orange block. */
public class ChartUtil {

    private ChartUtil() { }

    /** A varied, easy-to-tell-apart palette used across the app's charts. */
    private static final List<String> PALETTE = List.of(
            "#e63946", // red
            "#3a2e8f", // indigo
            "#ffc82e", // yellow
            "#00b8a3", // teal
            "#9d4edd", // purple
            "#2f80ed", // blue
            "#2ecc71", // green
            "#ff7f50", // coral
            "#1d3557", // navy
            "#e83e8c"  // pink
    );

    private static final double MIN_CATEGORY_GAP = 12;

    /**
     * Stops bars from stretching across the whole chart when there are only
     * a few of them. A BarChart has no "max bar width" setting, so this works
     * it out from the axis width and the number of bars and adjusts the
     * category gap to match - bars stay at most maxBarWidth wide and centred.
     */
    public static void limitBarWidth(BarChart<String, Number> chart, double maxBarWidth) {
        Runnable update = () -> {
            int bars = chart.getData().isEmpty() ? 0 : chart.getData().get(0).getData().size();
            double axisWidth = chart.getXAxis().getWidth();
            if (bars == 0 || axisWidth <= 0) return;
            double gap = Math.max(MIN_CATEGORY_GAP, axisWidth / bars - maxBarWidth);
            if (Math.abs(chart.getCategoryGap() - gap) > 0.5) {
                chart.setCategoryGap(gap);
            }
        };
        chart.getXAxis().widthProperty().addListener((obs, oldVal, newVal) -> update.run());
        chart.getData().addListener((ListChangeListener<XYChart.Series<String, Number>>) change -> Platform.runLater(update));
    }

    /** Only label whole numbers on a value axis (units sold can't be 0.2 of a unit). */
    public static void wholeNumberTicks(NumberAxis axis) {
        axis.setMinorTickCount(0);
        axis.setTickLabelFormatter(new StringConverter<Number>() {
            @Override
            public String toString(Number value) {
                double d = value.doubleValue();
                return d == Math.rint(d) ? String.valueOf((long) d) : "";
            }

            @Override
            public Number fromString(String text) {
                return text == null || text.isBlank() ? 0 : Double.parseDouble(text);
            }
        });
    }

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
