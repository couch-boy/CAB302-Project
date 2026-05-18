package com.example.cab302project;

import javafx.scene.Scene;

/**
 * Utility class for applying the application's light/dark theme.
 *
 * The theme is implemented as a CSS class ("dark-mode") added to or removed
 * from the root node of the current scene.  All dark-mode overrides live in
 * styles.css under the ".dark-mode" parent selector so no stylesheet swap is
 * needed — a single class toggle is enough.
 *
 * Call {@link #apply(Scene)} after every scene switch (and after any in-screen
 * toggle) to keep the theme in sync with the active user's preference.
 */
public class ThemeManager {

    /** The CSS class applied to the scene root when dark mode is active. */
    public static final String DARK_CLASS = "dark-mode";

    /**
     * Reads the current user's dark-mode preference from {@link UserSession}
     * and applies or removes the {@code dark-mode} CSS class on the scene's
     * root node accordingly.
     *
     * @param scene The active JavaFX {@link Scene}. Does nothing if null.
     */
    public static void apply(Scene scene) {
        if (scene == null || scene.getRoot() == null) return;

        var styleClasses = scene.getRoot().getStyleClass();
        boolean wantDark = UserSession.isDarkMode();

        if (wantDark && !styleClasses.contains(DARK_CLASS)) {
            styleClasses.add(DARK_CLASS);
        } else if (!wantDark) {
            styleClasses.remove(DARK_CLASS);
        }
    }
}