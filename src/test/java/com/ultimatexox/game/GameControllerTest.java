package com.ultimatexox.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GameController: game initialisation for both first-move
 * choices (regression tests for the original startup crash), move
 * application, undo semantics per mode, and full reset.
 */
class GameControllerTest {

    // ── Initialisation regression tests ──
    // The original Version 1 crashed with ArrayIndexOutOfBoundsException
    // right after the player chose who goes first. These two tests pin down
    // the required behaviour of BOTH choices.

    @Test
    void humanFirstInitialisesWithHumanToMove() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');

        assertTrue(c.getBoard().getLegalMoves().size() > 0);
        assertFalse(c.isGameOver());
        assertEquals('X', c.getHumanMark());
        assertEquals('O', c.getAiMark());
        // X always opens; the human holds X, so it is the human's turn.
        assertTrue(c.isHumanTurn(), "Human First must start on the human's turn");
        assertFalse(c.isAiTurn(), "AI must not be asked to move when human opens");
        assertEquals(0, c.getMoveCount());
    }

    @Test
    void aiFirstInitialisesWithAiToMoveAndNoCrash() {
        // Human chose "AI First": human gets O, AI gets X and must move first.
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'O');

        assertTrue(c.getBoard().getLegalMoves().size() > 0);
        assertFalse(c.isGameOver());
        assertEquals('O', c.getHumanMark());
        assertEquals('X', c.getAiMark());
        assertTrue(c.isAiTurn(), "AI First must start on the AI's turn");
        assertFalse(c.isHumanTurn());
    }

    @Test
    void aiOpeningMoveCanBeAppliedAndYieldsHumanTurn() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'O');
        // Simulates what the UI does after its background AI search returns:
        // apply one legal AI move and hand control to the human.
        UltimateBoard.Move aiMove = c.getBoard().getLegalMoves().get(0);

        assertTrue(c.applyMove(aiMove.board(), aiMove.cell()));
        assertEquals(1, c.getMoveCount());
        assertTrue(c.isHumanTurn());
        assertEquals('O', c.getBoard().getCurrentPlayer());
    }

    /** Fills three meta cells with X so board.recomputeGameState() ends the game. */
    private static void stageMetaWinForX(GameController c) {
        UltimateBoard b = c.getBoard();
        b.setMetaCell(0, 'X');
        b.setMetaCell(4, 'X');
        b.setMetaCell(8, 'X');
        b.recomputeGameState();
    }

    // ── Move application ──

    @Test
    void illegalMoveIsRejectedWithoutStateChange() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');
        assertTrue(c.applyMove(0, 0));
        assertFalse(c.applyMove(0, 0));                  // occupied
        assertFalse(c.applyMove(3, 3));                  // forced board is 0
        assertEquals('O', c.getBoard().getCurrentPlayer()); // unchanged by failures
    }

    @Test
    void noMovesAllowedAfterGameOver() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');
        stageMetaWinForX(c);
        assertTrue(c.isGameOver());
        assertFalse(c.applyMove(5, 5));
    }

    // ── Undo ──

    @Test
    void pvpUndoRemovesExactlyOneMove() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_HUMAN, 'X');
        c.applyMove(0, 0);                               // X
        c.applyMove(0, 1);                               // O
        assertEquals(2, c.getMoveCount());

        assertTrue(c.undo());
        assertEquals(1, c.getMoveCount());
        assertEquals('O', c.getBoard().getCurrentPlayer());
        assertEquals(UltimateBoard.EMPTY, c.getBoard().getSmallBoardCell(0, 1));

        assertTrue(c.undo());
        assertEquals(0, c.getMoveCount());

        assertFalse(c.undo());                           // nothing left to undo
        assertFalse(c.canUndo());
    }

    @Test
    void undoMidTurnTakesBackJustTheLastMove() {
        // Human moved, AI replied, human moved again — but the AI has not yet
        // replied to that latest move. One undo removes exactly that move.
        // Forced-board chain: (0,0) sends to board 0, (0,1) sends to board 1,
        // so the third move must be inside board 1.
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');
        c.applyMove(0, 0);                               // human X -> forces board 0
        c.applyMove(0, 1);                               // AI O    -> forces board 1
        c.applyMove(1, 2);                               // human X -> forces board 2
        assertEquals(3, c.getMoveCount());

        assertTrue(c.undo());
        assertEquals(2, c.getMoveCount());
        assertTrue(c.isHumanTurn());
        assertEquals('X', c.getBoard().getCurrentPlayer());
        assertEquals('X', c.getBoard().getSmallBoardCell(0, 0));
        assertEquals('O', c.getBoard().getSmallBoardCell(0, 1));
        assertEquals(UltimateBoard.EMPTY, c.getBoard().getSmallBoardCell(1, 2));
    }

    @Test
    void undoAfterAnAiResponseRemovesBothMoves() {
        // Spec section 11: after the AI has replied, undo must remove the
        // human's move AND the AI response, returning to the same decision point.
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');
        c.applyMove(0, 0);                               // human X   -> forces board 0
        c.applyMove(0, 1);                               // AI O      -> forces board 1
        c.applyMove(1, 2);                               // human X   -> forces board 2
        c.applyMove(2, 0);                               // AI O replies
        assertEquals(4, c.getMoveCount());

        assertTrue(c.undo());
        assertEquals(2, c.getMoveCount());               // human+AI pair removed
        assertTrue(c.isHumanTurn(), "must be back at the human's decision point");
        assertEquals('X', c.getBoard().getCurrentPlayer());
        assertEquals(UltimateBoard.EMPTY, c.getBoard().getSmallBoardCell(1, 2));
        assertEquals(UltimateBoard.EMPTY, c.getBoard().getSmallBoardCell(2, 0));
    }

    @Test
    void aiVsAiHasNoUndo() {
        GameController c = new GameController(GameController.Mode.AI_VS_AI, 'X');
        c.applyMove(0, 0);
        assertFalse(c.canUndo());
        assertFalse(c.undo());
    }

    // ── Restart ──

    @Test
    void restartCompletelyResetsTheMatch() {
        GameController c = new GameController(GameController.Mode.HUMAN_VS_AI, 'X');
        c.applyMove(0, 0);
        c.applyMove(0, 1);
        stageMetaWinForX(c);                              // end the match abruptly
        assertTrue(c.isGameOver());

        c.startNewGame();

        assertFalse(c.isGameOver());
        assertFalse(c.isDraw());
        assertEquals(UltimateBoard.EMPTY, c.getWinner());
        assertEquals(-1, c.getBoard().getNextBoard());
        assertNull(c.getBoard().getLastMove());
        assertEquals('X', c.getBoard().getCurrentPlayer());
        assertEquals(0, c.getMoveCount());
        assertEquals(81, c.getBoard().getLegalMoves().size());
        assertFalse(c.canUndo());
    }
}
