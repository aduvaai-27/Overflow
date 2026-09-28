package com.orderflow.util;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.util.StringConverter;

import java.util.List;

public class ChartUtil {

    private ChartUtil() { }

    private static final List<String> PALETTE = List.of(
            "#e63946",
            "#3a2e8f",
            "#ffc82e",
            "#00b8a3",
            "#9d4edd",
            "#2f80ed",
            "#2ecc71",
            "#ff7f50",
            "#1d3557",
            "#e83e8c"
    );

    private static final double MIN_CATEGORY_GAP = 12;

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

    public static void colorizeCategorical(XYChart.Series<String, Number> series) {
        int i = 0;
        for (XYChart.Data<String, Number> data : series.getData()) {
            String color = PALETTE.get(i % PALETTE.size());
            applyColorWhenReady(data, color);
            i++;
        }
    }

    public static void colorizeByValue(XYChart.Series<String, Number> series, String positiveColor, String negativeColor) {
        for (XYChart.Data<String, Number> data : series.getData()) {
            double value = data.getYValue().doubleValue();
            applyColorWhenReady(data, value >= 0 ? positiveColor : negativeColor);
        }
    }

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
