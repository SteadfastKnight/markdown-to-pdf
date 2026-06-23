package com.steadfastknight.md2pdf.ui;

import com.steadfastknight.md2pdf.Booklet;
import com.steadfastknight.md2pdf.FontRegistry;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Print studio" JavaFX front end for the booklet imposer. Same behaviours as
 * the original tkinter GUI -- auto-suggested output name, background generation,
 * open-on-success, friendly locked-file error -- in a typographic shell that
 * uses the app's own bundled book fonts and a live imposition preview.
 */
public class MainApp extends Application {

    private static final Map<String, Integer> PER_OPTIONS = new LinkedHashMap<>();
    static {
        PER_OPTIONS.put("4 — A6 · four per sheet", 4);
        PER_OPTIONS.put("2 — A5 · two per sheet", 2);
        PER_OPTIONS.put("1 — A4 · one per sheet", 1);
    }

    private final TextField mdField = new TextField();
    private final TextField outField = new TextField();
    private final ComboBox<String> fontCombo = new ComboBox<>();
    private final ComboBox<String> perCombo = new ComboBox<>();
    private final Button generateBtn = new Button("Generate");
    private final Label status = new Label("Choose a Markdown file to begin.");

    private final SheetPreview preview = new SheetPreview(236, 330);
    private final Label deskNote = new Label();
    private final Label deskSub = new Label();

    private Stage stage;
    private VBox formPane;
    private String lastSuggest = "";
    private double dragX, dragY;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        loadFonts();

        HBox content = new HBox(buildForm(), buildDesk());
        HBox.setHgrow(content.getChildren().get(0), Priority.ALWAYS);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-frame");
        root.setTop(buildTitleBar());
        root.setCenter(content);

        Scene scene = new Scene(root, 822, 600);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.initStyle(StageStyle.UNDECORATED);
        stage.getIcons().addAll(loadIcons());
        stage.setTitle("MD-to-PDF Booklet");
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();

