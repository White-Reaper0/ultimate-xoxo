package com.ultimatexox.app.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Design tokens and reusable styled components for the premium dark theme.
 */
final class Theme {

    static final double WINDOW_WIDTH = 480;
    static final double WINDOW_HEIGHT = 720;
    static final double CONTENT_MAX_WIDTH = 360;
    static final double BUTTON_WIDTH = 280;

    private Theme() {}

    static void applyStylesheet(javafx.scene.Scene scene) {
        var url = Theme.class.getResource("/styles/app.css");
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
    }

    static Label displayLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("label-display");
        return label;
    }

    static Label titleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("label-title");
        return label;
    }

    static Label subtitleLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("label-subtitle");
        return label;
    }

    static Label captionLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("label-caption");
        return label;
    }

    static Button primaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-primary");
        btn.setMaxWidth(BUTTON_WIDTH);
        btn.setPrefWidth(BUTTON_WIDTH);
        return btn;
    }

    static Button secondaryButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-secondary");
        btn.setMaxWidth(BUTTON_WIDTH);
        btn.setPrefWidth(BUTTON_WIDTH);
        return btn;
    }

    static Button ghostButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-ghost");
        return btn;
    }

    static Button backButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-back");
        return btn;
    }

    static VBox screenRoot() {
        VBox root = new VBox(24);
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(32, 24, 32, 24));
        root.setFillWidth(true);
        root.setMaxWidth(CONTENT_MAX_WIDTH);
        return root;
    }

    static VBox selectableCard(String title, String description) {
        VBox card = new VBox(6);
        card.getStyleClass().add("card-selectable");
        card.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f0f6fc;");

        Label descLbl = new Label(description);
        descLbl.setWrapText(true);
        descLbl.getStyleClass().add("label-subtitle");

        card.getChildren().addAll(titleLbl, descLbl);
        return card;
    }

    static void setCardSelected(VBox card, boolean selected) {
        if (selected) {
            if (!card.getStyleClass().contains("card-selected")) {
                card.getStyleClass().add("card-selected");
            }
        } else {
            card.getStyleClass().remove("card-selected");
        }
    }

    static HBox topBar(Button back, String heading) {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMaxWidth(Double.MAX_VALUE);

        Label headingLbl = titleLabel(heading);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(back, headingLbl, spacer);
        return bar;
    }

    static VBox statBlock(String value, String label) {
        VBox block = new VBox(4);
        block.setAlignment(Pos.CENTER);

        Label val = new Label(value);
        val.getStyleClass().add("stat-value");

        Label lbl = new Label(label);
        lbl.getStyleClass().add("stat-label");

        block.getChildren().addAll(val, lbl);
        return block;
    }
}
