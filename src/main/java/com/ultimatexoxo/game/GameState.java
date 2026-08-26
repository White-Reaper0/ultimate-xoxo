package com.ultimatexox.game;

import java.util.List;

/**
 * Immutable game state for AI search and analysis.
 * Contains all information needed to evaluate a position.
 */
public final class GameState {

    private final UltimateBoard board;
    private final int ply;
    private final UltimateBoard.Move lastMove;

    public GameState(UltimateBoard board) {
        this(board, 0, null);
    }

    private GameState(UltimateBoard board, int ply, UltimateBoard.Move lastMove) {
        this.board = board;
        this.ply = ply;
        this.lastMove = lastMove;
    }

    public UltimateBoard getBoard() {
        return board;
    }

    public int getPly() {
        return ply;
    }

    public UltimateBoard.Move getLastMove() {
        return lastMove;
    }

    public char getCurrentPlayer() {
        return board.getCurrentPlayer();
    }

    public char getOpponent() {
        return getCurrentPlayer() == 'X' ? 'O' : 'X';
    }

    public boolean isTerminal() {
        return board.isGameOver();
    }

    public char getWinner() {
        return board.getWinner();
    }

    public boolean isDraw() {
        return board.isDraw();
    }

    public List<UltimateBoard.Move> getLegalMoves() {
        return board.getLegalMoves();
    }

    public GameState applyMove(UltimateBoard.Move move) {
        UltimateBoard nextBoard = board.copy();
        nextBoard.makeMove(move.board(), move.cell());
        return new GameState(nextBoard, ply + 1, move);
    }

    public GameState copy() {
        return new GameState(board.copy(), ply, lastMove);
    }

    public String serialize() {
        String movePart = lastMove != null ? lastMove.board() + "," + lastMove.cell() : "none";
        return board.serialize() + "|" + ply + "|" + movePart;
    }

    public static GameState deserialize(String data) {
        String[] parts = data.split("\\|");
        if (parts.length < 5) throw new IllegalArgumentException("Invalid format");
        String boardData = String.join("|", parts[0], parts[1], parts[2], parts[3]);
        int ply = Integer.parseInt(parts[4]);
        String movePart = parts.length > 5 ? parts[5] : "none";
        UltimateBoard board = UltimateBoard.deserialize(boardData);
        UltimateBoard.Move lastMove = null;
        if (!movePart.equals("none")) {
            String[] moveParts = movePart.split(",");
            lastMove = new UltimateBoard.Move(Integer.parseInt(moveParts[0]), Integer.parseInt(moveParts[1]));
        }
        return new GameState(board, ply, lastMove);
    }
}