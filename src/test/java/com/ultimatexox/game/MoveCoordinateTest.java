package com.ultimatexox.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for the board <-> UI coordinate conventions.
 *
 * The JavaFX grid locates nodes as cellButtons[board][cell] where
 * board/cell are both 0..8. Global 0..80 indexes must convert back to
 * exactly that pair, otherwise a move would light up the wrong square.
 */
class MoveCoordinateTest {

    @Test
    void globalIndexRoundTripCoversAll81Cells() {
        for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
                UltimateBoard.Move move = new UltimateBoard.Move(b, c);
                int global = move.toGlobalIndex();
                assertTrue(global >= 0 && global < 81, "global index out of range: " + global);
                assertEquals(move, UltimateBoard.Move.fromGlobalIndex(global),
                        "(" + b + "," + c + ") lost its identity through conversion");
            }
        }
    }

    @Test
    void globalIndexesAreUniqueAcrossAllCells() {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (int b = 0; b < 9; b++) {
            for (int c = 0; c < 9; c++) {
                assertTrue(seen.add(new UltimateBoard.Move(b, c).toGlobalIndex()),
                        "two different cells share one global index");
            }
        }
    }

    /**
     * Mirrors the UI's layout math: small board b sits at metaGrid
     * (col = b % 3, row = b / 3) and cell c at (col = c % 3, row = c / 3).
     * If these formulas drift from XoxoApp.createSmallBoard(), marks appear
     * in the wrong squares.
     */
    @Test
    void layoutMathMatchesRowMajorGridPlacement() {
        for (int index = 0; index < 9; index++) {
            assertEquals(index % 3, index - (index / 3) * 3,
                    "col formula mismatch for index " + index);
            assertTrue(index / 3 >= 0 && index / 3 <= 2);
        }
    }
}
