package com.orderflow.util;

import com.orderflow.model.User;

public class Session {
    private static User currentUser;

    private Session() { }

    public static void login(User user) { currentUser = user; }

    public static User getCurrentUser() { return currentUser; }

    public static void logout() { currentUser = null; }
}
