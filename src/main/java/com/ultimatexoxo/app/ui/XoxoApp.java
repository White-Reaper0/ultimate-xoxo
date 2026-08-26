package com.ultimatexox.app.ui;

import com.ultimatexox.ai.Difficulty;
import com.ultimatexox.ai.MinimaxAI;
import com.ultimatexox.game.GameController;
import com.ultimatexox.game.UltimateBoard;
import com.ultimatexox.stats.Statistics;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.Random;

/**
 * JavaFX front end for Ultimate XOX (Version 1).
 *
 * Flow: Splash → Main Menu → Mode Select → (Difficulty + First Move) → Game.
 *
 * All game state lives in {@link GameController}; this class only builds
 * screens, forwards clicks, and schedules AI work on background threads
 * (results are applied back on the JavaFX thread via Platform.runLater).
 */
public class XoxoApp extends Application {

    // ── Game state ──
    private GameController controller;
    private MinimaxAI aiEngine;
    private final Statistics statistics = new Statistics();

    private Difficulty difficulty = Difficulty.MEDIUM;
    private FirstMoveChoice firstMoveChoice = FirstMoveChoice.HUMAN;
    /** Marks so the human can identify their own symbol in the UI. */
    private char humanMark = 'X';
    private char aiMark = 'O';

    private boolean aiThinking = false;
    /**
     * Incremented whenever the match is left/restarted/undone while an AI
     * search may still be running in the background. A finished search whose
     * generation no longer matches is discarded instead of applied. This is
     * how we guarantee that leaving a game never lets a stale AI move through.
     */
    private int searchGeneration = 0;

    // Session scoreboard (this app run); persistent totals live in Statistics.
    private int sessionHumanWins = 0;
    private int sessionAiWins = 0;
    private int sessionDraws = 0;

    // ── UI widgets ──
    private final Button[][] cellButtons = new Button[9][9];
    private final Label[][] cellMarks = new Label[9][9];
    private final Button[] metaBadges = new Button[9];          // small-board owner badge
    private final Label statusLabel = new Label();
    private final Label hintLabel = new Label();
    private final Label scoreLabel = new Label();
    private final HBox playerXPanel = new HBox();
    private final HBox playerOPanel = new HBox();
    private ProgressIndicator aiSpinner;
    private StackPane gameRoot;
    private Label modeBadge;
    // Direct references so panel text can be updated without CSS lookups.
    private Label panelXName = new Label(), panelORole = new Label();
    private Label panelOName = new Label(), panelXRole = new Label();

    // ── Screens ──
    private enum ScreenMode { HUMAN_VS_AI, LOCAL_PVP, AI_VS_AI }
    private enum FirstMoveChoice { HUMAN, AI, RANDOM }

    private ScreenMode selectedMode = ScreenMode.HUMAN_VS_AI;

    private Stage stage;
    private Scene splashScene, menuScene, modeScene, setupScene, loadingScene, gameScene, statsScene, aboutScene;
    private VBox modeCardVsAi, modeCardPvp, modeCardAiVsAi;
    private VBox[] difficultyCards;
    private VBox firstHumanCard, firstAiCard, firstRandomCard;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("Ultimate XOX");
        // Sensible desktop minimum: the full 9x9 board (~540px) plus player
        // panels and control row fit comfortably at this size.
        stage.setMinWidth(760);
        stage.setMinHeight(640);
        stage.setWidth(1000);
        stage.setHeight(700);

        splashScene = buildSplashScene();
        menuScene = buildMenuScene();
        modeScene = buildModeSelectScene();
        setupScene = buildSetupScene();
        loadingScene = buildLoadingScene();
        gameScene = buildGameScene();
        statsScene = buildStatsScene();
        aboutScene = buildAboutScene();

        for (Scene s : List.of(splashScene, menuScene, modeScene, setupScene,
                loadingScene, gameScene, statsScene, aboutScene)) {
            Theme.applyStylesheet(s);
        }

        stage.setScene(splashScene);
        stage.show();

        PauseTransition splashDelay = new PauseTransition(Duration.seconds(1.6));
        splashDelay.setOnFinished(e -> transitionTo(menuScene));
        splashDelay.play();

