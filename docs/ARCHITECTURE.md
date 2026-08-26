# Architecture (Version 1)

## Package Layout

```
src/main/java/com/ultimatexox/
    app/Main.java          thin launcher (delegates to XoxoApp)
    app/ui/XoxoApp.java    JavaFX screens + user interaction
    app/ui/Theme.java      shared styles/components
    game/UltimateBoard.java  81-cell board, rules state, legal moves
    game/Rules.java          pure rule/evaluation helpers (no state)
    game/GameController.java owns ONE match: modes, marks, undo history
    ai/Difficulty.java       Easy / Medium / Hard / Impossible parameters
    ai/MinimaxAI.java        the entire V1 AI
    stats/Statistics.java    local file persistence of results

src/test/java/com/ultimatexox/
    game/   UltimateBoardTest, GameControllerTest
    ai/     MinimaxAITest


## How a Move Flows Through the System

```
User click (XoxoApp.onCellClicked)
    |
    v
GameController.applyMove(b, c)      snapshot pushed for Undo, then:
    |
    v
UltimateBoard.makeMove(b, c)        validates against the rules, applies,
    |                               updates forced board / winner / draw
    v
XoxoApp.refreshBoard()              repaints cells from the board
```

## How the AI Moves (without freezing the UI)

```
UI thread                    background thread
---------                    -----------------
triggerAiMove():
  snapshot = board.copy()
  generation = ++searchGen
  start worker  ----------->  move = MinimaxAI.chooseMove(snapshot)
  (spinner shown)             ...up to difficulty time budget...
Platform.runLater(<- applies result only if generation matches)
```

The **generation counter** guarantees that Restart / Menu / Undo invalidate
any AI computation still running. A stale result is discarded instead of
applied to the new match.

## Who Plays First

`X` always opens the game. Choosing "Human First" gives the human `X`;
"AI First" gives the human `O` and schedules an AI move right after the
game scene appears — never before the board is initialised.
This is pinned by regression tests in `GameControllerTest`.

## Running

```
mvn javafx:run        # run the desktop app
mvn test              # run all tests
mvn clean package     # build jar + jlink image (see pom.xml)
```
