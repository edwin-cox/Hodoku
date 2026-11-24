# HoDoKu Kernel vs. Frontend Refactoring Analysis & Notes

## 1. Executive Summary

HoDoKu (`edwin-cox/Hodoku`) is a mature Java Swing desktop application (~65,000+ LOC across `src/generator/`, `src/solver/`, and `src/sudoku/`) originally designed with three packages (`generator`, `solver`, `sudoku`). While the **solver** and **generator** algorithms were already relatively modular, domain models, game-session orchestration, configuration, i18n formatting, and Swing UI classes were tightly intertwined—especially inside the `sudoku` package, which acted simultaneously as:
1. The core domain model (`Sudoku2`, `SolutionStep`, `SolutionType`, `StepConfig`, `DifficultyLevel`, `SudokuSet`),
2. The Swing GUI layer (`MainFrame`, `SudokuPanel`, `SolutionPanel`, `*Dialog`),
3. The global configuration & persistence singleton (`Options` via `XMLEncoder`/`XMLDecoder`),
4. The CLI / regression / batch runner (`Main`, `RegressionTester`).

**Phase 1** (implemented in this repository) introduces a UI-free `src/kernel/` package, severs direct Swing/AWT dependencies from `src/generator/`, `src/solver/`, and `Sudoku2`, and extracts headless game/search/evaluation engines (`SudokuGame`, `AllStepsFinder`, `BackdoorSearcher`, `ProgressEvaluator`, `SudokuCoreUtil`, `GameStateSnapshot`) behind listener interfaces while preserving full compatibility with the existing Swing UI and XML persistence.

> **Verification Note**: `java` / `javac` are not installed in this container environment (`which java javac` returned not found), so compilation could not be executed locally. All changes were audited statically across callers, method signatures, and imports.

---

## 2. Original Architecture & Interesting Findings

### 2.1 Surprising Existing Strengths
- **`Sudoku2` internal representation**: `Sudoku2` stores the board as fixed primitive arrays (`cells[81]`, `userValues[81]`, 16-bit candidate bitmasks `candidates[81]` + `userCandidates[81]`, `SudokuSet` bitsets for positions, and precomputed static lookup tables for rows/cols/blocks/buddies). It has **zero Swing imports** and uses an internal `candidatesAllowed` / `fixed` bit layout, making it fast and memory-compact for solver backtracking.
- **Hidden headless CLI already inside `sudoku.Main`**: Despite living in the UI package, `Main.java` (~2,268 lines) already implements headless CLI batch commands (`/bs`, `/bsaf`, `/bsa`, `/bt`, `/s`, `/sc`, `/sl`, `/so`, `/vg`, `/vf`, `/stdin`, `/test`, `/testf`, plus `RegressionTester`). However, running them still transitively pulled in `Options` (and therefore `java.awt.Color`, `Font`, and `GraphicsEnvironment`).
- **Bitwise `SudokuSetBase` / `SudokuSet` / `SudokuSetShort`**: The bitset operations in `sudoku` (`SudokuSet` using two `long`s for 81 cells) are completely UI-free and performance-tuned for constraint propagation.

### 2.2 Inverted & Circular Dependencies Discovered
1. **Circular dependency between `sudoku` and `generator`**:
   - `generator` naturally depends on `sudoku.Sudoku2` and `solver.SudokuSolverFactory`.
   - Unexpectedly, `sudoku.Sudoku2.setGivens(String)` directly imported `generator.SudokuGenerator` and `generator.SudokuGeneratorFactory` solely to check solution uniqueness (`getNumberOfSolutions(this, 2)`). This prevented `Sudoku2` from existing below `generator` in a clean dependency hierarchy.
2. **Algorithms trapped inside Swing Dialogs & Panels**:
   - **Backdoor Search (`BackdoorSearchDialog.java`)**: The entire combinatorial 1/2/3-cell and candidate backdoor search algorithm—including puzzle solving probes and candidate filtering—lived inside a `JDialog` subclass mutating `JProgressBar`, `JLabel`, and `DefaultListModel` directly on background threads.
   - **Exhaustive Step Finder (`FindAllSteps.java`)**: A pure solver orchestration class that lived in `sudoku/` and coupled directly to `FindAllStepsProgressDialog`, `EventQueue.invokeLater`, and `ResourceBundle.getBundle("intl/FindAllStepsProgressDialog")`.
   - **Background Difficulty Scorer (`ProgressChecker.java`)**: Directly held a reference to `MainFrame`, dispatched UI score/level label updates, and called `EventQueue.invokeLater` instead of returning an evaluation result.
   - **Game Loop & Undo/Redo (`SudokuPanel.java` + `MainFrame.java`)**: Core game state mutations (`setCell`, candidate toggles, undo/redo stacks, hint execution, `solveUpTo()` loop, and `GameMode.LEARNING` fast-forward setup skipping steps below the target training technique) were split across `SudokuPanel` and `MainFrame` alongside mouse listeners, clipboard paste handlers, and `Graphics2D` painting.
