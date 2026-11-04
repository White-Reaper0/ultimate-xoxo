✕ ◯ ✕ ◯  ULTIMATE XOXO
You don't choose where to play. Your opponent does.
Every move you make is also a command. Place a mark in the top-right corner of a small board, and your opponent must answer in the top-right board. Win the wrong square and you've just sent them somewhere deadly. Lose a board on purpose and you might win the war.
Nine boards. One meta-board. Eighty-one cells. And an opponent that looks up to 12 moves ahead.
Ultimate XOXO is a native Java desktop game of Ultimate Tic-Tac-Toe. It runs fully offline, with no accounts, no servers and no network, and it's built on a Minimax AI with alpha-beta pruning you can read in a single file.
Version 1 philosophy: working · stable · simple · understandable · easy to upgrade.
No neural networks. No MCTS. No black boxes. Just a sharp, honest opponent and code a CS student can open, understand and make better.
🎭 The Twist
Regular Tic-Tac-Toe is a coin-flip of patience. XOXO is a game of traps:
🎯 Your move aims their next move. The cell you pick decides the board they're forced into.
🧩 Small wins build a big win. Capture boards to line up three on the meta-board.
🚪 A full or finished board frees your opponent. Send them to a closed board and they can play anywhere. Be careful what you hand over.
🤝 Draws are real. Drawn boards count for no one, and whole games can lock up.
Full rulebook: GAME_RULES.md
Mode
The experience
🧠 Human vs Computer
You against the engine, with four levels of cruelty
🤝 Human vs Human
Pass-and-play rivalry on one device
🍿 AI vs AI
Pure spectator mode: watch two engines outthink each other
Who strikes first? Human First, AI First, or Random. Fully reliable, no surprises.
🧠 The Opponent
One file. Negamax search, alpha-beta pruning, iterative deepening against a clock, smart move ordering and a clean heuristic. Difficulty tunes exactly three knobs:
Level
Depth
Think time
Random slips
🟢 Easy
2
100 ms
30 %
🟡 Medium
4
500 ms
5 %
🟠 Hard
6
1500 ms
0 %
🔴 Impossible
12
3000 ms
0 %
Want to see how it thinks? → MINIMAX.md
✨ Features
🏆 Complete rules: forced boards, small-board wins and draws, meta-board wins, full-game draws
⏪ Undo: rewinds your move and the AI's reply, back to your decision point
💡 Hint: the same engine analyses the position in the background, outlines its best cell and shows an evaluation
🔄 Restart: one click resets board, turn, forced board, history and UI
📊 Persistent stats: games, wins, losses, draws and win rate, saved to %USERPROFILE%\.ultimate-xox\stats.properties
👁️ Readable at a glance: bold X/O, an amber glow on the last move, green outlines on the board(s) you can play
🪟 Responsive window: resize or maximize freely; size and state survive screen changes
📴 Zero runtime dependencies: no API keys, no network
🚀 Enter the Arena
You need: JDK 17+ (developed and tested on JDK 26) and Maven 3.9+
cd UltimateXOX
mvn javafx:run
Splash screen → main menu → New Game → mode → (difficulty + who goes first) → play.
🔜 A double-clickable UltimateXOX.exe via jpackage is planned once the app is stable (post-V1).
🎮 Controls
Button
Action
Undo
Takes back your last move (+ the AI reply in Human vs AI)
Hint
Analyses in the background and outlines the suggested cell
Restart
Completely resets the match
← Menu
Back to the menu; any running AI search is safely discarded
🟩 Green outline = you may play here · 🟨 Amber glow = last move · The corner badge on each small board shows its owner (= means drawn).
UI (JavaFX)  ──click──►  GameController  ──rules──►  UltimateBoard
     ▲                        │                          ▲
     └──── renderMove() ◄─────┘          MinimaxAI ◄─────┘ (board copy,
                                         background thread, generation guard)
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
Tests live in UltimateXOX/src/test/java/com/ultimatexox/:
game/ (board, controller incl. first-move regression tests, coordinates, render-state consistency) and ai/ (legal moves, wins, blocks, terminal states).
All in UltimateXOX/docs/, short and focused:
GAME_RULES.md: how the game works
MINIMAX.md: how the AI searches
ARCHITECTURE.md: how UI → GameController → Board → AI fit together
🛠️ Build It Better
Think you can make the AI sharper or the UI prettier? Keep changes small, understandable and tested:
build small → understand → test → play → improve → version.
See CONTRIBUTING.md.
📄 License
MIT. See LICENSE.