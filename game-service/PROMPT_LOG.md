# prompt_log.md — Refactoring *Connect Four* from Java → Python (LLM prompt pack)

**Goal.** Convert the provided Java `ConnectFour` into clean, testable Python modules that separate **state** and **logic**, are **deterministic** (no I/O, no randomness), and can power an AI later.

> Your current Python structure already hits this target with `models.py` (state) and `service.py` (logic), and optional module wiring in `__init__.py`. fileciteturn0file1 fileciteturn0file2 fileciteturn0file3

---

## 0) Global system/developer message (pin this once)

**ROLE (system):**  
You are a senior Python engineer. Convert legacy Java to Python using modern, idiomatic patterns. Keep I/O out of the game core. Preserve gameplay rules faithfully. Favor clarity, small pure functions, and exhaustive tests.

**CONSTRAINTS (developer):**
- Board is **6×7**, empty cells are `"."`, players are `"X"` and `"C"`.  
- No console input/output in the core. No `print`, no `input`.  
- Separate **state** and **logic** modules. Use `dataclasses` and `Enum`.  
- Provide deterministic functions for:  
  `get_legal_moves(state)`, `apply_move(state, column)`, `check_winner(state)`, `is_draw(state)`.
- New state objects should be returned on moves (no in‑place hidden mutations of external state).

> Reference Java source for correct rules and winning patterns. fileciteturn0file0

---

## 1) Source analysis prompt

**PROMPT (user):**  
“Analyze this Java file and enumerate all rules, constants, and behaviors that must be preserved in Python. Output a bullet list grouped by: *board geometry*, *tokens*, *move semantics*, *win detection (4 directions)*, *draw condition*, *turn switching*, *I/O that must be removed*. Then propose a minimal Python API.  


<PASTE `ConnectFour.java` HERE>”  fileciteturn0file0

**SUCCESS CRITERIA:** The model lists: ROWS=6, COLS=7, empty `"."`, players `'X'` & `'C'`, bottom-up drop, same four-in-a-row checks (horizontal, vertical, \ diagonal, / diagonal), draw when top row full, turn toggle, and removes console I/O.

---

## 2) Data model design prompt (state only)

**PROMPT (user):**  
“Design a Python `models.py` that holds **only data** and enums. Include:
- `Player` (`X`, `C`), `Cell` (`.`, `X`, `C`), `GameStatus` (ongoing/win_p1/win_p2/draw)
- `ROWS=6`, `COLS=7`
- `GameState` dataclass with: `board: List[List[Cell]]`, `current_player: Player`, `status: GameStatus=ONGOING`, `last_move_col: Optional[int]=None`, `move_number:int=0`  
Add concise docstrings and type hints.”

**REFERENCE SHAPE:** Your current `models.py` already matches this structure. fileciteturn0file1

**SUCCESS CRITERIA:** Pure datamodel, no logic, correct enums and defaults, 6×7 board type.

---

## 3) Game logic service prompt

**PROMPT (user):**  
“Implement a `GameEngineService` in `service.py` with:
- `new_game(starting_player=Player.P1) -> GameState` (allocates 6×7 `Cell.EMPTY`)
- `get_legal_moves(state) -> List[int]` (columns whose **top cell** is empty)
- `_drop_piece(board, column, player) -> int` (returns landing row; raises if full)
- `_winner_for(board) -> Optional[Player]` via four helpers:
  `_check_horizontal_win`, `_check_vertical_win`, `_check_diagonal_down_right_win`, `_check_diagonal_up_right_win`  
  Each mirrors the Java loops and comparisons.
- `_determine_game_status(board) -> GameStatus` (WIN_P1|WIN_P2|DRAW|ONGOING)
- `_is_board_full(board) -> bool` (check top row only)
- `_get_next_player(current_player, status) -> Player` (no switch after terminal)
- `apply_move(state, column) -> GameState` (validate, copy board, drop, recompute status, increment move, set `last_move_col`, switch player iff ongoing)
- `check_winner(state) -> Optional[Player]`
- `is_draw(state) -> bool`

