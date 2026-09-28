package com.orderflow.util;

import javafx.beans.binding.Bindings;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.chart.Chart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Region;

/**
 * Topic: JavaFX layout responsiveness (property bindings to window size).
 *
 * The screens were designed at the default 1000x650 window. Fixed-width side
 * panels (forms) and charts used to stay the same size forever. Here their
 * sizes are BOUND to the width / height of the content area, so they grow
 * smoothly when the window is enlarged. At the default and minimum window
 * size the original design sizes are kept (never smaller than the design).
 */
public final class ResponsiveUtil {

    /** Content-area size the screens were originally designed for. */
    private static final double DESIGN_WIDTH = 960;
    private static final double DESIGN_HEIGHT = 470;
    /** Panels/charts may grow at most this many times their design size. */
    private static final double MAX_GROWTH = 1.5;

    private ResponsiveUtil() {}

    /** Walks the loaded view and binds fixed-size panels and charts to the container's size. */
    public static void apply(Node view, Region container) {
        if (view instanceof Region) {
            Region region = (Region) view;
            if (isFixedWidthPanel(region)) {
                bindWidth(region, container);
            }
        }
        if (view instanceof Chart) {
            bindHeight((Region) view, container);
        }

        if (view instanceof ScrollPane && ((ScrollPane) view).getContent() != null) {
            apply(((ScrollPane) view).getContent(), container);
        }
        if (view instanceof TabPane) {
            for (Tab tab : ((TabPane) view).getTabs()) {
                if (tab.getContent() != null) {
                    apply(tab.getContent(), container);
                }
            }
        }
        if (view instanceof Parent) {
            for (Node child : ((Parent) view).getChildrenUnmodifiable()) {
                apply(child, container);
            }
        }
    }

    /** A "fixed" panel is one whose min, pref and max width were all set to the same number. */
    private static boolean isFixedWidthPanel(Region r) {
        double pref = r.getPrefWidth();
        return pref > 0 && r.getMinWidth() == pref && r.getMaxWidth() == pref;
    }

    private static void bindWidth(Region panel, Region container) {
        double design = panel.getPrefWidth();
        double ratio = design / DESIGN_WIDTH;
        panel.prefWidthProperty().bind(Bindings.min(design * MAX_GROWTH,
                Bindings.max(design, container.widthProperty().multiply(ratio))));
        panel.maxWidthProperty().bind(panel.prefWidthProperty());
    }

    private static void bindHeight(Region chart, Region container) {
        double design = chart.getPrefHeight();
        if (design <= 0) return;
        double ratio = design / DESIGN_HEIGHT;
        chart.prefHeightProperty().bind(Bindings.min(design * MAX_GROWTH,
                Bindings.max(design, container.heightProperty().multiply(ratio))));
    }
}
