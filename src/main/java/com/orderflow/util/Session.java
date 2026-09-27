package com.orderflow.util;

import com.orderflow.model.User;

/** Holds the currently logged-in user for the lifetime of the application. */
public class Session {
    private static User currentUser;

    private Session() { }

    public static void login(User user) { currentUser = user; }

    public static User getCurrentUser() { return currentUser; }

    public static void logout() { currentUser = null; }
}
