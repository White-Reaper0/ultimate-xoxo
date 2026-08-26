package com.ultimatexox.ai;

import com.ultimatexox.game.Rules;
import com.ultimatexox.game.UltimateBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Version 1 AI: Minimax with alpha-beta pruning.
 *
 * HOW A MOVE IS CHOSEN
 * --------------------
 *   1. List all legal moves for the current position.
 *   2. For each move, imagine playing it (on a copy of the board).
 *   3. Score the resulting position with the negamax search below.
 *   4. Play the move with the best score.
 *
 * NEGAMAX IN ONE PARAGRAPH
 * ------------------------
 * Negamax is Minimax written symmetrically: the score of a position is always
 * from the point of view of the player who is about to move. A win for the
 * side to move is a large positive number; a loss is a large negative number.
 * Because the perspective flips every ply, the child's score is simply
 * negated: my position is exactly as good as my opponent's position is bad.
 *
 * ALPHA-BETA PRUNING IN ONE PARAGRAPH
 * -----------------------------------
 * While searching moves we remember the best score found so far (alpha) and
 * the score the opponent can already guarantee elsewhere (beta). As soon as
 * alpha >= beta, the remaining sibling moves cannot influence the final
 * decision and are skipped ("pruned"). This makes the same search visit far
 * fewer positions without changing the result.
 *
 * ITERATIVE DEEPENING
 * -------------------
 * Instead of searching directly to depth N (which may take too long), we
 * search depth 1, then 2, ... until either maxDepth or the time budget runs
 * out. The last fully completed iteration wins. Deeper iterations also give
 * better move ordering information, which makes alpha-beta prune harder.
 */
public final class MinimaxAI {

    /** Scores above this mean "this side forces a win" in the search. */
    private static final int WIN_SCORE = 100_000;

