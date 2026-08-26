# Minimax AI (Version 1)

The complete AI lives in one file: `src/main/java/com/ultimatexox/ai/MinimaxAI.java`.
Read it top to bottom — the class comment explains every concept used here.

## The Pipeline

```
Game state (UltimateBoard)
    |
    v
Generate legal moves            board.getLegalMoves()
    |
    v
Iterative deepening search      depth 1, 2, 3 ... until time/depth limit
    |                           (each iteration keeps the best move found)
    v
Negamax + alpha-beta pruning    scores positions; skips useless branches
    |
    v
Static evaluation               heuristic for non-terminal leaf nodes
    |
    v
Best move                       returned to the UI
```

## Key Ideas

### Negamax
Minimax written symmetrically. A position's score is always from the point of
view of the player to move. Because perspectives flip every ply, a child's
score is simply negated: `score = -negamax(child, ...)`.

Terminal scores: win = `+WIN_SCORE` (+ remaining-depth bonus so faster wins
score higher), loss = `-WIN_SCORE`, draw = `0`.

One engine quirk matters: after a game-ending move the board does NOT switch
the current player, so terminal scoring compares against `getWinner()` instead
of assuming who is "to move".

#
### Move ordering
Moves are sorted by a cheap guess before searching:
1. moves that immediately win a small board,
2. moves that block an opponent's small-board win or meta threat,
3. centre cells/boards.

Strong moves first => cutoffs happen earlier => deeper search in the budget.

### Static evaluation (`evaluate`)
For non-terminal leaves, positive = good for the side to move:
- owning a small board: +/- ~500 (centre board and corners weighted higher),
- two-in-a-row threats on the meta board: +/- ~800,
- two-in-a-row threats inside open boards: +/- 60..75,
- occupying the centre cell of an open board: small bonus.

These numbers are heuristics, tuned by feel — not magic constants.

## Difficulty Levels

Difficulty only changes three parameters (see `ai/Difficulty.java`):

| Level      | Max depth | Time budget | Random-move chance |
|------------|-----------|-------------|--------------------|
| Easy       | 2         | 100 ms      | 30%                |
| Medium     | 4         | 500 ms      | 5%                 |
| Hard       | 6         | 1500 ms     | 0%                 |
| Impossible | 12        | 3000 ms     | 0%                 |

"Iterative deepening" means the AI searches depth 1, then 2, ... and keeps the
best move from the last COMPLETED depth before the time runs out. That is how
Impossible "uses its full budget" safely without ever freezing the UI.

## Threading Contract

`chooseMove()` is pure with respect to the passed board (it copies) and may
take up to the difficulty's time budget. It must therefore be called OFF the
JavaFX thread. The UI wraps it in a worker thread and applies the result via
`Platform.runLater`, discarding results whose generation counter no longer
matches (restart/left mid-search).