3. **UI Progress Dialogs coupled directly into `solver` & `generator`**:
   - `solver.SudokuSolver` imported `sudoku.SolverProgressDialog`, `sudoku.FindAllStepsProgressDialog`, and `sudoku.GuiState` (`solve(boolean)` launched a Swing modal dialog from inside the solver!).
   - `solver.FishSolver` and `solver.SudokuStepFinder` accepted `sudoku.FindAllStepsProgressDialog` directly in `getAllFishes(...)` / `getAllKrakenFishes(...)` and invoked `EventQueue`/`dialog.setStepLabel(...)`.
   - `generator.BackgroundGenerator` imported `java.awt.EventQueue` and `sudoku.GenerateSudokuProgressDialog`.
   - `solver.SimpleSolver`, `solver.FishSolver`, and `solver.SudokuSolver` depended on `sudoku.SudokuUtil`, which mixes pure math/step-list helpers (`combinations`, `clearStepList`, `getSSFormatted`) with `java.awt.Font`, `PrinterJob`, `PageFormat`, and `UIManager` Look-and-Feel helpers.

### 2.3 Bugs & Concurrency Hazards Uncovered During Extraction

1. **Bug in `BackdoorSearchDialog.java` line 850 (3-candidate output typo)**:
   - When formatting a 3-candidate backdoor, `orgOut` appended `cand2.getIndex()` twice (`cand1`, `cand2`, `cand2`) instead of `cand3.getIndex()`. Fixed in `kernel.BackdoorSearcher`.
2. **Latent Bug in `BackdoorSearchDialog.java` lines 807–816 (commented-out 2nd and 3rd cell assignments)**:
   - In `checkSingleOrCandidate(...)`, debug `System.out.println` statements had been wrapped in a `/* ... */` block comment that accidentally swallowed `sudoku.setCell(index2, ...)` and `sudoku.setCell(index3, ...)`. As a result, depth-2 and depth-3 *cell* backdoor searches only ever filled `index1`, making 2-cell and 3-cell backdoors behave incorrectly. Restored cleanly in `kernel.BackdoorSearcher`.
3. **Non-Thread-Safe Mutation of Global `Options.getInstance()` Singleton**:
   - Several solver/search methods temporarily mutate global `Options` flags and restore them at the end of the method:
     - `SudokuSolver.getProgressScore(...)` (mutates `checkTemplates` / `solverStepsProgress`),
     - `FishSolver.getAllFishes(...)` & `getAllKrakenFishes(...)` & `getKrakenFish(...)` (mutate `maxFins`, `maxEndoFins`, `checkTemplates`),
     - `AllStepsFinder` / `FindAllSteps` (temporarily flips `checkTemplates` around template steps).
   - If a background puzzle generator (`BackgroundGeneratorThread`), `ProgressEvaluator`, or `BackdoorSearcher` runs concurrently with interactive solving/hinting, these global mutations race with each other.
   - Note also: `FishSolver.getAllFishes` / `getAllKrakenFishes` previously leaked modified `Options` values if an exception or early interrupt occurred before restoration; moving to a `try/finally` or immutable `SolverConfig` (Phase 2) resolves both.

### 2.4 XML Serialization & God-Object Constraints (`Options`, `GuiState`, `DifficultyLevel`)
- **`sudoku.Options` (2,516 lines)** is serialized with `java.beans.XMLEncoder` / `XMLDecoder` (JavaBean property reflection). Changing package names or removing public getters/setters on persisted classes (`Options`, `DifficultyLevel`, `StepConfig`, `GuiState`) would break existing `*.hcfg` / `*.hsol` user configuration and savepoint files unless a translation layer or compatibility wrapper is maintained.
- `Options` currently bundles:
  - Solver heuristics & step weights (`StepConfig[] solverSteps`),
  - Background generator puzzle caches (`puzzles`, `learningPuzzles`, `practisingPuzzles`) and eagerly triggers `generator.BackgroundGeneratorThread.getInstance().resetAll(...)`,
  - UI aesthetics (`java.awt.Color`, `Font`, `GraphicsEnvironment.getLocalGraphicsEnvironment()`, `ResourceBundle.getBundle("intl/MainFrame")`, Look & Feel, ColorKu palette).
