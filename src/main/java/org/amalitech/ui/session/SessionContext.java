package org.amalitech.ui.session;

import org.amalitech.models.User;

/**
 * Simple in-memory session holder for the desktop app.
 */
public class SessionContext {
    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static boolean isAuthenticated() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}

