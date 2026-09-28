package com.orderflow.service;

/**
 * Topic: Advanced OOP (interface).
 * Contract for any service that owns background threads and must be
 * stoppable when the user leaves the screen or closes the app.
 */
public interface BackgroundService {
    void stop();
}