- **`DifficultyLevel`** holds `backgroundColor` and `foregroundColor` as `java.awt.Color`.
- **`SolutionType`** (enum) and **`SolutionStep`** call `ResourceBundle.getBundle(...)` statically and during `toString()`/`getStepName()`, and `SolutionStep` also queries `Options.getInstance().getFishDisplayMode()` and `Options.getInstance().getColoringColors().length`.
- **`Sudoku2`** still reads two convenience flags from `Options.getInstance()`: `isUseZeroInsteadOfDot()` (line 1200) and `isUseOrInsteadOfAndForFilter()` (line 1718).

---

## 3. Summary of Phase 1 Refactoring (Completed)

### 3.1 New `src/kernel/` Package (Zero Swing/AWT/ResourceBundle Dependencies)
All 12 files in `src/kernel/` are pure Java with zero references to `java.awt.*`, `javax.swing.*`, `ResourceBundle`, `MainFrame`, `SudokuPanel`, `SolutionPanel`, `SudokuUtil`, `GuiState`, or dialogs:

| File | Responsibility |
|---|---|
| `SolverProgressListener.java` | Progress callback (`onStepProgress`, `isCancelled`) for `SudokuSolver.solve(SolverProgressListener)` |
| `StepSearchStage.java` | Typed stage enum (`SIMPLE_STEPS` .. `PROGRESS_SCORE`) replacing UI string keys inside solver/all-steps orchestration |
| `StepSearchProgressListener.java` | Headless progress callback for `AllStepsFinder`, `FishSolver`, and `SudokuSolver.getProgressScore` |
| `GenerationProgressListener.java` | Attempt counter callback for `BackgroundGenerator` |
| `SolutionCounter.java` | `@FunctionalInterface` (`countSolutions(Sudoku2, int)`) breaking `Sudoku2` -> `generator` circularity |
| `SudokuCoreUtil.java` | Pure utilities extracted from `SudokuUtil` (`NEW_LINE`, `clearStepListWithNullify`, `clearStepList`, `combinations`, `getSSFormatted`, `getSSPMGrid`) |
| `ProgressEvaluator.java` | Headless background difficulty/score evaluator extracted from `ProgressChecker` |
| `BackdoorSearchListener.java` | Structured callback interface (`SearchKind`, `MeasureKind`, progress, hit notification, completion) |
| `BackdoorSearcher.java` | Headless 1/2/3-cell & candidate backdoor engine extracted from `BackdoorSearchDialog` (with the 2 bugs fixed) |
| `AllStepsFinder.java` | Pure exhaustive step finder extracted from `FindAllSteps` without `EventQueue` or dialogs |
| `GameStateSnapshot.java` | Headless save/restore state DTO (`sudoku`, `initialState`, `undoStack`, `redoStack`, `name`, `timestamp`) |
| `SudokuGame.java` | Headless game session controller (`Sudoku2`, undo/redo stacks, cell/candidate edits, hint lookup, `applyStep`, `solveUpTo`, `prepareLearningMode`, progress/validity check, snapshot export/restore) |

### 3.2 Decoupling Across `generator`, `solver`, and `sudoku`
- **`generator/BackgroundGenerator.java`**: Removed `java.awt.EventQueue` and `GenerateSudokuProgressDialog`; accepts `GenerationProgressListener`.
- **`generator/SudokuGeneratorFactory.java`**: Static initializer registers `Sudoku2.setDefaultSolutionCounter(...)` so `Sudoku2.setGivens` works seamlessly whenever `generator` is loaded, without `Sudoku2` importing `generator.*`.
- **`sudoku/Sudoku2.java`**: Removed `generator.SudokuGenerator` / `SudokuGeneratorFactory` imports; `setGivens` uses the injected `SolutionCounter`.
- **`solver/SudokuSolver.java`**: Removed `solve(boolean)`, `SolverProgressDialog`, `FindAllStepsProgressDialog`, `GuiState`, `SudokuUtil`, and `getState`/`setState`; uses `SolverProgressListener`, `StepSearchProgressListener`, and `SudokuCoreUtil`.
- **`solver/FishSolver.java` & `solver/SudokuStepFinder.java`**: Replaced `FindAllStepsProgressDialog` parameters with `StepSearchProgressListener`.
- **`solver/SimpleSolver.java`**: Replaced `SudokuUtil` with `SudokuCoreUtil`.
- **Swing Adapter Updates**:
  - `SolverProgressDialog` implements `SolverProgressListener` and hosts `SolverProgressDialog.solve(SudokuSolver)` (callers updated in `SudokuPanel` and `SolutionPanel`).
  - `FindAllStepsProgressDialog` implements `StepSearchProgressListener` and translates `StepSearchStage` to localized `ResourceBundle` labels on the EDT.
  - `FindAllSteps` is now a thin compatibility subclass extending `kernel.AllStepsFinder`.
  - `GenerateSudokuProgressDialog` implements `GenerationProgressListener`.
  - `ProgressChecker` delegates calculation to `kernel.ProgressEvaluator` and posts UI updates to `MainFrame` on the EDT.
  - `BackdoorSearchDialog` delegates search execution to `kernel.BackdoorSearcher` via `BackdoorSearchListener`.
  - `GuiState` reads/writes `SudokuSolver` fields directly and adds `toSnapshot()` / `applySnapshot(GameStateSnapshot)`.
  - `SudokuUtil` delegates pure methods to `kernel.SudokuCoreUtil`.
  - `SudokuPanel.solveUpTo()` delegates its solver loop to `SudokuGame.solveUpTo(...)`, and `MainFrame` delegates `GameMode.LEARNING` fast-forward setup to `SudokuGame.prepareLearningMode(...)`.

