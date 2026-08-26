package com.ultimatexox.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Ultimate Tic-Tac-Toe board: nine 3x3 boards inside a 3x3 meta-board.
 * Pure Java, no UI dependencies.
 */
public final class UltimateBoard {

    public static final char EMPTY = '-';
    public static final int NUM_SMALL_BOARDS = 9;
    public static final int SMALL_BOARD_SIZE = 9;
    public static final int TOTAL_CELLS = 81;

    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // rows
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // columns
        {0, 4, 8}, {2, 4, 6}             // diagonals
    };

    final char[] smallBoards = new char[TOTAL_CELLS];
    final char[] metaBoard = new char[NUM_SMALL_BOARDS];
    int nextBoard = -1; // -1 means free move
    char currentPlayer = 'X';
    char winner = EMPTY;
    int[] winningLine = null;
    boolean gameOver = false;
    boolean draw = false;
    Move lastMove = null;

    public UltimateBoard() {
        reset();
    }

    public void reset() {
        Arrays.fill(smallBoards, EMPTY);
        Arrays.fill(metaBoard, EMPTY);
        nextBoard = -1;
        currentPlayer = 'X';
        winner = EMPTY;
        winningLine = null;
        gameOver = false;
        draw = false;
        lastMove = null;
    }

    public char getSmallBoardCell(int boardIndex, int cellIndex) {
        return smallBoards[boardIndex * SMALL_BOARD_SIZE + cellIndex];
    }

    public void setSmallBoardCell(int boardIndex, int cellIndex, char mark) {
        smallBoards[boardIndex * SMALL_BOARD_SIZE + cellIndex] = mark;
    }

    public char getMetaCell(int index) {
        return metaBoard[index];
    }

    /**
     * Directly assigns a small board's meta owner ('X', 'O', 'D' or EMPTY).
     * Intended for tests and for restoring saved positions — normal play
     * should use makeMove(), which also handles turn switching.
     */
    public void setMetaCell(int index, char owner) {
        metaBoard[index] = owner;
    }

    /** Recomputes gameOver/winner/draw after the board was modified directly. */
    public void recomputeGameState() {
        checkGameState();
    }

    public int getNextBoard() {
        return nextBoard;
    }

    public char getCurrentPlayer() {
        return currentPlayer;
    }

    public char getWinner() {
        return winner;
    }

    public int[] getWinningLine() {
        return winningLine;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public boolean isDraw() {
        return draw;
    }

    public boolean isSmallBoardWon(int boardIndex) {
        return metaBoard[boardIndex] != EMPTY;
    }

    public boolean isSmallBoardFull(int boardIndex) {
        for (int i = 0; i < SMALL_BOARD_SIZE; i++) {
            if (smallBoards[boardIndex * SMALL_BOARD_SIZE + i] == EMPTY) {
                return false;
            }
        }
        return true;
    }

    public boolean isSmallBoardClosed(int boardIndex) {
        return isSmallBoardWon(boardIndex) || isSmallBoardFull(boardIndex);
    }

    public List<Integer> getLegalBoards() {
        List<Integer> boards = new ArrayList<>();
        if (gameOver) return boards;

        if (nextBoard != -1 && !isSmallBoardClosed(nextBoard)) {
            boards.add(nextBoard);
        } else {
            for (int i = 0; i < NUM_SMALL_BOARDS; i++) {
                if (!isSmallBoardClosed(i)) {
                    boards.add(i);
                }
            }
        }
        return boards;
    }

    public List<Move> getLegalMoves() {
        List<Move> moves = new ArrayList<>();
        for (int board : getLegalBoards()) {
            for (int cell = 0; cell < SMALL_BOARD_SIZE; cell++) {
                if (getSmallBoardCell(board, cell) == EMPTY) {
                    moves.add(new Move(board, cell));
                }
            }
        }
        return moves;
    }

    public boolean isLegalMove(int board, int cell) {
        if (gameOver) return false;
        if (board < 0 || board >= NUM_SMALL_BOARDS) return false;
        if (cell < 0 || cell >= SMALL_BOARD_SIZE) return false;
        if (getSmallBoardCell(board, cell) != EMPTY) return false;
        if (isSmallBoardClosed(board)) return false;
        if (nextBoard != -1 && nextBoard != board && !isSmallBoardClosed(nextBoard)) return false;
        return true;
    }

    public MoveResult makeMove(int board, int cell) {
        if (!isLegalMove(board, cell)) {
            return new MoveResult(false, "Illegal move");
        }

        lastMove = new Move(board, cell);
        setSmallBoardCell(board, cell, currentPlayer);

        MoveResult result = checkSmallBoardWin(board);
        if (result.success) {
            metaBoard[board] = currentPlayer;
            result = checkMetaWin();
            if (result.success) {
                gameOver = true;
                winner = currentPlayer;
                return result;
            }
            if (isMetaBoardFull()) {
                gameOver = true;
                draw = true;
                return new MoveResult(true, "Draw");
            }
        } else if (isSmallBoardFull(board)) {
            metaBoard[board] = 'D';
            if (isMetaBoardFull()) {
                gameOver = true;
                draw = true;
                return new MoveResult(true, "Draw");
            }
        }

        int targetBoard = cell;
        if (isSmallBoardClosed(targetBoard)) {
            nextBoard = -1;
        } else {
            nextBoard = targetBoard;
        }

        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        return new MoveResult(true, "Move played");
    }

    public Move getLastMove() {
        return lastMove;
    }

    private MoveResult checkSmallBoardWin(int board) {
        char[] cells = new char[SMALL_BOARD_SIZE];
        for (int i = 0; i < SMALL_BOARD_SIZE; i++) {
            cells[i] = getSmallBoardCell(board, i);
        }

        for (int[] line : LINES) {
            char a = cells[line[0]];
            char b = cells[line[1]];
            char c = cells[line[2]];
            if (a != EMPTY && a == b && b == c) {
                winningLine = line.clone();
                return new MoveResult(true, "Small board won");
            }
        }
        return new MoveResult(false, "No win");
    }

    private MoveResult checkMetaWin() {
        for (int[] line : LINES) {
            char a = metaBoard[line[0]];
            char b = metaBoard[line[1]];
            char c = metaBoard[line[2]];
            if (a != EMPTY && a != 'D' && a == b && b == c) {
                winner = a;
                winningLine = line.clone();
                return new MoveResult(true, "Meta board won");
            }
        }
        return new MoveResult(false, "No meta win");
    }

    private boolean isMetaBoardFull() {
        for (char c : metaBoard) {
            if (c == EMPTY) return false;
        }
        return true;
    }

    public UltimateBoard copy() {
        UltimateBoard copy = new UltimateBoard();
        copy.restore(this);
        return copy;
    }

    /**
     * Replaces this board's entire state with {@code other}'s.
     * Used by Undo (restore a snapshot) and by copy().
     */
    public void restore(UltimateBoard other) {
        System.arraycopy(other.smallBoards, 0, this.smallBoards, 0, TOTAL_CELLS);
        System.arraycopy(other.metaBoard, 0, this.metaBoard, 0, NUM_SMALL_BOARDS);
        this.nextBoard = other.nextBoard;
        this.currentPlayer = other.currentPlayer;
        this.winner = other.winner;
        this.winningLine = other.winningLine != null ? other.winningLine.clone() : null;
        this.gameOver = other.gameOver;
        this.draw = other.draw;
        this.lastMove = other.lastMove;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Meta: ");
        for (int i = 0; i < 9; i++) {
            sb.append(metaBoard[i] == EMPTY ? '-' : metaBoard[i]);
            if (i % 3 == 2) sb.append(" ");
        }
        sb.append("\n");
        for (int br = 0; br < 3; br++) {
            for (int r = 0; r < 3; r++) {
                for (int bc = 0; bc < 3; bc++) {
                    int board = br * 3 + bc;
                    for (int c = 0; c < 3; c++) {
                        int cell = r * 3 + c;
                        sb.append(getSmallBoardCell(board, cell));
                    }
                    if (bc < 2) sb.append(" | ");
                }
                sb.append("\n");
            }
            if (br < 2) sb.append("------+-------+------\n");
        }
        sb.append("Next board: ").append(nextBoard == -1 ? "FREE" : nextBoard).append("\n");
        sb.append("Current: ").append(currentPlayer).append("\n");
        if (gameOver) {
            sb.append(draw ? "DRAW" : winner + " WINS");
        }
        return sb.toString();
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(currentPlayer).append('|');
        sb.append(nextBoard).append('|');
        for (char c : metaBoard) sb.append(c == EMPTY ? '-' : c);
        sb.append('|');
        for (int b = 0; b < NUM_SMALL_BOARDS; b++) {
            for (int c = 0; c < SMALL_BOARD_SIZE; c++) {
                char v = getSmallBoardCell(b, c);
                sb.append(v == EMPTY ? '.' : v);
            }
            if (b < NUM_SMALL_BOARDS - 1) sb.append('/');
        }
        return sb.toString();
    }

    public static UltimateBoard deserialize(String data) {
        UltimateBoard board = new UltimateBoard();
        String[] parts = data.split("\\|");
        if (parts.length != 4) throw new IllegalArgumentException("Invalid serialization format");

        board.currentPlayer = parts[0].charAt(0);
        board.nextBoard = Integer.parseInt(parts[1]);

        char[] meta = parts[2].toCharArray();
        for (int i = 0; i < NUM_SMALL_BOARDS; i++) {
            board.metaBoard[i] = meta[i] == '-' ? EMPTY : meta[i];
        }

        String[] boards = parts[3].split("/");
        for (int b = 0; b < NUM_SMALL_BOARDS; b++) {
            char[] cells = boards[b].toCharArray();
            for (int c = 0; c < SMALL_BOARD_SIZE; c++) {
                board.smallBoards[b * SMALL_BOARD_SIZE + c] = cells[c] == '.' ? EMPTY : cells[c];
            }
        }

        board.checkGameState();
        return board;
    }

    private void checkGameState() {
        // checkMetaWin() assigns this.winner when a meta line exists.
        if (checkMetaWin().success) {
            gameOver = true;
            return;
        }
        if (isMetaBoardFull()) {
            gameOver = true;
            draw = true;
        }
    }

    /**
     * One candidate action: play in small board {@code board} at {@code cell}.
     * Both indices are 0..8. A record gives us equals/hashCode for free.
     */
    public record Move(int board, int cell) {

        /** Flattened index 0..80 across all 81 cells (row-major). */
        public int toGlobalIndex() {
            return board * SMALL_BOARD_SIZE + cell;
        }

        public static Move fromGlobalIndex(int globalIndex) {
            return new Move(globalIndex / SMALL_BOARD_SIZE, globalIndex % SMALL_BOARD_SIZE);
        }

        @Override
        public String toString() {
            return "Move(board=" + board + ", cell=" + cell + ")";
        }
    }

    public static final class MoveResult {
        public final boolean success;
        public final String message;

        public MoveResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}