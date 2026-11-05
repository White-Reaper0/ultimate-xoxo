<div align="center">

# ✕ ◯ ✕ ◯ &nbsp; **ULTIMATE XOXO**

### *You don't choose where to play. Your opponent does.*

**A native Java desktop game of Ultimate Tic-Tac-Toe — where every mark you place is also a sentence you speak to your rival, and every sentence forces their reply.**

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-0073A7?logo=java&logoColor=white)](https://openjfx.io/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit](https://img.shields.io/badge/tests-JUnit%205-red)](#-tests)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Offline](https://img.shields.io/badge/offline-100%25-111111?logo=wifi&logoColor=red)](#-zero-runtime-dependencies)

[The Twist](#-the-twist) · [Modes](#-modes) · [The Opponent](#-the-opponent) · [Quick Start](#-enter-the-arena) · [Architecture](#-architecture) · [Docs](#-docs)

</div>

---

## ☄️ The Hook

```
   YOU play cell 8 of board 4 (bottom-right corner)
        │
        └──────►  your OPPONENT is now forced into board 8
                                      │
                                      └──────►  board 8 is already won by you
                                                 │
                                                 └────► the cage door swings open:
                                                        they may now play ANYWHERE
                                                        — including the board
                                                        you were saving for yourself.
```

Regular Tic-Tac-Toe is a coin-flip of patience. **XOXO is a game of speech acts.** You don't just claim a cell — you *send your opponent somewhere*. Send them into a corner they can't escape. Send them into a board you already control. Win the wrong square and you've just handed them a golden road. Lose a board on purpose and you might still win the war.

**Nine boards. One meta-board. Eighty-one cells.** Three claimed boards in a line on the meta-board end the match instantly — and an opponent that looks **up to 12 plies ahead** is quietly counting every one of your sentences before you finish saying it.

> **Version 1 philosophy:** *working · stable · simple · understandable · easy to upgrade.*
> No neural networks. No MCTS. No black boxes. Just a sharp, honest opponent and code a CS student can open, read in an afternoon, and make better.

---

## 🎭 The Twist

| | |
|---|---|
| 🎯 **Your move aims their next move** | The cell you pick decides the board your opponent is *forced* into. Offense and defense are the same gesture. |
| 🧩 **Small wins build a big win** | Capture individual boards to line up three on the meta-board. Every local victory is a move in a slower, deadlier game. |
| 🚪 **A full or finished board frees your opponent** | Send them to a closed board and the constraint vanishes — they can play *anywhere*. Be very careful what you hand over. |
| 🪤 **Traps compound** | A "safe" board can become a prison. A lost board can become a launcher. Nothing on the grid stays innocent. |
| 🤝 **Draws are real** | Drawn boards count for no one — and whole games can lock up into stalemates. |

📖 **Full rulebook:** [`docs/GAME_RULES.md`](docs/GAME_RULES.md)

---

## 🎬 A Match, In Three Beats

**1. You take the centre.** The engine now *must* answer in the centre board.

**2. It answers in cell 0 — top-left.** Now *you* must play in board 0. Notice what just happened: your opening move bought you the centre, and its reply bought you the corner.

**3. You trade territory.** Each of you is simultaneously playing the board in front of you and *designing the board your opponent will face next*. The meta-game is not an abstraction layered on top — it **is** the game.

This is why the first three moves feel like small talk and the fourth feels like a knife.

---

## 🕹️ Modes

| Mode | The experience |
|---|---|
| 🧠 **Human vs Computer** | You against the engine, with **four levels of cruelty**. |
| 🤝 **Human vs Human** | Pass-and-play rivalry on one device. No server, no account, no waiting. |
| 🍿 **AI vs AI** | Pure spectator mode. Pour a drink and watch two engines outthink each other into the ground. |

**Who strikes first?** `Human First` · `AI First` · `Random` — fully reliable, no surprises.

Flow: *Splash → Main Menu → New Game → mode → difficulty + who goes first → play.*

---

## 🧠 The Opponent

**One file.** `MinimaxAI.java` — negamax search, alpha-beta pruning, iterative deepening against a clock, instant-win detection and clean move ordering. Difficulty tunes exactly three knobs:

| Level | Depth | Think time | Random slips |
|:---|:---:|:---:|:---:|
| 🟢 **Easy** | 2 | 100 ms | 30 % |
| 🟡 **Medium** | 4 | 500 ms | 5 % |
| 🟠 **Hard** | 6 | 1500 ms | 0 % |
| 🔴 **Impossible** | 12 | 3000 ms | 0 % |

> Even Easy takes an immediate win when it sees one — so weak levels stay *fun* instead of *blind*.

🔍 **Want to watch it think?** → [`docs/MINIMAX.md`](docs/MINIMAX.md) — the whole search explained in plain language, one paragraph per idea.

---

## ✨ Features

- 🏆 **Complete rules** — forced boards, small-board wins *and* draws, meta-board wins, full-game draws, illegal-move rejection
- ⏪ **Undo** — rewinds your move *and* the AI's reply, back to your true decision point
- 💡 **Hint** — the very same engine analyses your position in the background, outlines its best cell and shows you the evaluation
- 🔄 **Restart** — one click resets board, turn, forced board, history and UI
- 📊 **Persistent stats** — games, wins, losses, draws and win rate, saved to `~/.ultimate-xox/stats.properties`
- 👁️ **Readable at a glance** — bold X/O, an amber glow on the last move, green outlines on the board(s) you may legally play, an ownership badge on every small board (`=` means drawn)
- 🪟 **Responsive window** — resize or maximize freely; size and state survive screen changes

### 📴 Zero Runtime Dependencies
No API keys. No accounts. No network calls. No telemetry. No "phone home". The game plays itself on your machine or it doesn't play at all.

---

## 🚀 Enter the Arena

**You need:** JDK **17+** (developed and tested on JDK 26) and Maven **3.9+**.

```bash
git clone https://github.com/White-Reaper0/ultimate-xoxo.git
cd ultimate-xoxo
mvn javafx:run
```

```bash
mvn test              # run the test suite
mvn clean package     # build the project
```

> 🔜 A double-clickable `UltimateXOX.exe` via `jpackage` is planned once the app is stable (post-V1).

---

## 🎮 Controls

| Button | Action |
|---|---|
| **Undo** | Takes back your last move (+ the AI reply in Human vs AI) |
| **Hint** | Analyses in the background and outlines the suggested cell |
| **Restart** | Completely resets the match |
| **← Menu** | Back to the menu; any running AI search is safely discarded |

```
🟩 Green outline = you may play here
🟨 Amber glow    = last move
🏅 Corner badge  = owner of that small board   (= means drawn)
```

---

## 🏛️ Architecture

```
UI (JavaFX)  ──click──►  GameController  ──rules──►  UltimateBoard
     ▲                        │                          ▲
     └──── renderMove() ◄─────┘          MinimaxAI ◄─────┘  (board copy,
                                         background thread, generation guard)
```

The UI never owns game truth. `GameController` owns exactly **one** match. The AI searches an immutable *copy* of the board on a background thread, guarded by a generation counter — so a new click invalidates any stale search instantly and the EDT never blocks.

```text
src/main/java/com/ultimatexoxo/
├── app/Main.java             # thin launcher
├── app/ui/XoxoApp.java       # all JavaFX screens + interaction
├── app/ui/Theme.java         # shared styled components
├── game/UltimateBoard.java   # 81-cell board, rules state, legal moves
├── game/Rules.java           # pure rule/evaluation helpers
├── game/GameController.java  # owns ONE match: modes, marks, undo history
├── game/GameState.java       # immutable snapshot for undo / AI search
├── ai/Difficulty.java        # Easy/Medium/Hard/Impossible parameters
├── ai/MinimaxAI.java         # the entire V1 AI (negamax + alpha-beta)
└── stats/Statistics.java     # local file persistence
```

### 🧪 Tests
Live in `src/test/java/com/ultimatexox/`:

- `game/` — board rules, controller (incl. first-move regression tests), move coordinates, render-state consistency
- `ai/` — legal move generation, win detection, blocking, terminal states

---

## 📚 Docs

All short and focused, all in [`docs/`](docs/):

| File | What it answers |
|---|---|
| [`GAME_RULES.md`](docs/GAME_RULES.md) | How the game works |
| [`MINIMAX.md`](docs/MINIMAX.md) | How the AI searches |
| [`ARCHITECTURE.md`](docs/ARCHITECTURE.md) | How UI → GameController → Board → AI fit together |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | How to change it without breaking it |
| [`SECURITY.md`](SECURITY.md) | How to report a problem |

---

## 🛠️ Build It Better

Think you can make the AI sharper or the UI prettier? Good. The codebase is deliberately small, deliberately readable, and deliberately *yours*:

> **build small → understand → test → play → improve → version.**

Read [`CONTRIBUTING.md`](CONTRIBUTING.md), keep the diff honest, and ship it.

---

## 📄 License

**MIT.** See [`LICENSE`](LICENSE).

<div align="center">
<br>

**Open it. Make one careless move. Watch the whole board rearrange itself around you.**

</div>
