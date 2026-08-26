module com.ultimatexox {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;

    opens com.ultimatexox.app.ui to javafx.graphics;

    exports com.ultimatexox.app;
    exports com.ultimatexox.app.ui;
    exports com.ultimatexox.game;
    exports com.ultimatexox.ai;
    exports com.ultimatexox.stats;
}
