package com.ultimatexox.game;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns ONE match of Ultimate Tic-Tac-Toe.
 *
 * The controller sits between the UI and the rules engine:
 *
 *   UI  --applyMove()-->  GameController  -->  UltimateBoard (rules)
 *        <--state queries--
 *
 * It also keeps a stack of board snapshots so Undo can restore earlier
 * positions. It deliberately knows nothing about threads or JavaFX:
 * the UI decides WHEN to run AI and applies the chosen move through
 * applyMove(), which keeps this class easy to unit test.
 */
public final class GameController {

    /** Who is playing. AI difficulty/engines are NOT handled here. */
    public enum Mode { HUMAN_VS_HUMAN, HUMAN_VS_AI, AI_VS_AI }

    private final Mode mode;
    /** In HUMAN_VS_AI: the mark the human plays ('X' when human moves first). */
    private final char humanMark;
    private final char aiMark;

    private final UltimateBoard board = new UltimateBoard();
    /** Snapshots taken BEFORE each successful move; used by undo(). */
    private final Deque<UltimateBoard> history = new ArrayDeque<>();

    public GameController(Mode mode, char humanMark) {
        if (mode == null) throw new IllegalArgumentException("mode must not be null");
        this.mode = mode;
        // X always opens the game. "Who plays first" therefore means
        // "which mark does the human get": X for Human First, O for AI First.
        this.humanMark = humanMark == 'O' ? 'O' : 'X';
        this.aiMark = this.humanMark == 'X' ? 'O' : 'X';
    }

    // ── Setup ───────────────────────────────────────────────────────────

    /** Completely resets the match: board, turn, forced board, winner, history. */
    public void startNewGame() {
        board.reset();
        history.clear();
    }

    // ── Moves ───────────────────────────────────────────────────────────

    /**
     * Attempts to play (boardIndex, cellIndex) for the current player.
     * Returns false (and changes nothing) if the move breaks any rule.
     */
    public boolean applyMove(int boardIndex, int cellIndex) {
        if (board.isGameOver()) return false;
        history.push(board.copy());          // snapshot BEFORE the move
        UltimateBoard.MoveResult result = board.makeMove(boardIndex, cellIndex);
        if (!result.success) {
            history.pop();                   // illegal: pretend nothing happened
            return false;
        }
        return true;
    }

    /**
     * Takes back moves until it is once again a HUMAN's decision point.
     *   - Human vs Human : removes exactly one move.
     *   - Human vs AI    : removes the AI reply AND the human move.
     *   - AI vs AI       : not supported (nothing happens).
     * Returns true if the position changed.
     */
    public boolean undo() {
        if (mode == Mode.AI_VS_AI || history.isEmpty()) return false;

        board.restore(history.peek());
        history.pop();
        if (mode == Mode.HUMAN_VS_HUMAN) return true;

        // HUMAN_VS_AI: keep popping while it would still be the AI's turn,
        // so the human always lands back on their own decision point.
        while (!history.isEmpty() && board.getCurrentPlayer() == aiMark) {
            board.restore(history.peek());
            history.pop();
        }
        return true;
    }

    /** True when an undo is possible right now (works even after game over). */
    public boolean canUndo() {
        return mode != Mode.AI_VS_AI && !history.isEmpty();
    }

    // ── State queries (thin pass-through to the board) ──────────────────

    public UltimateBoard getBoard()            { return board; }
    public Mode getMode()                      { return mode; }
    public char getHumanMark()                 { return humanMark; }
    public char getAiMark()                    { return aiMark; }
    public boolean isGameOver()                { return board.isGameOver(); }
    public boolean isDraw()                    { return board.isDraw(); }
    public char getWinner()                    { return board.getWinner(); }
    public int getMoveCount()                  { return history.size(); }

    /** Whose decision point is next? (AI_VS_AI exposes the raw side to move.) */
    public boolean isHumanTurn() {
        return switch (mode) {
            case HUMAN_VS_HUMAN -> true;
            case HUMAN_VS_AI -> !board.isGameOver() && board.getCurrentPlayer() == humanMark;
            case AI_VS_AI -> false;
        };
    }

    public boolean isAiTurn() {
        return switch (mode) {
            case HUMAN_VS_AI -> !board.isGameOver() && board.getCurrentPlayer() == aiMark;
            case AI_VS_AI -> !board.isGameOver();
            case HUMAN_VS_HUMAN -> false;
        };
    }
}
