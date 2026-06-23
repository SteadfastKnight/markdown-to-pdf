package com.steadfastknight.md2pdf;

/**
 * Fat-jar / jpackage entry point. Deliberately does NOT extend
 * javafx.application.Application -- launching an Application subclass directly
 * from the classpath triggers JavaFX's "missing runtime components" error, so
 * this thin wrapper bootstraps {@link Main} instead.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        Main.main(args);
    }
}
