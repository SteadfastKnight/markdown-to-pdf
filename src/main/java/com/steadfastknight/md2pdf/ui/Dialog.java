package com.steadfastknight.md2pdf.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * A small, theme-matched modal dialog used instead of JavaFX's default Alert
 * (whose native chrome + blue "?" clashed with the print-studio look). Paper
 * card, ink/EB-Garamond heading, custom buttons; centred on its owner.
 */
public final class Dialog {
    private Dialog() {}

    /** Two-button confirm. Returns true if the primary action was chosen. */
    public static boolean confirm(Window owner, String title, String message,
                                  String primaryText, String secondaryText) {
        return show(owner, title, message, primaryText, secondaryText, false);
    }

    /** Single-button notice (used for errors). */
    public static void notice(Window owner, String title, String message) {
        show(owner, title, message, "OK", null, true);
    }

    private static boolean show(Window owner, String title, String message,
                                String primaryText, String secondaryText, boolean error) {
        Stage stage = new Stage(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) stage.initOwner(owner);

        final boolean[] result = {false};

        Label heading = new Label(title);
        heading.getStyleClass().add("dialog-title");
        Region rule = new Region();
        rule.getStyleClass().add("rule-accent");
        rule.setPrefWidth(46);
        rule.setMaxWidth(46);
        Label body = new Label(message);
        body.getStyleClass().add("dialog-msg");
        body.setWrapText(true);
        body.setMaxWidth(360);

        Button primary = new Button(primaryText);
        primary.getStyleClass().add("dialog-btn-primary");
        primary.setDefaultButton(true);
        primary.setOnAction(e -> { result[0] = true; stage.close(); });

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        if (secondaryText != null) {
            Button secondary = new Button(secondaryText);
            secondary.getStyleClass().add("dialog-btn-secondary");
            secondary.setCancelButton(true);
            secondary.setOnAction(e -> { result[0] = false; stage.close(); });
            buttons.getChildren().add(secondary);
        }
        buttons.getChildren().add(primary);

        VBox card = new VBox(12, heading, rule, body, buttons);
        card.getStyleClass().addAll("dialog-card", error ? "dialog-error" : "dialog-ok");
        card.setPadding(new Insets(24, 26, 22, 26));
        VBox.setMargin(buttons, new Insets(8, 0, 0, 0));

        Scene scene = new Scene(card, Color.TRANSPARENT);
        scene.getStylesheets().add(Dialog.class.getResource("/css/app.css").toExternalForm());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) stage.close(); });
        stage.setScene(scene);

        // centre on owner once sized
        stage.setOnShown(e -> {
            if (owner != null) {
                stage.setX(owner.getX() + (owner.getWidth() - card.getWidth()) / 2);
                stage.setY(owner.getY() + (owner.getHeight() - card.getHeight()) / 2);
            }
        });
        stage.showAndWait();
        return result[0];
    }
}
