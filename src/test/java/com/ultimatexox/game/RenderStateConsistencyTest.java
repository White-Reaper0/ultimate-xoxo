package com.ultimatexox.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guarantees that the data the board renderer reads is always consistent
 * with the moves that were applied.
 *
 * XoxoApp.refreshBoard() paints every cell strictly from these accessors:
 * getSmallBoardCell(b, c) and getLastMove(). If the assertions below hold,
 * then MOVE STATE == VISUAL STATE by construction — a mark can only be on
 * screen if the engine stores it, and it disappears the moment the engine
 * no longer stores it (undo / restart).
 */
class RenderStateConsistencyTest {

    private final GameController game =
            new GameController(GameController.Mode.HUMAN_VS_HUMAN, 'X');

    /** All cells the renderer would currently paint with a mark. */
    private List<String> paintedMarks(UltimateBoard b) {
        List<String> marks = new ArrayList<>();
        for (int board = 0; board < 9; board++) {
            for (int cell = 0; cell < 9; cell++) {
                char p = b.getSmallBoardCell(board, cell);
                if (p != UltimateBoard.EMPTY) marks.add(p + "@" + board + "," + cell);
            }
        }
        return marks;
    }

    @Test
    void everyAppliedMoveIsVisibleInTheRenderedData() {
        // Five legal, forced-board-respecting moves (cell sends to that board):
        int[][] moves = {{0, 0}, {0, 1}, {1, 2}, {2, 3}, {3, 4}};
        List<String> expected = new ArrayList<>();
        for (int[] m : moves) {
            assertTrue(game.applyMove(m[0], m[1]), "move " + m[0] + "," + m[1] + " must be legal");
            expected.add(game.getBoard().getSmallBoardCell(m[0], m[1]) + "@" + m[0] + "," + m[1]);
        }
        // Every one of the five marks must still be present simultaneously,
        // including the first move after all later ones.
        List<String> visible = paintedMarks(game.getBoard());
        assertEquals(expected, visible);
    }

    @Test
    void lastMoveStaysReadableAfterLaterMoves() {
        game.applyMove(0, 0);
        game.applyMove(0, 1);
        game.applyMove(1, 2);

        UltimateBoard.Move last = game.getBoard().getLastMove();
        assertEquals(new UltimateBoard.Move(1, 2), last);
        assertEquals('X', game.getBoard().getSmallBoardCell(last.board(), last.cell()),
                "the newest mark must be readable at its coordinates");
        assertEquals('X', game.getBoard().getSmallBoardCell(0, 0),
                "older marks must remain readable after newer ones");
    }

    @Test
    void undoRemovesTheMarkFromTheRenderedData() {
        game.applyMove(0, 0);
        game.applyMove(0, 1);
        assertTrue(game.undo());

        assertEquals(UltimateBoard.EMPTY, game.getBoard().getSmallBoardCell(0, 1),
                "undo must clear the cell the renderer paints");
        assertEquals(new UltimateBoard.Move(0, 0), game.getBoard().getLastMove(),
                "last-move highlight data must point at the surviving mark");
    }

    @Test
    void restartClearsAll81CellsTheRendererPaints() {
        game.applyMove(0, 0);
        game.applyMove(0, 1);
        assertFalse(paintedMarks(game.getBoard()).isEmpty());

        game.startNewGame();

        assertTrue(paintedMarks(game.getBoard()).isEmpty(),
                "after restart no cell may contain a mark");
        assertNull(game.getBoard().getLastMove());
    }

    @Test
    void renderedMarksAlwaysMatchThePlayerWhoMoved() {
        char expected = 'X';
        // Forced-board chain: playing CELL n forces the opponent into BOARD n.
        // (4,0)->board0, (0,8)->board8, (8,7)->board7, (7,6)->board6.
        for (int[] m : new int[][]{{4, 0}, {0, 8}, {8, 7}, {7, 6}}) {
            assertTrue(game.applyMove(m[0], m[1]), "chain move must be legal");
            char stored = game.getBoard().getSmallBoardCell(m[0], m[1]);
            assertEquals(expected, stored, "renderer would show the wrong symbol");
            expected = (expected == 'X') ? 'O' : 'X';
        }
    }
}