        // TEMPORARY DIAGNOSTIC: jump straight into a Human-vs-AI game so the
        // rendering pipeline can be screenshot-tested without menu navigation.
        if (DEBUG_CELLS) {
            selectedMode = ScreenMode.HUMAN_VS_AI;
            firstMoveChoice = FirstMoveChoice.HUMAN;
            difficulty = Difficulty.MEDIUM;
            PauseTransition auto = new PauseTransition(Duration.seconds(2.2));
            auto.setOnFinished(e -> launchGame());
            auto.play();
        }
    }

    // ── Navigation helper ───────────────────────────────────────────────

    private void transitionTo(Scene next) {
        // Preserve the user's window geometry across scene changes. Scenes are
        // created WITHOUT explicit dimensions, so setScene() alone never resizes
        // the stage; this guard is belt-and-braces for maximized state quirks.
        boolean wasMaximized = stage.isMaximized();
        double width = stage.getWidth();
        double height = stage.getHeight();

        FadeTransition fade = new FadeTransition(Duration.millis(180),
                stage.getScene() != null ? stage.getScene().getRoot() : next.getRoot());
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            stage.setScene(next);
            // Restore exactly what the user had: size and/or maximized state.
            if (wasMaximized != stage.isMaximized()) {
                stage.setMaximized(wasMaximized);
            }
            if (!wasMaximized && width > 0
                    && (Math.abs(stage.getWidth() - width) > 1 || Math.abs(stage.getHeight() - height) > 1)) {
                stage.setWidth(width);
                stage.setHeight(height);
            }
            next.getRoot().setOpacity(1);
            fadeIn(next.getRoot());
        });
        fade.play();
    }

    private void fadeIn(javafx.scene.Node node) {
        node.setOpacity(0);
        FadeTransition in = new FadeTransition(Duration.millis(220), node);
        in.setFromValue(0);
        in.setToValue(1);
        in.play();
    }

    // ── Splash ──────────────────────────────────────────────────────────

    private Scene buildSplashScene() {
        Label title = Theme.displayLabel("ULTIMATE XOX");
        Label tagline = Theme.subtitleLabel("Ultimate Tic-Tac-Toe");

        Region accent = new Region();
        accent.getStyleClass().add("splash-accent-line");

        VBox branding = new VBox(12, title, accent, tagline);
        branding.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(36, 36);
        progress.getStyleClass().add("splash-progress");
        StackPane.setMargin(progress, new Insets(0, 0, 48, 0));

        StackPane root = new StackPane(branding, progress);
        root.getStyleClass().add("splash-root");
        StackPane.setAlignment(progress, Pos.BOTTOM_CENTER);
        return new Scene(root);
    }

    // ── Main menu ───────────────────────────────────────────────────────

    private Scene buildMenuScene() {
        VBox content = Theme.screenRoot();
        content.setAlignment(Pos.CENTER);

        Label logo = Theme.displayLabel("ULTIMATE XOX");
        Label tagline = Theme.subtitleLabel("Nine boards. One winner.");

        Button play = Theme.primaryButton("New Game");
        play.setOnAction(e -> {
            selectMode(selectedMode); // re-sync card highlight
            transitionTo(modeScene);
        });

        Button stats = Theme.secondaryButton("Statistics");
        stats.setOnAction(e -> {
            refreshStatsLabels();
            transitionTo(statsScene);
        });

        Button about = Theme.secondaryButton("About");
        about.setOnAction(e -> transitionTo(aboutScene));

        Button exit = Theme.ghostButton("Exit");
        exit.setOnAction(e -> stage.close());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox buttons = new VBox(12, play, stats, about, exit);
        buttons.setAlignment(Pos.CENTER);

        content.getChildren().addAll(logo, tagline, spacer, buttons);
        return new Scene(wrapCentered(content));
    }

    // ── Mode selection ──────────────────────────────────────────────────

    private Scene buildModeSelectScene() {
        VBox content = Theme.screenRoot();

        Button back = Theme.backButton("<- Back");
        back.setOnAction(e -> transitionTo(menuScene));
        content.getChildren().add(Theme.topBar(back, "Select Mode"));

        modeCardVsAi = Theme.selectableCard("Human vs Computer",
                "Play against the local Minimax AI.");
        modeCardPvp = Theme.selectableCard("Human vs Human",
                "Two players share this device.");
        modeCardAiVsAi = Theme.selectableCard("AI vs AI",
                "Watch two Minimax engines play each other.");

        modeCardVsAi.setOnMouseClicked(e -> selectMode(ScreenMode.HUMAN_VS_AI));
        modeCardPvp.setOnMouseClicked(e -> selectMode(ScreenMode.LOCAL_PVP));
        modeCardAiVsAi.setOnMouseClicked(e -> selectMode(ScreenMode.AI_VS_AI));
        selectMode(ScreenMode.HUMAN_VS_AI);

        Button continueBtn = Theme.primaryButton("Continue");
        continueBtn.setOnAction(e -> {
            if (selectedMode == ScreenMode.HUMAN_VS_AI) {
                transitionTo(setupScene);           // needs difficulty + who starts
            } else {
                launchGame();                        // X opens; nothing else to ask
            }
        });

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().addAll(new VBox(12, modeCardVsAi, modeCardPvp, modeCardAiVsAi), spacer, continueBtn);
        return new Scene(wrapCentered(content));
    }

    private void selectMode(ScreenMode mode) {
        selectedMode = mode;
        Theme.setCardSelected(modeCardVsAi, mode == ScreenMode.HUMAN_VS_AI);
        Theme.setCardSelected(modeCardPvp, mode == ScreenMode.LOCAL_PVP);
        Theme.setCardSelected(modeCardAiVsAi, mode == ScreenMode.AI_VS_AI);
    }

    // ── Setup: difficulty + first move (Human vs Computer only) ─────────

    private Scene buildSetupScene() {
        VBox content = Theme.screenRoot();

        Button back = Theme.backButton("<- Back");
        back.setOnAction(e -> transitionTo(modeScene));
        content.getChildren().add(Theme.topBar(back, "New Game"));

        Label diffHeader = sectionHeader("Difficulty");
        difficultyCards = new VBox[Difficulty.values().length];
        VBox diffBox = new VBox(10);
        for (int i = 0; i < Difficulty.values().length; i++) {
            Difficulty d = Difficulty.values()[i];
            VBox card = Theme.selectableCard(d.toString(), d.getDescription());
            Difficulty level = d;
            card.setOnMouseClicked(e -> selectDifficulty(level));
            difficultyCards[i] = card;
            diffBox.getChildren().add(card);
        }

        Label firstHeader = sectionHeader("Who plays first?");
        firstHumanCard = Theme.selectableCard("Human First", "You open the game as X.");
        firstAiCard = Theme.selectableCard("AI First", "The computer opens as X; you play O.");
        firstRandomCard = Theme.selectableCard("Random", "A coin flip decides who opens.");

        firstHumanCard.setOnMouseClicked(e -> selectFirstMove(FirstMoveChoice.HUMAN));
        firstAiCard.setOnMouseClicked(e -> selectFirstMove(FirstMoveChoice.AI));
        firstRandomCard.setOnMouseClicked(e -> selectFirstMove(FirstMoveChoice.RANDOM));

        Button start = Theme.primaryButton("Start Game");
        start.setOnAction(e -> launchGame());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().addAll(diffHeader, diffBox, firstHeader,
                new VBox(10, firstHumanCard, firstAiCard, firstRandomCard),
                spacer, start);
        return new Scene(wrapCentered(content));
    }

    private void selectDifficulty(Difficulty d) {
        difficulty = d;
        for (int i = 0; i < Difficulty.values().length; i++) {
            Theme.setCardSelected(difficultyCards[i], Difficulty.values()[i] == d);
        }
    }

    private void selectFirstMove(FirstMoveChoice c) {
        firstMoveChoice = c;
        Theme.setCardSelected(firstHumanCard, c == FirstMoveChoice.HUMAN);
        Theme.setCardSelected(firstAiCard, c == FirstMoveChoice.AI);
        Theme.setCardSelected(firstRandomCard, c == FirstMoveChoice.RANDOM);
    }

    private Label sectionHeader(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f0f6fc;");
        return lbl;
    }

    // ── Loading ─────────────────────────────────────────────────────────

    private Scene buildLoadingScene() {
        VBox content = Theme.screenRoot();
        content.setAlignment(Pos.CENTER);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(42, 42);

        content.getChildren().addAll(
                Theme.displayLabel("Starting game..."),
                progress,
                Theme.subtitleLabel("Setting up the board"));
        return new Scene(wrapCentered(content));
    }

    // ── Game screen ─────────────────────────────────────────────────────

    private Scene buildGameScene() {
        gameRoot = new StackPane();
        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(16, 20, 16, 20));

        Button menuBtn = Theme.backButton("<- Menu");
        menuBtn.setOnAction(e -> {
            searchGeneration++;                 // invalidate any running AI search
            transitionTo(menuScene);
        });
        modeBadge = new Label();
        modeBadge.getStyleClass().add("badge");
        HBox topBar = new HBox(12, menuBtn, spacer(), modeBadge);
        topBar.setAlignment(Pos.CENTER_LEFT);
        layout.setTop(topBar);

        GridPane grid = buildBoardGrid();
        layout.setCenter(grid);
        BorderPane.setAlignment(grid, Pos.CENTER);

        configurePanel(playerXPanel, "X", panelXName, panelXRole);
        configurePanel(playerOPanel, "O", panelOName, panelORole);
        HBox playerBar = new HBox(12, playerXPanel, playerOPanel);
        playerBar.setAlignment(Pos.CENTER);
        HBox.setHgrow(playerXPanel, Priority.ALWAYS);
        HBox.setHgrow(playerOPanel, Priority.ALWAYS);
        playerXPanel.setMaxWidth(Double.MAX_VALUE);
        playerOPanel.setMaxWidth(Double.MAX_VALUE);

        statusLabel.getStyleClass().add("label-status");
        statusLabel.setAlignment(Pos.CENTER);
        statusLabel.setWrapText(true);

        hintLabel.getStyleClass().add("label-caption");
        hintLabel.setAlignment(Pos.CENTER);
        hintLabel.setWrapText(true);
        hintLabel.setStyle("-fx-text-fill: #10b981;");

        scoreLabel.getStyleClass().add("label-subtitle");
        scoreLabel.setAlignment(Pos.CENTER);

        Button undoBtn = Theme.secondaryButton("Undo");
        undoBtn.setOnAction(e -> performUndo());

        Button hintBtn = Theme.secondaryButton("Hint");
        hintBtn.setOnAction(e -> requestHint());

        Button restartBtn = Theme.secondaryButton("Restart");
        restartBtn.setOnAction(e -> restartMatch());

        HBox controls = new HBox(10, undoBtn, hintBtn, restartBtn);
        controls.setAlignment(Pos.CENTER);

        VBox bottom = new VBox(8, playerBar, statusLabel, hintLabel, scoreLabel, controls);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(8, 0, 0, 0));
        layout.setBottom(bottom);

        gameRoot.getChildren().add(layout);
        return new Scene(gameRoot);
    }

    private Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private GridPane buildBoardGrid() {
        GridPane metaGrid = new GridPane();
        metaGrid.getStyleClass().add("meta-board-grid");
        metaGrid.setHgap(10);
        metaGrid.setVgap(10);
        metaGrid.setAlignment(Pos.CENTER);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int boardIndex = row * 3 + col;
                metaGrid.add(createSmallBoard(boardIndex), col, row);
            }
        }
        return metaGrid;
    }

    private StackPane createSmallBoard(int boardIndex) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("small-board-grid");
        grid.setHgap(2);
        grid.setVgap(2);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int cellIndex = row * 3 + col;

                // Button = input/background layer.
                Button btn = new Button();
                btn.getStyleClass().add("cell-button");
                btn.setText("");
                btn.setGraphic(null);

                // Label = visual X/O layer.
                Label mark = new Label();
                mark.getStyleClass().add("cell-mark");
                mark.setMouseTransparent(true);
                mark.setText("");

                StackPane cell = new StackPane(btn, mark);
                cell.setAlignment(Pos.CENTER);

                int b = boardIndex;
                int c = cellIndex;

                btn.setOnAction(e -> onCellClicked(b, c));

                cellButtons[boardIndex][cellIndex] = btn;
                cellMarks[boardIndex][cellIndex] = mark;

                grid.add(cell, col, row);
            }
        }

        // Badge in the corner shows which player owns small board b.
        Button badge = new Button();
        badge.getStyleClass().add("meta-board-button");
        badge.setDisable(true);
        badge.setPrefSize(22, 22);
        metaBadges[boardIndex] = badge;

        StackPane wrapper = new StackPane(grid, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(2, 2, 0, 0));

        return wrapper;
    }

    // ── Starting / restarting ───────────────────────────────────────────

    private void launchGame() {
        resolveMarks();
        GameController.Mode mode = switch (selectedMode) {
            case HUMAN_VS_AI -> GameController.Mode.HUMAN_VS_AI;
            case LOCAL_PVP -> GameController.Mode.HUMAN_VS_HUMAN;
            case AI_VS_AI -> GameController.Mode.AI_VS_AI;
        };
        // Fresh controller + fresh engine guarantees no leftover state from a
        // previous match and no AI computation against an uninitialised board.
        controller = new GameController(mode, humanMark);
        aiEngine = new MinimaxAI(difficulty);
        controller.startNewGame();
        searchGeneration++;
        sessionHumanWins = sessionAiWins = sessionDraws = 0;

        updateModeBadge();
        updatePlayerPanels();
        clearHint();
        refreshBoard();
        updateScoreLabel();
        statusLabel.setText(openingStatusText());

        transitionTo(loadingScene);
        PauseTransition briefPause = new PauseTransition(Duration.millis(600));
        briefPause.setOnFinished(e -> {
            transitionTo(gameScene);
            // If the AI opens (AI First, or AI vs AI), schedule its move now that
            // the board exists — never before initialisation.
            if (controller.isAiTurn()) {
                Platform.runLater(this::triggerAiMove);
            }
            // TEMPORARY DIAGNOSTIC: simulate the opening human click through the
            // real input->controller->render pipeline, then remove.
            if (DEBUG_CELLS && controller.getMode() == GameController.Mode.HUMAN_VS_AI
                    && controller.isHumanTurn()) {
                PauseTransition demo = new PauseTransition(Duration.millis(1200));
                demo.setOnFinished(ev -> {
                    System.out.println("DEMO: playing (4,4)");
                    onCellClicked(4, 4);
                });
                demo.play();
            }
        });
        briefPause.play();
    }

    /** Decides which mark the human plays based on the first-move choice. */
    private void resolveMarks() {
        switch (firstMoveChoice) {
            case HUMAN -> { humanMark = 'X'; aiMark = 'O'; }
            case AI -> { humanMark = 'O'; aiMark = 'X'; }
            case RANDOM -> {
                if (new Random().nextBoolean()) { humanMark = 'X'; aiMark = 'O'; }
                else { humanMark = 'O'; aiMark = 'X'; }
            }
        }
    }

    private void restartMatch() {
        if (controller == null) return;
        searchGeneration++;          // discard any in-flight AI result
        aiThinking = false;
        hideSpinner();
        controller.startNewGame();   // board, turn, forced board, history: all reset
        clearHint();
        refreshBoard();
        updateScoreLabel();
        statusLabel.setText(openingStatusText());
        if (controller.isAiTurn()) {
            triggerAiMove();
        }
    }

    private String openingStatusText() {
        if (controller.getMode() == GameController.Mode.AI_VS_AI) return "AI vs AI in progress...";
        return turnStatusText();
    }

    private String turnStatusText() {
        if (controller.isGameOver()) return "";
        String where = playableBoardText(controller.getBoard());
        if (controller.getMode() == GameController.Mode.HUMAN_VS_AI) {
            return (controller.getBoard().getCurrentPlayer() == humanMark)
                    ? "Your move (" + where + ")" : "Computer is thinking...";
        }
        return "Player " + controller.getBoard().getCurrentPlayer() + "'s move (" + where + ")";
    }

    /** Human-readable description of which small boards may be played. */
    private String playableBoardText(UltimateBoard board) {
        int next = board.getNextBoard();
        if (next == -1) return "any open board";
        return "play in board " + (next + 1);
    }

    // ── Player interaction ──────────────────────────────────────────────

    private void onCellClicked(int boardIndex, int cellIndex) {
        if (controller == null || controller.isGameOver()
                || aiThinking || !controller.isHumanTurn()) {
            return;
        }
        if (!controller.applyMove(boardIndex, cellIndex)) return; // illegal: ignore

        clearHint();
        renderMove(new UltimateBoard.Move(boardIndex, cellIndex));
        if (!checkGameEnd() && controller.isAiTurn()) {
            triggerAiMove();
        }
    }

    private void performUndo() {
        if (controller == null || aiThinking || !controller.canUndo()) return;
        searchGeneration++;              // a pending AI reply becomes stale
        controller.undo();
        clearHint();
        refreshBoard();
        statusLabel.setText(turnStatusText());
        updateActivePlayerHighlight();
    }

    // ── AI scheduling ───────────────────────────────────────────────────

    private void triggerAiMove() {
        if (controller == null || !controller.isAiTurn() || aiThinking) return;

        aiThinking = true;
        setBoardEnabled(false);
        showSpinner();
        statusLabel.setText(controller.getMode() == GameController.Mode.AI_VS_AI
                ? "AI " + controller.getBoard().getCurrentPlayer() + " is thinking..."
                : "Computer is thinking...");

        final int generation = searchGeneration;
        final UltimateBoard snapshot = controller.getBoard().copy();

        Thread worker = new Thread(() -> {
            UltimateBoard.Move chosen = aiEngine.chooseMove(snapshot);
            Platform.runLater(() -> {
                if (generation != searchGeneration) {
                    // The match was restarted/left while we were thinking: drop result.
                    return;
                }
                aiThinking = false;
                hideSpinner();
                if (chosen != null && controller.applyMove(chosen.board(), chosen.cell())) {
                    renderMove(chosen);          // same rendering path as human moves
                    if (!checkGameEnd() && controller.isAiTurn()) {
                        triggerAiMove();     // AI vs AI: other engine replies
                    }
                } else if (!controller.isGameOver()) {
                    setBoardEnabled(true);
                    statusLabel.setText(turnStatusText());
                }
            });
        }, "minimax-worker");
        worker.setDaemon(true);
        worker.start();
    }

    // ── Hint (basic move analysis) ──────────────────────────────────────

    private boolean hintRunning = false;

    private void requestHint() {
        if (controller == null || hintRunning || aiThinking
                || controller.isGameOver() || !controller.isHumanTurn()) {
            return;
        }
        hintRunning = true;
        hintLabel.setText("Analysing...");
        final UltimateBoard snapshot = controller.getBoard().copy();
        // A dedicated engine at fixed strength keeps hints consistent and fast.
        final MinimaxAI analyst = new MinimaxAI(Difficulty.HARD);

        Thread worker = new Thread(() -> {
            MinimaxAI.Analysis analysis = analyst.analyze(snapshot);
            Platform.runLater(() -> {
                hintRunning = false;
                if (analysis == null || analysis.bestMove() == null) {
                    hintLabel.setText("");
                    return;
                }
                paintHint(analysis.bestMove(),
                        String.format("Hint: board %d, cell %d%s",
                                analysis.bestMove().board() + 1,
                                analysis.bestMove().cell() + 1,
                                formatEval(analysis.scoreCentipawns())));
            });
        }, "hint-worker");
        worker.setDaemon(true);
        worker.start();
    }

    private String formatEval(int centipawns) {
        double pawns = centipawns / 100.0;
        return String.format("  (%+.1f)", pawns);
    }

    private void paintHint(UltimateBoard.Move move, String text) {
        clearHint();
        Button target = cellButtons[move.board()][move.cell()];
        if (target != null) {
            target.getStyleClass().add("cell-hint");
        }
        hintLabel.setText(text);
    }

    private void clearHint() {
        for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
                Button btn = cellButtons[b][c];
                if (btn != null) btn.getStyleClass().remove("cell-hint");
            }
        }
        hintLabel.setText("");
    }

    // ── Board painting ──────────────────────────────────────────────────
    //
    // COORDINATE CONVENTIONS (the UI and the engine MUST agree on these):
    //
    //   board index b : 0..8   -> meta row = b / 3, meta col = b % 3
    //   cell index c  : 0..8   -> row = c / 3, col = c % 3
    //
    // cellButtons[b][c] is created with exactly this mapping in
    // createSmallBoard(), so a Move(b, c) from the engine ALWAYS locates its
    // JavaFX node as cellButtons[b][c]. Global 0..80 indexes exist only inside
    // UltimateBoard.Move helpers and are never used to look up nodes here.
    //
    // refreshBoard() is THE single rendering mechanism: it repaints every cell
    // strictly from the authoritative UltimateBoard. Human moves (onCellClicked),
    // AI moves (triggerAiMove), undo, restart and hints all funnel through it,
    // so MOVE STATE == VISUAL STATE by construction.

    /** Temporary diagnosis switch — must be false in normal builds. */
    private static final boolean DEBUG_CELLS = false;

    /** Dumps live visual properties of one painted cell to stdout. */
    private void debugCell(String tag, Button btn, int b, int c) {
        if (!DEBUG_CELLS || btn.getText().isEmpty()) return;
        // Deep-dive: inspect the skin's internal Text node after layout/CSS.
        Platform.runLater(() -> {
            javafx.scene.text.Text t = (javafx.scene.text.Text) btn.lookup(".text");
            var bounds = btn.localToScreen(btn.getBoundsInLocal());
            System.out.println("TEXT NODE board=" + b + " cell=" + c
                    + " btnText='" + btn.getText() + "'"
                    + " textNode=" + (t == null ? "NULL" :
                        "'" + t.getText() + "' fill=" + t.getFill()
                        + " font=" + t.getFont().getSize() + "/" + t.getFont().getFamily()
                        + " visible=" + t.isVisible()
                        + " opacity=" + t.getOpacity()
                        + " bounds=" + t.getBoundsInParent())
                    + " btnBounds=" + (bounds == null ? "?" :
                        Math.round(bounds.getWidth()) + "x" + Math.round(bounds.getHeight())
                        + "@" + Math.round(bounds.getCenterX()) + "," + Math.round(bounds.getCenterY()))
                    + " btnFont=" + btn.getFont().getSize() + "/" + btn.getFont().getFamily()
                    + " btnTextFill=" + btn.getTextFill());
        });
    }

    /** Called right after a successful move; renders that move via the one true path. */
    private void renderMove(UltimateBoard.Move move) {
        refreshBoard();
    }

    private void refreshBoard() {
        UltimateBoard board = controller.getBoard();

        for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
                paintCell(board, b, c);
            }
            paintMetaBadge(board, b);
        }

        highlightLastMove(board);
        updatePlayableHighlight(board);
        updateActivePlayerHighlight();
        if (!controller.isGameOver()) statusLabel.setText(turnStatusText());
    }

    /** Makes the JavaFX button at (b, c) display exactly what the board stores. */
    private void paintCell(UltimateBoard board, int b, int c) {
        char piece = board.getSmallBoardCell(b, c);
        Button btn = cellButtons[b][c];
        Label mark = cellMarks[b][c];

        // The Button is only the input/background layer.
        // Never use Button.setText() for board marks.

        btn.setText("");

        mark.getStyleClass().removeAll(
                "cell-mark-x",
                "cell-mark-o"
        );

        if (piece == 'X') {
            mark.setText("X");
            mark.getStyleClass().add("cell-mark-x");
        } else if (piece == 'O') {
            mark.setText("O");
            mark.getStyleClass().add("cell-mark-o");
        } 
        else {
            mark.setText("");
        }
    }
    /** Corner badge showing which player owns small board b ('=' = drawn). */
    private void paintMetaBadge(UltimateBoard board, int b) {
        Button badge = metaBadges[b];
        badge.getStyleClass().removeAll("meta-x", "meta-o", "meta-draw");
        char owner = board.getMetaCell(b);
        if (owner == 'D') {
            badge.setText("=");
            badge.getStyleClass().add("meta-draw");
            badge.setStyle("-fx-text-fill: #8b949e; -fx-font-weight: bold;");
        } else if (owner != UltimateBoard.EMPTY) {
            badge.setText(String.valueOf(owner));
            badge.getStyleClass().add(owner == 'X' ? "meta-x" : "meta-o");
            badge.setStyle("-fx-text-fill: " + (owner == 'X' ? "#58a6ff" : "#f0b429")
                    + "; -fx-font-weight: bold;");
        } else {
            badge.setText("");
            badge.setStyle(null);
        }
    }

    private void highlightLastMove(UltimateBoard board) {
        UltimateBoard.Move last = board.getLastMove();
        if (last != null) {
            cellButtons[last.board()][last.cell()].getStyleClass().add("cell-last-move");
        }
    }

    /** Marks the small board(s) the current player must play in. */
    private void updatePlayableHighlight(UltimateBoard board) {
        List<Integer> legalBoards = board.getLegalBoards();
        boolean interactive = !board.isGameOver() && !aiThinking && controller.isHumanTurn();
        for (int b = 0; b < 9; b++) {
            boolean legalHere = legalBoards.contains(b);
            for (int c = 0; c < 9; c++) {
                Button btn = cellButtons[b][c];
                boolean emptyCell = board.getSmallBoardCell(b, c) == UltimateBoard.EMPTY;
                boolean clickable = interactive && legalHere && emptyCell;
                btn.setDisable(false);
                btn.setMouseTransparent(!clickable);
                if (legalHere && emptyCell) btn.getStyleClass().add("cell-playable");
                else btn.getStyleClass().remove("cell-playable");
            }
        }
    }

    private void setBoardEnabled(boolean enabled) {
        UltimateBoard board = controller.getBoard();
        List<Integer> legalBoards = board.getLegalBoards();
        for (int b = 0; b < 9; b++) {
            boolean legalHere = legalBoards.contains(b);
            for (int c = 0; c < 9; c++) {
                boolean emptyCell = board.getSmallBoardCell(b, c) == UltimateBoard.EMPTY;
                cellButtons[b][c].setDisable(false);
                cellButtons[b][c].setMouseTransparent(!(enabled && legalHere && emptyCell));
            }
        }
    }

    private void updateActivePlayerHighlight() {
        playerXPanel.getStyleClass().remove("player-panel-active");
        playerOPanel.getStyleClass().remove("player-panel-active");
        if (controller == null || controller.isGameOver()) return;
        char current = controller.getBoard().getCurrentPlayer();
        (current == 'X' ? playerXPanel : playerOPanel).getStyleClass().add("player-panel-active");
    }

    private void configurePanel(HBox panel, String mark, Label nameLabel, Label roleLabel) {
        panel.getStyleClass().add("player-panel");
        panel.setAlignment(Pos.CENTER_LEFT);
        panel.setPadding(new Insets(10, 14, 10, 14));

        Label markLbl = new Label(mark);
        String color = mark.equals("X") ? "#58a6ff" : "#f0b429";
        markLbl.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        VBox names = new VBox(2);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #f0f6fc;");
        roleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #8b949e;");
        names.getChildren().addAll(nameLabel, roleLabel);

        panel.getChildren().addAll(markLbl, names);
        HBox.setMargin(markLbl, new Insets(0, 12, 0, 0));
    }

    private void updatePlayerPanels() {
        boolean humanIsX = humanMark == 'X';
        switch (controller.getMode()) {
            case HUMAN_VS_AI -> {
                panelXName.setText(humanIsX ? "You" : "Computer");
                panelOName.setText(humanIsX ? "Computer" : "You");
                panelXRole.setText(humanIsX ? "Human" : "AI - " + difficulty);
                panelORole.setText(humanIsX ? "AI - " + difficulty : "Human");
            }
            case HUMAN_VS_HUMAN -> {
                panelXName.setText("Player X");
                panelOName.setText("Player O");
                panelXRole.setText("Human");
                panelORole.setText("Human");
            }
            case AI_VS_AI -> {
                panelXName.setText("AI X");
                panelOName.setText("AI O");
                panelXRole.setText("Computer - " + difficulty);
                panelORole.setText("Computer - " + difficulty);
            }
        }
    }

    private void updateModeBadge() {
        if (modeBadge == null) return;
        modeBadge.setText(switch (selectedMode) {
            case HUMAN_VS_AI -> "vs Computer - " + difficulty;
            case LOCAL_PVP -> "Local Multiplayer";
            case AI_VS_AI -> "AI vs AI - " + difficulty;
        });
    }

    // ── Game end / results ──────────────────────────────────────────────

    /** Returns true when the game has ended (and the result UI was shown). */
    private boolean checkGameEnd() {
        if (!controller.isGameOver()) return false;

        boolean draw = controller.isDraw();
        char winner = controller.getWinner();
        boolean humanWon = !draw && winner == humanMark;

        // Session scoreboard counts every mode; persistent statistics count Human vs AI only.
        if (draw) sessionDraws++;
        else if (controller.getMode() == GameController.Mode.HUMAN_VS_AI) {
            if (humanWon) sessionHumanWins++;
            else sessionAiWins++;
        } else if (winner == 'X') sessionHumanWins++;
        else sessionAiWins++;

        if (controller.getMode() == GameController.Mode.HUMAN_VS_AI) {
            statistics.recordResult(draw ? Statistics.Result.DRAW
                    : humanWon ? Statistics.Result.HUMAN_WIN : Statistics.Result.AI_WIN);
        }

        updateScoreLabel();
        statusLabel.setText(draw ? "Draw!" :
                (controller.getMode() == GameController.Mode.HUMAN_VS_AI)
                        ? (humanWon ? "You win!" : "Computer wins.")
                        : "Player " + winner + " wins!");
        showResultOverlay(draw, winner);
        return true;
    }

    private void updateScoreLabel() {
        String xName, oName;
        if (controller != null && controller.getMode() == GameController.Mode.HUMAN_VS_AI) {
            xName = humanMark == 'X' ? "You" : "AI";
            oName = humanMark == 'X' ? "AI" : "You";
        } else {
            xName = "X";
            oName = "O";
        }
        scoreLabel.setText(xName + ": " + sessionHumanWins
                + "   " + oName + ": " + sessionAiWins
                + "   Draws: " + sessionDraws);
    }

    private void showResultOverlay(boolean draw, char winner) {
        VBox card = new VBox(14);
        card.getStyleClass().add("result-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(320);

        Label headline = new Label(resultHeadline(draw, winner));
        headline.getStyleClass().add(draw ? "result-draw"
                : "result-" + (isWinnerHumanSide(winner) ? "win" : "loss"));

        Label detail = new Label(resultDetail(draw));
        detail.getStyleClass().add("label-subtitle");
        detail.setWrapText(true);
        detail.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        Button rematch = Theme.primaryButton("Rematch");
        rematch.setOnAction(e -> {
            removeResultOverlay();
            restartMatch();
        });
        Button menu = Theme.secondaryButton("Main Menu");
        menu.setOnAction(e -> {
            searchGeneration++;
            removeResultOverlay();
            transitionTo(menuScene);
        });

        card.getChildren().addAll(headline, detail, rematch, menu);

        StackPane overlay = new StackPane(card);
        overlay.getStyleClass().add("result-overlay");
        overlay.setId("result-overlay");
        overlay.setPickOnBounds(false);
        gameRoot.getChildren().add(overlay);
    }

    private boolean isWinnerHumanSide(char winner) {
        if (controller.getMode() == GameController.Mode.HUMAN_VS_AI) return winner == humanMark;
        return true; // in PvP/AvA any win is displayed as "win" styling for X/O sides
    }

    private String resultHeadline(boolean draw, char winner) {
        if (draw) return "Draw";
        return switch (controller.getMode()) {
            case HUMAN_VS_AI -> winner == humanMark ? "Victory!" : "Defeat";
            case HUMAN_VS_HUMAN -> "Player " + winner + " wins!";
            case AI_VS_AI -> "AI " + winner + " wins!";
        };
    }

    private String resultDetail(boolean draw) {
        if (draw) return "Nobody could find a winning line.";
        return switch (controller.getMode()) {
            case HUMAN_VS_AI -> winnerText();
            case HUMAN_VS_HUMAN -> "Well played.";
            case AI_VS_AI -> "The " + controller.getWinner() + " engine takes the match.";
        };
    }

    private String winnerText() {
        return controller.getWinner() == humanMark
                ? "You outplayed the computer."
                : "The computer won this one. Try again!";
    }

    private void removeResultOverlay() {
        gameRoot.getChildren().removeIf(n -> n.getId() != null && n.getId().equals("result-overlay"));
    }

    // ── Spinner ─────────────────────────────────────────────────────────

    private void showSpinner() {
        if (aiSpinner == null) {
            aiSpinner = new ProgressIndicator();
            aiSpinner.setMaxSize(34, 34);
            aiSpinner.getStyleClass().add("ai-spinner");
            aiSpinner.setMouseTransparent(true);
        }
        if (!gameRoot.getChildren().contains(aiSpinner)) {
            gameRoot.getChildren().add(aiSpinner);
        }
        aiSpinner.setVisible(true);
    }

    private void hideSpinner() {
        if (aiSpinner != null) {
            gameRoot.getChildren().remove(aiSpinner);
            aiSpinner.setVisible(false);
        }
    }

    // ── Statistics screen ───────────────────────────────────────────────

    private Label statGames, statHumanWins, statAiWins, statDraws, statRate;

    private Scene buildStatsScene() {
        VBox content = Theme.screenRoot();

        Button back = Theme.backButton("<- Back");
        back.setOnAction(e -> transitionTo(menuScene));
        content.getChildren().add(Theme.topBar(back, "Statistics"));

        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        statGames = new Label("0");
        statHumanWins = new Label("0");
        statAiWins = new Label("0");
        statDraws = new Label("0");
        statRate = new Label("Win rate: -");

        HBox row = new HBox(24,
                statBlock(statGames, "Games"),
                statBlock(statHumanWins, "Your Wins"),
                statBlock(statAiWins, "AI Wins"),
                statBlock(statDraws, "Draws"));
        row.setAlignment(Pos.CENTER);
        statRate.getStyleClass().add("label-subtitle");

        Button reset = Theme.secondaryButton("Reset Statistics");
        reset.setOnAction(e -> {
            statistics.reset();
            refreshStatsLabels();
        });

        card.getChildren().addAll(row, statRate, reset);
        content.getChildren().addAll(card,
                Theme.captionLabel("Counts Human vs Computer games. Saved locally."));
        return new Scene(wrapCentered(content));
    }

    private VBox statBlock(Label value, String caption) {
        value.getStyleClass().add("stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("stat-label");
        VBox box = new VBox(4, value, cap);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private void refreshStatsLabels() {
        statGames.setText(String.valueOf(statistics.getGamesPlayed()));
        statHumanWins.setText(String.valueOf(statistics.getHumanWins()));
        statAiWins.setText(String.valueOf(statistics.getAiWins()));
        statDraws.setText(String.valueOf(statistics.getDraws()));
        double rate = statistics.getHumanWinRatePercent();
        statRate.setText(rate < 0 ? "Win rate: -" : String.format("Win rate: %.1f%%", rate));
    }

    // ── About ───────────────────────────────────────────────────────────

    private Scene buildAboutScene() {
        VBox content = Theme.screenRoot();

        Button back = Theme.backButton("<- Back");
        back.setOnAction(e -> transitionTo(menuScene));
        content.getChildren().add(Theme.topBar(back, "About"));

        VBox card = new VBox(12);
        card.getStyleClass().add("card");

        Label name = Theme.displayLabel("Ultimate XOX");
        name.setStyle("-fx-font-size: 24px;");
        Label version = Theme.captionLabel("Version 1.0");
        Label desc = Theme.subtitleLabel(
                "Ultimate Tic-Tac-Toe with a local Minimax AI "
                        + "(alpha-beta pruning, four difficulty levels). "
                        + "No internet connection required.");
        desc.setWrapText(true);

        card.getChildren().addAll(name, version, desc);
        content.getChildren().add(card);
        return new Scene(wrapCentered(content));
    }

    // ── Layout helper ───────────────────────────────────────────────────

    private StackPane wrapCentered(VBox content) {
        StackPane wrapper = new StackPane(content);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setStyle("-fx-background-color: #0b0e11;");
        return wrapper;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
