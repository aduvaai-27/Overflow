package com.orderflow.util;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;

import java.util.function.Function;

/** Small helper to give status/phase/amount columns a bit of color instead of plain black text everywhere. */
public class TableColorUtil {

    private TableColorUtil() { }

    /** Colors a text column based on its own value (e.g. order status, payment status, stock status). */
    public static <T> void colorizeText(TableColumn<T, String> column, Function<String, String> colorForValue) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(value);
                    String color = colorForValue.apply(value);
                    setStyle(color == null ? "" : "-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                }
            }
        });
    }

    /** Colors a numeric column green when >= 0 and red when negative (e.g. a capital ledger amount). */
    public static <T> void colorizeSignedNumber(TableColumn<T, Double> column, String positiveColor, String negativeColor) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f", value));
                    setStyle("-fx-text-fill: " + (value >= 0 ? positiveColor : negativeColor) + "; -fx-font-weight: bold;");
                }
            }
        });
    }
}