    private static final int[][] LINES = {
        {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
        {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
        {0, 4, 8}, {2, 4, 6}
    };

    private final Difficulty difficulty;
    // Used only to add small randomness on Easy/Medium so games don't repeat.
    private final Random random = new Random();

    public MinimaxAI(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /** Simple analysis result used by the UI hint feature. */
    public record Analysis(UltimateBoard.Move bestMove, int scoreCentipawns) {}

    /**
     * Picks the move to play for the side to move on {@code board}.
     * Never mutates {@code board}. Returns null only when the game is over.
     */
    public UltimateBoard.Move chooseMove(UltimateBoard board) {
        if (board.isGameOver()) return null;
        List<UltimateBoard.Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) return null;
        if (moves.size() == 1) return moves.get(0);

        // Taking an immediate win needs no search — even Easy does it,
        // which keeps weak levels fun instead of blind.
        UltimateBoard.Move instantWin = findImmediateWin(board, moves);
        if (instantWin != null) return instantWin;

        // Weak levels sometimes play a random legal move instead of searching.
        if (random.nextDouble() < difficulty.getRandomMoveChance()) {
            return moves.get(random.nextInt(moves.size()));
        }

        return searchBestMove(board).bestMove();
    }

    /** Full-strength analysis used by the "Hint" button. No randomness applied. */
    public Analysis analyze(UltimateBoard board) {
        if (board.isGameOver()) return null;
        List<UltimateBoard.Move> moves = board.getLegalMoves();
        if (moves.isEmpty()) return null;
        UltimateBoard.Move instantWin = findImmediateWin(board, moves);
        if (instantWin != null) return new Analysis(instantWin, WIN_SCORE);
        return searchBestMove(board);
    }

    // ── Search ──────────────────────────────────────────────────────────

    private Analysis searchBestMove(UltimateBoard root) {
        long deadline = System.nanoTime() + difficulty.getTimeBudgetMs() * 1_000_000L;
        List<UltimateBoard.Move> ordered = orderMoves(root, root.getLegalMoves());

        UltimateBoard.Move bestMove = ordered.get(0); // never null: caller checked
        int bestScore = 0;

        for (int depth = 1; depth <= difficulty.getMaxDepth(); depth++) {
            int alpha = Integer.MIN_VALUE + 1;
            int beta = Integer.MAX_VALUE - 1;
            UltimateBoard.Move depthBest = null;
            int depthBestScore = Integer.MIN_VALUE;
            boolean completed = true;

            for (UltimateBoard.Move move : ordered) {
                UltimateBoard next = root.copy();
                next.makeMove(move.board(), move.cell());
                // Negate: after our move the opponent is "side to move".
                int score = -negamax(next, depth - 1, -beta, -alpha);
                if (score > depthBestScore) {
                    depthBestScore = score;
                    depthBest = move;
                }
                if (score > alpha) alpha = score;
                if (System.nanoTime() > deadline) { completed = false; break; }
            }

            if (completed && depthBest != null) {
                bestMove = depthBest;
                bestScore = depthBestScore;
            }
            if (!completed) break;
            // Found a forced win — no deeper search can improve it.
            if (bestScore > WIN_SCORE / 2) break;
        }
        return new Analysis(bestMove, bestScore);
    }

    /**
     * Score of {@code board} for the side to move, searching up to
     * {@code depthRemaining} plies ahead. Alpha-beta window: [alpha, beta].
     */
    private int negamax(UltimateBoard board, int depthRemaining, int alpha, int beta) {
        if (board.isGameOver()) {
            if (board.isDraw()) return 0;
            // Note: the engine does not switch players after the final move, so we
            // compare against getWinner() instead of assuming who is "to move".
            // Prefer faster wins: more remaining depth means an earlier win.
            boolean sideToMoveWon = board.getWinner() == board.getCurrentPlayer();
            return sideToMoveWon ? WIN_SCORE + depthRemaining : -(WIN_SCORE + depthRemaining);
        }
        if (depthRemaining <= 0) return evaluate(board);

        for (UltimateBoard.Move move : orderMoves(board, board.getLegalMoves())) {
            UltimateBoard next = board.copy();
            next.makeMove(move.board(), move.cell());
            int score = -negamax(next, depthRemaining - 1, -beta, -alpha);
            if (score >= beta) return score;      // beta cutoff — opponent won't allow this
            if (score > alpha) alpha = score;     // tighten window for later siblings
        }
        return alpha;
    }

    /** Orders moves so that likely-strong moves are searched first (better pruning). */
    private List<UltimateBoard.Move> orderMoves(UltimateBoard board, List<UltimateBoard.Move> moves) {
        char me = board.getCurrentPlayer();
        char opp = (me == 'X') ? 'O' : 'X';
        // Score each move once, then sort by that cached score. Scoring inside the
        // comparator would recompute values many times during sorting.
        record Scored(UltimateBoard.Move move, int score) {}
        List<Scored> scored = new ArrayList<>(moves.size());
        for (UltimateBoard.Move m : moves) {
            scored.add(new Scored(m, heuristicMoveScore(board, m, me, opp)));
        }
        scored.sort((a, b) -> Integer.compare(b.score(), a.score()));
        List<UltimateBoard.Move> ordered = new ArrayList<>(scored.size());
        for (Scored s : scored) ordered.add(s.move());
        return ordered;
    }

    private int heuristicMoveScore(UltimateBoard board, UltimateBoard.Move m, char me, char opp) {
        int s = 0;
        if (Rules.wouldWinSmallBoard(board, m.board(), m.cell(), me)) s += 10_000; // win a small board now
        else if (Rules.wouldWinSmallBoard(board, m.board(), m.cell(), opp)) s += 8_000; // deny opponent
        if (Rules.wouldWinMeta(board, m.board(), opp)) s += 9_000;  // stop their meta threat
        if (m.cell() == 4) s += 30;                                  // centre cell is flexible
        if (m.cell() == 0 || m.cell() == 2 || m.cell() == 6 || m.cell() == 8) s += 15;
        if (m.board() == 4) s += 20;                                 // centre small board is strong
        return s;
    }

    private UltimateBoard.Move findImmediateWin(UltimateBoard board, List<UltimateBoard.Move> moves) {
        char me = board.getCurrentPlayer();
        for (UltimateBoard.Move m : moves) {
            UltimateBoard next = board.copy();
            next.makeMove(m.board(), m.cell());
            if (next.isGameOver() && !next.isDraw() && next.getWinner() == me) return m;
        }
        return null;
    }

    // ── Static evaluation ───────────────────────────────────────────────

    /**
     * Heuristic score of a non-terminal position, positive = good for the
     * side to move. Combines three simple ideas:
     *   1. owning small boards (weighted towards the centre),
     *   2. two-in-a-row threats on the meta board,
     *   3. two-in-a-row threats inside open small boards.
     */
    int evaluate(UltimateBoard board) {
        char me = board.getCurrentPlayer();
        char opp = (me == 'X') ? 'O' : 'X';
        int score = 0;

        for (int b = 0; b < UltimateBoard.NUM_SMALL_BOARDS; b++) {
            double weight = positionalWeight(b);
            char owner = board.getMetaCell(b);

            if (owner == me) score += 500 * weight;
            else if (owner == opp) score -= 550 * weight; // denying them slightly matters more
            else {
                // Open board: count threats inside it.
                score += 60 * Rules.countTwoInARow(board, b, me);
                score -= 75 * Rules.countTwoInARow(board, b, opp);
                // Small nudge for occupying central cells of open boards.
                if (board.getSmallBoardCell(b, 4) == me) score += 12 * weight;
                else if (board.getSmallBoardCell(b, 4) == opp) score -= 12 * weight;
            }
        }

        score += 800 * Rules.countMetaTwoInARow(board, me);
        score -= 900 * Rules.countMetaTwoInARow(board, opp);
        return score;
    }

    private double positionalWeight(int index) {
        return switch (index) {
            case 4 -> 1.5;                                   // centre board
            case 0, 2, 6, 8 -> 1.2;                          // corners
            default -> 1.0;                                  // edges
        };
    }
}