---

## 4. Recommended Phase 2 Roadmap (Full Physical Module Split)

To physically split HoDoKu into independent Maven/Gradle modules (`hodoku-core` with zero `java.desktop` / `java.awt` / `javax.swing` dependency vs. `hodoku-swing`), address these remaining ties in order:

1. **Extract `SolverConfig` & Split `Options`**:
   - Create an immutable/copyable `kernel.SolverConfig` holding only solver flags, `StepConfig[]`, and `DifficultyLevel[]` metrics (`maxScore`, `name`, `ordinal`) without `Color`, `Font`, `GraphicsEnvironment`, `ResourceBundle`, or `BackgroundGeneratorThread`.
   - Have `SudokuSolver`, `FishSolver`, `SingleDigitPatternSolver`, `AlsSolver`, `ChainSolver`, `TableEntry`, and `SudokuStepFinder` read from a per-solver `SolverConfig` instance rather than the mutable global `Options.getInstance()` singleton. This simultaneously eliminates all `Options.getInstance()` data races under multithreaded execution.
   - Keep `sudoku.Options` as the Swing-facing JavaBean wrapper for `XMLEncoder`/`XMLDecoder` backwards compatibility, syncing changes into `SolverConfig`.

2. **Decouple `DifficultyLevel`, `SolutionType`, and `SolutionStep` from AWT & `ResourceBundle`**:
   - Remove `java.awt.Color` (`backgroundColor`, `foregroundColor`) from `DifficultyLevel` into a UI theme map keyed by `DifficultyType`.
   - Separate enum/step data (`SolutionType`, `SolutionStep`) from localized rendering by introducing a `StepFormatter` in `sudoku` (owning `intl/SolutionType` and `intl/SolutionStep` `ResourceBundle` lookups and Fish/Coloring display formatting), leaving `toString()` in core purely structural or delegating to an optional formatter strategy.

3. **Remove Remaining `Options` Reads in `Sudoku2`**:
   - Parameterize `Sudoku2.getSudoku(ClipboardMode, SolutionStep, boolean useZeroInsteadOfDot)` and `getCandidateCells(..., boolean useAndForFilter)` (with existing no-arg overloads defaulting or injected via configuration) so `Sudoku2` has zero compile-time dependency on `Options`.

4. **Move Core Domain Types into `kernel` / `hodoku-core`**:
   - Once `Options`, `DifficultyLevel`, `SolutionType`, `SolutionStep`, and `Sudoku2` are AWT/Swing/ResourceBundle-free, relocate the pure domain classes (`Sudoku2`, `SudokuSet*`, `SudokuCell`, `Candidate`, `Entity`, `Chain`, `SudokuSinglesQueue`, `SolutionStep`, `SolutionType`, `SolutionCategory`, `StepConfig`, `DifficultyLevel`, `DifficultyType`, `GameMode`, `SudokuStatus`, `ClipboardMode`, `AlsInSolutionStep`) plus `generator` and `solver` into a standalone `hodoku-core` module that compiles with `--limit-modules java.base,java.logging` (zero `java.desktop`).
   - Keep `RegressionTester` and CLI commands (`Main` batch subcommands) in a headless `hodoku-cli` entrypoint referencing only `hodoku-core`, completely detached from Swing.
