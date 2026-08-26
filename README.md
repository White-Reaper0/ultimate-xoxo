# Ultimate XOX — Version 1

Ultimate XOX is a **native Java desktop application** implementing
**Ultimate Tic-Tac-Toe**: nine 3×3 boards arranged inside a 3×3 meta-board,
with a local **Minimax AI** (alpha-beta pruning).

> Version 1 philosophy: **working + stable + simple + understandable + easy to upgrade.**
> No neural networks, no MCTS, no servers, no internet connection — just a solid,
> fully offline game that a CS student can read, understand and improve.

## Highlights

- **Full Ultimate Tic-Tac-Toe rules** — forced-board rule, small-board wins/draws,
  meta-board wins, complete draws
- **Three game modes** — Human vs Computer, Human vs Human (shared device),
  AI vs AI (spectator)
- **Minimax AI with alpha-beta pruning** — iterative deepening under a time budget,
  smart move ordering, clean heuristic evaluation (all in one readable file)
- **Four difficulty levels** — Easy / Medium / Hard / Impossible
- **Who plays first choice** — Human First, AI First or Random (fully reliable)
- **Undo** — removes your last move plus the AI's reply, back to your decision point
- **Restart** — one click resets board, turn, forced board, history and UI
- **Hint button** — basic move analysis: best move + evaluation from the same engine
- **Persistent statistics** — games played, your wins, AI wins, draws, win rate;
  stored locally in `%USERPROFILE%\.ultimate-xox\stats.properties`
- **Readable marks & highlights** — X/O always visible, last move highlighted,
  playable small board(s) outlined green
- **Responsive window** — open/maximize/resize freely; size and maximized state are
  preserved across scene changes
- **Zero external dependencies at runtime** — no API keys, no network, works offline

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 or newer (developed and tested on JDK 26) |
| Maven | 3.9+ |

## How to Run the Game

```bash
cd UltimateXOX
mvn javafx:run
```

A splash screen appears, then the main menu:
**New Game → choose a mode → (difficulty + who plays first) → play.**

### Run the Tests

```bash
cd UltimateXOX
mvn test
```

### Package (jar + jlink image)

```bash
cd UltimateXOX
mvn clean package
```

`jpackage` can later wrap the jlink image into a double-clickable
`UltimateXOX.exe` once the application is stable (planned post-V1).

## Controls During a Game

| Button | Action |
|--------|--------|
| **Undo** | Takes back your last move (+ the AI reply in Human vs AI) |
| **Hint** | Runs a background analysis and outlines the suggested cell |
| **Restart** | Completely resets the match (board, turn, history, UI) |
| **← Menu** | Returns to the main menu (any running AI search is safely discarded) |

Green outline = you may play in that small board. Amber glow = last move played.
The corner badge of each small board shows its owner (`=` = drawn).

## AI Difficulty Levels

Difficulty changes exactly three search parameters — nothing more:

| Level | Max depth | Time budget | Random-move chance |
|-------|-----------|-------------|--------------------|
| Easy | 2 | 100 ms | 30 % |
| Medium | 4 | 500 ms | 5 % |
| Hard | 6 | 1500 ms | 0 % |
| Impossible | 12 | 3000 ms | 0 % |

See [UltimateXOX/docs/MINIMAX.md](UltimateXOX/docs/MINIMAX.md) for how the search works.

## Architecture

```
UI (JavaFX)  ──click──►  GameController  ──rules──►  UltimateBoard
     ▲                        │                          ▲
     └──── renderMove() ◄─────┘          MinimaxAI ◄─────┘ (board copy,
                                         background thread, generation guard)
```

```
UltimateXOX/src/main/java/com/ultimatexox/
├── app/Main.java            # thin launcher
├── app/ui/XoxoApp.java      # all JavaFX screens + interaction
├── app/ui/Theme.java        # shared styled components
├── game/UltimateBoard.java  # 81-cell board, rules state, legal moves
├── game/Rules.java          # pure rule/evaluation helpers
├── game/GameController.java # owns ONE match: modes, marks, undo history
├── ai/Difficulty.java       # Easy/Medium/Hard/Impossible parameters
├── ai/MinimaxAI.java        # the entire V1 AI (negamax + alpha-beta)
└── stats/Statistics.java    # local file persistence
```

Tests live in `UltimateXOX/src/test/java/com/ultimatexox/`:
`game/` (board, controller incl. first-move regression tests, coordinates,
render-state consistency) and `ai/` (legal moves, wins, blocks, terminal states).

## Documentation

Short and focused, inside `UltimateXOX/docs/`:

- [GAME_RULES.md](UltimateXOX/docs/GAME_RULES.md) — how Ultimate Tic-Tac-Toe works
- [MINIMAX.md](UltimateXOX/docs/MINIMAX.md) — how the AI searches moves
- [ARCHITECTURE.md](UltimateXOX/docs/ARCHITECTURE.md) — how UI → GameController → Board → AI fit together


## Contributing

Keep changes small, understandable and tested — build small → understand →
test → play → improve → version. See [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT — see [LICENSE](LICENSE).