        updateLayoutInfo();
        playIntro();
    }

    /** Custom title bar (the window is undecorated) -- icon, wordmark, controls. */
    private Region buildTitleBar() {
        ImageView mark = new ImageView(new Image(getClass().getResourceAsStream("/icon/app-32.png")));
        mark.setFitWidth(18);
        mark.setFitHeight(18);
        mark.setPreserveRatio(true);

        Label title = styled(new Label("MD-to-PDF Booklet"), "titlebar-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button min = styled(new Button("–"), "win-btn");
        min.setFocusTraversable(false);
        min.setOnAction(e -> stage.setIconified(true));

        Button close = new Button("✕");
        close.getStyleClass().addAll("win-btn", "win-close");
        close.setFocusTraversable(false);
        close.setOnAction(e -> Platform.exit());

        HBox bar = new HBox(8, mark, title, spacer, min, close);
        bar.getStyleClass().add("titlebar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setMinHeight(34);
        bar.setPrefHeight(34);

        // drag the undecorated window by the title bar
        bar.setOnMousePressed(e -> { dragX = e.getScreenX() - stage.getX(); dragY = e.getScreenY() - stage.getY(); });
        bar.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - dragX); stage.setY(e.getScreenY() - dragY); });
        return bar;
    }

    private List<Image> loadIcons() {
        List<Image> icons = new ArrayList<>();
        for (int s : new int[]{16, 32, 48, 64, 128, 256}) {
            icons.add(new Image(getClass().getResourceAsStream("/icon/app-" + s + ".png")));
        }
        return icons;
    }

    private void loadFonts() {
        for (String f : new String[]{
                "EBGaramond-Regular", "EBGaramond-Bold", "Lora-Regular",
                "Lora-Bold", "SourceSans-Regular", "SourceSans-Bold"}) {
            Font.loadFont(getClass().getResourceAsStream("/fonts/" + f + ".ttf"), 12);
        }
    }

    // ---- left: the form ----------------------------------------------------
    private Region buildForm() {
        Label overline = styled(new Label("PRINT STUDIO"), "overline");
        Label title = styled(new Label("Markdown → Booklet"), "title");
        Region accentRule = new Region();
        accentRule.getStyleClass().add("rule-accent");
        accentRule.setPrefWidth(58);
        accentRule.setMaxWidth(58);
        Label subtitle = styled(new Label(
                "Impose Markdown onto print-ready A4 — fold, cut, and bind."), "subtitle");
        subtitle.setWrapText(true);

        VBox header = new VBox(6, overline, title, accentRule, subtitle);
        header.setPadding(new Insets(0, 0, 8, 0));

        // source file
        Button browseMd = styled(new Button("Browse"), "browse-btn");
        browseMd.setOnAction(e -> pickMarkdown(stageOf(browseMd)));
        mdField.setPromptText("path to your .md file");
        HBox.setHgrow(mdField, Priority.ALWAYS);
        VBox srcGroup = fieldGroup("SOURCE FILE", new HBox(10, mdField, browseMd));

        // typeface
        fontCombo.getItems().addAll(FontRegistry.FONTS.keySet());
        fontCombo.getSelectionModel().selectFirst();
        fontCombo.setMaxWidth(Double.MAX_VALUE);
        VBox fontGroup = fieldGroup("TYPEFACE", fontCombo);

        // layout
        perCombo.getItems().addAll(PER_OPTIONS.keySet());
        perCombo.getSelectionModel().selectFirst();
        perCombo.setMaxWidth(Double.MAX_VALUE);
        perCombo.setOnAction(e -> { updateLayoutInfo(); suggestOut(); });
        VBox perGroup = fieldGroup("LAYOUT", perCombo);

        // save as
        Button browseOut = styled(new Button("Browse"), "browse-btn");
        browseOut.setOnAction(e -> pickOutput(stageOf(browseOut)));
        outField.setPromptText("destination .pdf");
        HBox.setHgrow(outField, Priority.ALWAYS);
        VBox outGroup = fieldGroup("SAVE AS", new HBox(10, outField, browseOut));

        generateBtn.getStyleClass().add("generate-btn");
        generateBtn.setOnAction(e -> doGenerate());
        HBox genRow = new HBox(generateBtn);
        genRow.setPadding(new Insets(6, 0, 0, 0));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region statusRule = new Region();
        statusRule.getStyleClass().add("rule");
        status.getStyleClass().add("status");
        status.setWrapText(true);
        VBox statusBox = new VBox(8, statusRule, status);

        VBox form = new VBox(16, header, srcGroup, fontGroup, perGroup, outGroup,
                genRow, spacer, statusBox);
        form.getStyleClass().add("form-pane");
        form.setPadding(new Insets(34, 42, 28, 46));
        form.setMinWidth(440);
        this.formPane = form;
        return form;
    }

    private VBox fieldGroup(String label, Node control) {
        return new VBox(5, styled(new Label(label), "field-label"), control);
    }

    // ---- right: the preview desk -------------------------------------------
    private Region buildDesk() {
        Label caption = styled(new Label("SHEET PREVIEW"), "desk-caption");
        deskNote.getStyleClass().add("desk-note");
        deskSub.getStyleClass().add("desk-sub");

        VBox notes = new VBox(3, deskNote, deskSub);
        notes.setAlignment(Pos.CENTER);

        VBox desk = new VBox(18, caption, preview, notes);
        desk.setAlignment(Pos.CENTER);
        desk.getStyleClass().add("desk-pane");
        desk.setPadding(new Insets(30));
        desk.setPrefWidth(322);
        desk.setMinWidth(322);
        return desk;
    }

    private void updateLayoutInfo() {
        int per = PER_OPTIONS.get(perCombo.getValue());
        preview.render(per);
        switch (per) {
            case 4 -> { deskNote.setText("4 × A6 per sheet"); deskSub.setText("two cuts · four booklet pages"); }
            case 2 -> { deskNote.setText("2 × A5 per sheet"); deskSub.setText("one cut · two pages, turned 90°"); }
            default -> { deskNote.setText("1 × A4 per sheet"); deskSub.setText("no cuts · full sheet"); }
        }
    }

    // ---- behaviours --------------------------------------------------------
    private void pickMarkdown(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choose Markdown file");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown", "*.md", "*.markdown", "*.txt"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        File f = fc.showOpenDialog(stage);
        if (f != null) {
            mdField.setText(f.getAbsolutePath());
            suggestOut();
            setStatus("Ready.", null);
        }
    }

    private void pickOutput(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Save PDF as");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        String cur = outField.getText();
        if (cur != null && !cur.isBlank()) {
            File c = new File(cur);
            if (c.getParentFile() != null && c.getParentFile().isDirectory()) {
                fc.setInitialDirectory(c.getParentFile());
            }
            fc.setInitialFileName(c.getName());
        } else {
            fc.setInitialFileName("booklet - A6 4up.pdf");
        }
        File f = fc.showSaveDialog(stage);
        if (f != null) outField.setText(f.getAbsolutePath());
    }

    /** Auto-suggest the output name, but never clobber a path the user edited. */
    private void suggestOut() {
        String md = mdField.getText();
        if (md == null || md.isBlank()) return;
        String cur = outField.getText();
        if (cur != null && !cur.isBlank() && !cur.equals(lastSuggest)) return;
        try {
            String text = Files.readString(Path.of(md));
            String title = Booklet.titleFromMd(text, stem(md));
            int per = PER_OPTIONS.get(perCombo.getValue());
            String np = Path.of(md).toAbsolutePath().getParent()
                    .resolve(Booklet.defaultOutName(title, per)).toString();
            outField.setText(np);
            lastSuggest = np;
        } catch (Exception ignored) {
        }
    }

    private void doGenerate() {
        String md = mdField.getText(), out = outField.getText();
        if (md == null || md.isBlank() || !new File(md).exists()) {
            alert("Missing input", "Choose a valid Markdown file.");
            return;
        }
        if (out == null || out.isBlank()) {
            alert("Missing output", "Choose where to save the PDF.");
            return;
        }
        int per = PER_OPTIONS.get(perCombo.getValue());
        String fontKey = fontCombo.getValue();

        generateBtn.setDisable(true);
        setStatus("Generating…", null);

        Task<Booklet.GenResult> task = new Task<>() {
            @Override protected Booklet.GenResult call() throws Exception {
                return Booklet.generate(Path.of(md), fontKey, out, per);
            }
        };
        task.setOnSucceeded(e -> {
            generateBtn.setDisable(false);
            Booklet.GenResult r = task.getValue();
            String msg = String.format(
                    "Done — %d A4 sheet(s), pages %.0f×%.0f mm. Cut at the inner cross.",
                    r.sheets(), r.wMm(), r.hMm());
            setStatus(msg, "status-ok");
            offerToOpen(out, msg);
        });
        task.setOnFailed(e -> {
            generateBtn.setDisable(false);
            Throwable ex = task.getException();
            String msg = (ex instanceof IOException && isLocked(ex))
                    ? "Could not save — is the PDF open in a viewer? Close it and retry."
                    : "Error: " + (ex == null ? "unknown" : ex.getMessage());
            setStatus(msg, "status-error");
            alert("Failed", msg);
        });
        Thread t = new Thread(task, "generate");
        t.setDaemon(true);
        t.start();
    }

    private void offerToOpen(String out, String msg) {
        boolean open = Dialog.confirm(stage, "Done", msg + "\n\nOpen the PDF now?",
                "Open PDF", "Not now");
        if (open) openFile(out);
    }

    private void openFile(String out) {
        File f = new File(out);
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(f);
                return;
            }
        } catch (Exception ignored) {
        }
        getHostServices().showDocument(f.toURI().toString());
    }

    // ---- helpers -----------------------------------------------------------
    private void setStatus(String text, String styleClass) {
        status.setText(text);
        status.getStyleClass().removeAll("status-ok", "status-error");
        if (styleClass != null) status.getStyleClass().add(styleClass);
    }

    private static boolean isLocked(Throwable ex) {
        String m = ex.getMessage();
        if (m == null) return false;
        m = m.toLowerCase();
        return m.contains("another process") || m.contains("access is denied")
                || m.contains("being used");
    }

    private void alert(String title, String body) {
        Dialog.notice(stage, title, body);
    }

    private void playIntro() {
        int i = 0;
        for (Node n : formPane.getChildren()) {
            n.setOpacity(0);
            n.setTranslateY(14);
            FadeTransition fade = new FadeTransition(Duration.millis(360), n);
            fade.setToValue(1);
            TranslateTransition slide = new TranslateTransition(Duration.millis(360), n);
            slide.setToY(0);
            fade.setDelay(Duration.millis(70L * i));
            slide.setDelay(Duration.millis(70L * i));
            fade.play();
            slide.play();
            i++;
        }
    }

    private static <T extends Node> T styled(T node, String cls) {
        node.getStyleClass().add(cls);
        return node;
    }

    private static String stem(String path) {
        String name = Path.of(path).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }

    private static Stage stageOf(Node n) {
        return (Stage) n.getScene().getWindow();
    }
}
