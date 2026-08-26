package com.ultimatexox.ai;

import com.ultimatexox.game.UltimateBoard;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Version 1 Minimax AI:
 *  - always returns a legal move on a live board,
 *  - takes an immediately available win,
 *  - blocks an immediate losing threat,
 *  - handles terminal positions by returning null.
 */
class MinimaxAITest {

    /** HARD has no randomness, so assertions are deterministic. */
    private final MinimaxAI ai = new MinimaxAI(Difficulty.HARD);

    @Test
    void returnsNullOnTerminalPosition() {
        UltimateBoard finished = new UltimateBoard();
        finished.setMetaCell(0, 'X');
        finished.setMetaCell(1, 'X');
        finished.setMetaCell(2, 'X');
        finished.recomputeGameState();
        assertTrue(finished.isGameOver());
        assertNull(ai.chooseMove(finished));
    }

    @Test
    void choosesALegalMoveOnAFreshBoard() {
        List<UltimateBoard.Move> legal = new UltimateBoard().getLegalMoves();
        UltimateBoard.Move choice = ai.chooseMove(new UltimateBoard());
        assertNotNull(choice);
        assertTrue(legal.contains(choice), "AI must pick one of the legal moves");
    }

    @Test
    void takesAnImmediateWin() {
        // Side to move is X; X already has the top row of board 2 minus cell 8.
        UltimateBoard board = new UltimateBoard();
        board.setSmallBoardCell(2, 6, 'X');
        board.setSmallBoardCell(2, 7, 'X');

        UltimateBoard.Move move = ai.chooseMove(board);
        assertEquals(new UltimateBoard.Move(2, 8), move,
                "AI must complete three-in-a-row instead of searching");
    }

    @Test
    void blocksAnImmediateLoss() {
        // O threatens to win board 1 via its middle row at cell 5.
        // It is X's turn: any non-blocking move loses small board 1.
        UltimateBoard board = new UltimateBoard();
        board.setSmallBoardCell(1, 3, 'O');
        board.setSmallBoardCell(1, 4, 'O');

        UltimateBoard.Move move = ai.chooseMove(board);
        assertEquals(new UltimateBoard.Move(1, 5), move,
                "AI must deny the opponent's immediate win");
    }

    @Test
    void prefersWinningTheWholeGameOverASmallBoard() {
        // X owns boards 0 and 1. Playing a winning move in board 2 ends the
        // game immediately; the AI must take it.
        UltimateBoard board = new UltimateBoard();
        board.setMetaCell(0, 'X');
        board.setMetaCell(1, 'X');
        board.setSmallBoardCell(2, 0, 'X');
        board.setSmallBoardCell(2, 1, 'X');
        board.recomputeGameState();                      // no meta line yet (board 2 open)
        assertFalse(board.isGameOver());

        assertEquals(new UltimateBoard.Move(2, 2), ai.chooseMove(board));
    }

    @Test
    void difficultyLevelsAllProduceLegalMoves() {
        for (Difficulty level : Difficulty.values()) {
            UltimateBoard fresh = new UltimateBoard();
            UltimateBoard.Move move = new MinimaxAI(level).chooseMove(fresh);
            assertNotNull(move, level + " must produce a move");
            assertTrue(fresh.getLegalMoves().contains(move));
        }
    }
}
