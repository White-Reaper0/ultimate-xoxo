package com.ultimatexox.game;

/**
 * Game rules and utilities for Ultimate Tic-Tac-Toe.
 * Pure logic, no state mutation.
 */
public final class Rules {

    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    private Rules() {}

    public static char checkSmallBoardWinner(UltimateBoard board, int boardIndex) {
        char[] cells = new char[9];
        for (int i = 0; i < 9; i++) {
            cells[i] = board.getSmallBoardCell(boardIndex, i);
        }
        for (int[] line : LINES) {
            char a = cells[line[0]];
            char b = cells[line[1]];
            char c = cells[line[2]];
            if (a != UltimateBoard.EMPTY && a == b && b == c) {
                return a;
            }
        }
        return UltimateBoard.EMPTY;
    }

    public static boolean isSmallBoardFull(UltimateBoard board, int boardIndex) {
        for (int i = 0; i < 9; i++) {
            if (board.getSmallBoardCell(boardIndex, i) == UltimateBoard.EMPTY) {
                return false;
            }
        }
        return true;
    }

    public static char checkMetaWinner(UltimateBoard board) {
        char[] meta = new char[9];
        for (int i = 0; i < 9; i++) {
            meta[i] = board.getMetaCell(i);
        }
        for (int[] line : LINES) {
            char a = meta[line[0]];
            char b = meta[line[1]];
            char c = meta[line[2]];
            if (a != UltimateBoard.EMPTY && a != 'D' && a == b && b == c) {
                return a;
            }
        }
        return UltimateBoard.EMPTY;
    }

    public static boolean isMetaFull(UltimateBoard board) {
        for (int i = 0; i < 9; i++) {
            if (board.getMetaCell(i) == UltimateBoard.EMPTY) return false;
        }
        return true;
    }

    public static int[] getWinningLine(UltimateBoard board) {
        char[] meta = new char[9];
        for (int i = 0; i < 9; i++) {
            meta[i] = board.getMetaCell(i);
        }
        for (int[] line : LINES) {
            char a = meta[line[0]];
            char b = meta[line[1]];
            char c = meta[line[2]];
            if (a != UltimateBoard.EMPTY && a != 'D' && a == b && b == c) {
                return line.clone();
            }
        }
        return null;
    }

    public static int countTwoInARow(UltimateBoard board, int boardIndex, char player) {
        char opponent = player == 'X' ? 'O' : 'X';
        int count = 0;
        char[] cells = new char[9];
        for (int i = 0; i < 9; i++) {
            cells[i] = board.getSmallBoardCell(boardIndex, i);
        }
        for (int[] line : LINES) {
            int own = 0, empty = 0, opp = 0;
            for (int idx : line) {
                if (cells[idx] == player) own++;
                else if (cells[idx] == UltimateBoard.EMPTY) empty++;
                else opp++;
            }
            if (opp == 0 && own == 2 && empty == 1) count++;
        }
        return count;
    }

    public static int countMetaTwoInARow(UltimateBoard board, char player) {
        char opponent = player == 'X' ? 'O' : 'X';
        int count = 0;
        char[] meta = new char[9];
        for (int i = 0; i < 9; i++) {
            meta[i] = board.getMetaCell(i);
        }
        for (int[] line : LINES) {
            int own = 0, empty = 0, opp = 0;
            for (int idx : line) {
                if (meta[idx] == player) own++;
                else if (meta[idx] == UltimateBoard.EMPTY) empty++;
                else if (meta[idx] == opponent) opp++;
            }
            if (opp == 0 && own == 2 && empty == 1) count++;
        }
        return count;
    }

    /**
     * True if placing {@code player} at (boardIndex, cell) would complete a
     * three-in-a-row inside that small board. Checks only the lines that pass
     * through the given cell, so no board copy is needed.
     */
    public static boolean wouldWinSmallBoard(UltimateBoard board, int boardIndex, int cell, char player) {
        for (int[] line : LINES) {
            if (line[0] != cell && line[1] != cell && line[2] != cell) continue;
            boolean allPlayer = true;
            for (int i : line) {
                int c = (i == cell) ? player : board.getSmallBoardCell(boardIndex, i);
                if (c != player) { allPlayer = false; break; }
            }
            if (allPlayer) return true;
        }
        return false;
    }

    /**
     * True if {@code player} owning small board {@code boardIndex} would complete
     * a three-in-a-row on the meta board (i.e. win the whole game).
     */
    public static boolean wouldWinMeta(UltimateBoard board, int boardIndex, char player) {
        for (int[] line : LINES) {
            if (line[0] != boardIndex && line[1] != boardIndex && line[2] != boardIndex) continue;
            boolean allPlayer = true;
            for (int i : line) {
                int o = (i == boardIndex) ? player : board.getMetaCell(i);
                if (o != player) { allPlayer = false; break; }
            }
            if (allPlayer) return true;
        }
        return false;
    }
}