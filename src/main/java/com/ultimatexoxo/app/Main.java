package com.ultimatexox.app;

import com.ultimatexox.app.ui.XoxoApp;

/**
 * Minimal launcher for the JavaFX desktop client.
 * The class intentionally stays thin so the UI entry point remains easy to wrap
 * in packaged desktop or mobile builds without changing game logic.
 */
public class Main {
    public static void main(String[] args) {
        XoxoApp.main(args);
    }
}
