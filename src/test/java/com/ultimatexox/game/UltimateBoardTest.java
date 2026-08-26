package com.ultimatexox.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the rules engine: legal moves, small-board wins/draws,
 * meta wins, complete draws, the forced-board rule, copy and reset.
 */
class UltimateBoardTest {

    private UltimateBoard board;

    @BeforeEach
    void setUp() {
        board = new UltimateBoard();
    }

    // ── Initial state ──

    @Test
    void newBoardIsEmptyAndXStarts() {
        assertEquals('X', board.getCurrentPlayer());
        assertEquals(-1, board.getNextBoard());          // free choice of board
        assertFalse(board.isGameOver());
        assertEquals(UltimateBoard.EMPTY, board.getWinner());
        assertNull(board.getLastMove());
        for (int b = 0; b < 9; b++) {
            assertEquals(UltimateBoard.EMPTY, board.getMetaCell(b));
            for (int c = 0; c < 9; c++) {
                assertEquals(UltimateBoard.EMPTY, board.getSmallBoardCell(b, c));
            }
        }
    }

    @Test
    void all81MovesAreLegalOnFreshBoard() {
        assertEquals(81, board.getLegalMoves().size());
    }

    // ── Legal / illegal moves ──

    @Test
    void moveSwitchesPlayerAndForcesNextBoard() {
        assertTrue(board.makeMove(0, 4).success);
        assertEquals('O', board.getCurrentPlayer());
        assertEquals(4, board.getNextBoard());           // cell decides next board
        // Every legal reply must be inside board 4.
        List<UltimateBoard.Move> replies = board.getLegalMoves();
        assertFalse(replies.isEmpty());
        for (UltimateBoard.Move m : replies) {
            assertEquals(4, m.board());
        }
        // Playing in a different board is rejected.
        assertFalse(board.makeMove(1, 1).success);
    }

    @Test
    void illegalMovesAreRejected() {
        assertTrue(board.makeMove(0, 0).success);        // X takes top-left cell
        assertFalse(board.makeMove(0, 0).success);       // same cell occupied
        assertFalse(board.makeMove(-1, 0).success);      // out of range
        assertFalse(board.makeMove(0, 9).success);       // out of range
        assertFalse(board.makeMove(8, 8).success);       // forced board is 0, not 8
        assertEquals('O', board.getCurrentPlayer());     // nothing changed
    }

    // ── Forced-board rule ──

    @Test
    void sendingToClosedBoardGrantsFreeChoice() {
        // Board 4 is won by X: closed. A move whose cell sends O to board 4
        // must let O choose any open board instead.
        stageSmallBoardWin(4, 'X');
        assertTrue(board.makeMove(0, 4).success);        // X plays cell 4 -> would send to board 4
        assertEquals('O', board.getCurrentPlayer());
        assertEquals(-1, board.getNextBoard());
        // 8 open boards remain: seven untouched (9 cells) plus board 0 with one
        // occupied cell -> 7*9 + 8 = 71 legal replies.
        assertEquals(71, board.getLegalMoves().size());
    }

    private void stageSmallBoardWin(int boardIndex, char winner) {
        int[] line = {0, 1, 2};
        for (int c : line) board.setSmallBoardCell(boardIndex, c, winner);
        board.setMetaCell(boardIndex, winner);
        board.recomputeGameState();
    }

    // ── Small board win / draw ──

    @Test
    void smallBoardWinIsRecordedOnMetaBoard() {
        // Stage two X marks in board 5's top row; X may then play anywhere
        // (free choice) and completes the row with the third mark.
        board.setSmallBoardCell(5, 0, 'X');
        board.setSmallBoardCell(5, 1, 'X');

        assertTrue(board.makeMove(5, 2).success);        // X completes top row
        assertEquals('X', board.getMetaCell(5));         // X owns board 5
        assertEquals('O', board.getCurrentPlayer());     // game continues
        assertFalse(board.isGameOver());                 // one board does not win the match
    }

    @Test
    void fullSmallBoardWithoutLineBecomesDrawn() {
        // Fill board 0 with an alternating pattern that has no 3-in-a-row.
        char[] pattern = {'X', 'O', 'X',
                          'X', 'O', 'O',
                          'O', 'X', 'X'};
        for (int c = 0; c < 9; c++) {
            board.setSmallBoardCell(0, c, pattern[c]);
        }
        assertEquals(UltimateBoard.EMPTY, Rules.checkSmallBoardWinner(board, 0));
        assertTrue(board.isSmallBoardFull(0));
        assertTrue(board.isSmallBoardClosed(0));
    }

    // ── Meta win / complete draw ──

    @Test
    void metaWinEndsTheGame() {
        board.setMetaCell(0, 'X');
        board.setMetaCell(1, 'X');
        board.setMetaCell(2, 'X');
        board.recomputeGameState();
        assertTrue(board.isGameOver());
        assertEquals('X', board.getWinner());
        assertFalse(board.isDraw());
        assertEquals(0, board.getLegalMoves().size());
    }

    @Test
    void allNineBoardsDecidedWithoutMetaLineIsADraw() {
        char[] owners = {'X', 'O', 'D',
                         'D', 'X', 'O',
                         'O', 'D', 'O'};
        for (int i = 0; i < 9; i++) board.setMetaCell(i, owners[i]);
        board.recomputeGameState();
        assertTrue(board.isGameOver());
        assertTrue(board.isDraw());
        assertEquals(UltimateBoard.EMPTY, board.getWinner());
    }

    @Test
    void drawnBoardsDoNotCountForMetaLines() {
        board.setMetaCell(0, 'D');
        board.setMetaCell(1, 'X');
        board.setMetaCell(2, 'X');
        board.recomputeGameState();                       // D,X,X is not a win
        assertFalse(board.isGameOver());
        assertEquals(UltimateBoard.EMPTY, board.getWinner());
    }

    // ── Copy / restore / reset ──

    @Test
    void copyIsIndependentOfOriginal() {
        assertTrue(board.makeMove(0, 0).success);         // X
        UltimateBoard snapshot = board.copy();

        assertTrue(board.makeMove(0, 1).success);         // O mutates original

        assertEquals('O', snapshot.getCurrentPlayer());   // snapshot unchanged
        assertEquals(UltimateBoard.EMPTY, snapshot.getSmallBoardCell(0, 1));
        assertEquals('O', board.getSmallBoardCell(0, 1));

        snapshot.restore(board.copy());                   // restore overwrites state
        assertEquals('X', snapshot.getCurrentPlayer());   // O moved, X to play
        assertEquals('O', snapshot.getSmallBoardCell(0, 1));
    }

    @Test
    void resetReturnsToFreshState() {
        board.makeMove(0, 0);
        board.makeMove(0, 1);
        board.reset();
        assertEquals('X', board.getCurrentPlayer());
        assertEquals(-1, board.getNextBoard());
        assertFalse(board.isGameOver());
        assertEquals(81, board.getLegalMoves().size());
        assertNull(board.getLastMove());
    }

    @Test
    void serializationRoundTripPreservesPosition() {
        board.makeMove(0, 0);
        board.makeMove(0, 1);
        UltimateBoard restored = UltimateBoard.deserialize(board.serialize());
        assertEquals(board.serialize(), restored.serialize());
        assertEquals(board.getCurrentPlayer(), restored.getCurrentPlayer());
        assertEquals(board.getNextBoard(), restored.getNextBoard());
    }
}
