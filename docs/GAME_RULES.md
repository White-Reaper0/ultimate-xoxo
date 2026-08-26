# Ultimate Tic-Tac-Toe — Game Rules

## The Board

- 9 small boards arranged in a 3x3 grid (the **meta board**).
- Each small board is itself a 3x3 tic-tac-toe grid.
- 81 cells total.

```
Board indices:          Cell indices (inside each small board):
 0 | 1 | 2                0 | 1 | 2
-----------              -----------
 3 | 4 | 5                3 | 4 | 5
-----------              -----------
 6 | 7 | 8                6 | 7 | 8
```

A cell is addressed as `(board, cell)` — e.g. `(4, 8)` is the bottom-right
cell of the centre small board.

## Turn Order

- X always plays first.
- Players alternate; a move never changes who moves next except by ending the game.

## The Forced-Board Rule (the "ultimate" twist)

The CELL you play decides WHICH small board your opponent must play in next:

> Play in cell N -> opponent must play somewhere inside small board N.

Example: you play `(2, 5)` (cell 5 of board 2). Your opponent must now make
their move inside small board 5.

## Free Choice

If the forced board is already decided (won, lost or completely full), the
constraint disappears and the player may play in ANY open small board.
This is shown in-game as "any open board" instead of "play in board N".

## Winning a Small Board

Complete three-in-a-row inside one small board to claim it.
Claimed boards are marked with the owner's symbol; drawn boards show "=".
Claimed/drawn boards accept no further moves.

## Winning the Game

Claim three small boards that form a line on the META board
(row, column or diagonal). That ends the game immediately.

Note: drawn ("=") boards belong to nobody and cannot be part of a meta line.

## Draws

The game is a draw when all nine small boards are decided and no meta line exists.

## Illegal Moves

A move is illegal if the target cell is occupied, its small board is decided,
the small board is not the currently forced one (when a force is active),
or the game is over. Illegal clicks are simply ignored by the UI.
