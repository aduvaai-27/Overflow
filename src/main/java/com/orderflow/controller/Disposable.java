package com.orderflow.controller;

/**
 * Implemented by sub-view controllers that start background resources
 * (e.g. DashboardController starts a monitoring thread) which must be
 * stopped when the user navigates to a different screen.
 */
public interface Disposable {
    void dispose();
}