Keep functions deterministic. No prints. Raise `ValueError` for illegal moves.”

**REFERENCE:** Your current `service.py` fulfills this spec and mirrors Java’s win checks. fileciteturn0file2 fileciteturn0file0

**SUCCESS CRITERIA:** Logic parity with Java, small helpers, clear exceptions, returns a **new** `GameState`.

---

## 4) Optional module wiring / API prompt

**PROMPT (user):**  
“Provide a minimal `__init__.py` or module setup that exposes the public API (`GameState`, `Player`, `Cell`, `GameStatus`, `GameEngineService`) and, if needed, registers the service in a DI container and returns a FastAPI router.”

**REFERENCE:** Your `__init__.py`-style module already demonstrates DI/Router setup and public exports. fileciteturn0file3

---

## 5) Unit test generation prompt

**PROMPT (user):**  
“Write pytest tests that *do not* import the Java file. Cover:
- new game invariants (empty top row; current player; status=ONGOING)
- legal moves on empty vs. partially filled columns
- gravity (multiple drops stack from bottom)
- four wins (—, |, \ , /)
- draw (full top row without winners)
- illegal move (full column) raises ValueError
- `apply_move` returns a **new** state object; old state unchanged.”

**SUCCESS CRITERIA:** Tests pass and document behavior clearly.

---

## 6) Property checks prompt (optional but strong)

**PROMPT (user):**  
“Add lightweight property-based tests (e.g., with `hypothesis`) for invariants:
- After any legal move: exactly one more non-empty cell; current player toggles iff status=ONGOING; no floating tokens (each token rests on bottom or another token).”

---

## 7) Refactor & quality prompts

**PROMPT (user):**  
“Refactor for clarity:
- Keep private helpers for each win direction (readability > micro-optimizations)
- Keep type hints, docstrings, and small cyclomatic complexity per function
- Ensure `_is_board_full` only inspects row 0 (top) like the Java draw check.” fileciteturn0file0

---

## 8) Error handling prompts

**PROMPT (user):**  
“Ensure all invalid inputs cause explicit `ValueError` with helpful messages:
- `apply_move` on terminal state
- Column index out of range
- Playing in a full column.”

---

## 9) Serialization prompt (optional)

**PROMPT (user):**  
“Provide pure serializers: `state_to_dict(state)`, `state_from_dict(d)` using enum `.value` for transport, without tying to FastAPI.”

---

## 10) AI player stub prompt (optional)

**PROMPT (user):**  
“Create `ai_player` module with a pure `choose_move(state)` baseline (e.g., pick center if legal, else highest-scoring from a heuristic), no randomness. Never modify state in-place.”

---

## 11) Logging prompt (optional)

**PROMPT (user):**  
“Add a tiny logger utility (`game_logger`) that can record moves and outcomes, but keep it outside the core logic; accept injected logger or `None`.”

---

## 12) One-shot meta prompt (if you want to regenerate everything)

**PROMPT (user):**  
“You will receive a Java *Connect Four* implementation. Produce: (1) `models.py`, (2) `service.py`, (3) `tests/test_game.py`, and (4) optional `__init__.py` with DI/router wiring. Follow constraints in section **0**. Respect data/logic separation. Cite all rules you inferred from the Java (board size, tokens, win patterns) and ensure parity. Do not include any console I/O.”

---

## Appendix A — Parity cues to watch for

- Horizontal/vertical/diagonal loops match Java bounds and directions. fileciteturn0file0  
- Draw detection relies on **top row occupancy**, not entire grid scan (perf + parity). fileciteturn0file0  
- Your Python already mirrors these details precisely. fileciteturn0file2

---

## Appendix B — Minimal commit plan

1. `feat(game): add datamodel (enums + GameState)`  
2. `feat(game): implement GameEngineService with four win checks`  
3. `test(game): add deterministic unit tests`  
4. `chore: add optional DI/router wiring`

---

**Done.** This prompt pack lets you reproduce or audit the Java→Python refactor with repeatable, high-signal LLM prompts while keeping your current structure intact.
