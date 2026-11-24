/*
 * Copyright (C) 2008-12  Bernhard Hobiger
 *
 * This file is part of HoDoKu.
 *
 * HoDoKu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HoDoKu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with HoDoKu. If not, see <http://www.gnu.org/licenses/>.
 */
package kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import solver.SudokuSolver;
import sudoku.ClipboardMode;
import sudoku.DifficultyLevel;
import sudoku.DifficultyType;
import sudoku.GameMode;
import sudoku.Options;
import sudoku.SolutionStep;
import sudoku.Sudoku2;
import sudoku.SudokuStatus;

/**
 * Headless game session kernel managing board state, undo/redo history, move
 * mutations, hint stepping, training fast-forward, and solve-up-to logic
 * without any Swing/AWT dependencies.
 */
public class SudokuGame {

	private Sudoku2 sudoku;
	private Stack<Sudoku2> undoStack = new Stack<Sudoku2>();
	private Stack<Sudoku2> redoStack = new Stack<Sudoku2>();
	private SolutionStep activeStep;
	private int chainIndex = -1;
	private final SudokuSolver solver;
	private final SolutionCounter solutionCounter;

	public SudokuGame(SudokuSolver solver, SolutionCounter solutionCounter) {
		this.solver = solver;
		this.solutionCounter = solutionCounter;
		this.sudoku = new Sudoku2();
		this.sudoku.clearSudoku();
	}

	public Sudoku2 getSudoku() {
		return sudoku;
	}

	public void setSudoku(Sudoku2 sudoku) {
		this.sudoku = sudoku;
	}

	public SudokuSolver getSolver() {
		return solver;
	}

	public Stack<Sudoku2> getUndoStack() {
		return undoStack;
	}

	public Stack<Sudoku2> getRedoStack() {
		return redoStack;
	}

	public SolutionStep getActiveStep() {
		return activeStep;
	}

	public void setActiveStep(SolutionStep activeStep) {
		this.activeStep = activeStep;
	}

	public int getChainIndex() {
		return chainIndex;
	}

	public void setChainIndex(int chainIndex) {
		this.chainIndex = chainIndex;
	}

	public boolean canUndo() {
		return !undoStack.isEmpty();
	}

	public boolean canRedo() {
		return !redoStack.isEmpty();
	}

	public void clearUndoRedo() {
		undoStack.clear();
		redoStack.clear();
	}

	/**
	 * Restores the previous board state from the undo stack.
	 *
	 * @return {@code true} if an undo occurred
	 */
	public boolean undo() {
		if (!canUndo()) {
			return false;
		}
		redoStack.push(sudoku);
		sudoku = undoStack.pop();
		return true;
	}

	/**
	 * Reapplies a board state from the redo stack.
	 *
	 * @return {@code true} if a redo occurred
	 */
	public boolean redo() {
		if (!canRedo()) {
			return false;
		}
		undoStack.push(sudoku);
		sudoku = redoStack.pop();
		return true;
	}

	/**
	 * Loads a puzzle string, computes uniqueness/validity status, and optionally
	 * solves it via {@link #solver} to populate initial difficulty and score.
	 *
	 * @param init          puzzle string (null or empty resets to blank grid)
	 * @param alreadySolved if true, skips full initial solve
	 * @return resulting {@link SudokuStatus} (also stored on {@link #sudoku})
	 */
	public SudokuStatus loadPuzzle(String init, boolean alreadySolved) {
		activeStep = null;
		chainIndex = -1;
		clearUndoRedo();

		if (init == null || init.isEmpty()) {
			sudoku = new Sudoku2();
			return sudoku.getStatus();
		}

		sudoku.setSudoku(init);
		sudoku.setLevel(Options.getInstance().getDifficultyLevels()[DifficultyType.EASY.ordinal()]);
		sudoku.setScore(0);
		Sudoku2 tmpSudoku = sudoku.clone();
		if (!alreadySolved) {
			solver.setSudoku(tmpSudoku);
		}
		int anzSolutions = solutionCounter.countSolutions(sudoku, 1);
		if (anzSolutions == 0) {
			sudoku.setStatus(SudokuStatus.INVALID);
		} else if (anzSolutions > 1) {
			sudoku.setStatus(SudokuStatus.MULTIPLE_SOLUTIONS);
		} else if (!sudoku.checkSudoku()) {
			sudoku.setStatus(SudokuStatus.INVALID);
		} else {
			sudoku.setStatus(SudokuStatus.VALID);
			if (sudoku.getFixedCellsAnz() > 17) {
				Sudoku2 fixedOnly = new Sudoku2();
				fixedOnly.setSudoku(sudoku.getSudoku(ClipboardMode.CLUES_ONLY));
				int anzFixedSol = solutionCounter.countSolutions(fixedOnly, 1);
				sudoku.setStatusGivens(anzFixedSol);
			}
			if (!alreadySolved) {
				tmpSudoku.setStatus(SudokuStatus.VALID);
				tmpSudoku.setStatusGivens(sudoku.getStatusGivens());
				tmpSudoku.setSolution(sudoku.getSolution());
				solver.solve();
			}
			sudoku.setLevel(solver.getSudoku().getLevel());
			sudoku.setScore(solver.getSudoku().getScore());
		}
		return sudoku.getStatus();
	}

	/**
	 * Sets a cell's value without touching undo history (for batch updates where
	 * the caller pushes a single undo snapshot).
	 *
	 * @return {@code true} if the cell changed
	 */
	public boolean setCellValueDirect(int row, int col, int number) {
		int index = Sudoku2.getIndex(row, col);
		if (!sudoku.isFixed(index) && sudoku.getValue(index) != number) {
			if (sudoku.getValue(index) != 0) {
				sudoku.setCell(row, col, 0);
			}
			sudoku.setCell(row, col, number);
			return true;
		}
		return false;
	}

	/**
	 * Sets a cell's value as a single undoable move.
	 *
	 * @return {@code true} if the board changed
	 */
	public boolean setCellValue(int row, int col, int number) {
		undoStack.push(sudoku.clone());
		boolean changed = setCellValueDirect(row, col, number);
		if (changed) {
			redoStack.clear();
		} else {
			undoStack.pop();
		}
		return changed;
	}

	/**
	 * Toggles a candidate across a selection of cells (or a single cell if
	 * {@code cells} is empty), updating undo/redo stacks.
	 */
	public boolean toggleCandidate(List<Integer> cells, int fallbackRow, int fallbackCol, int candidate,
			boolean userCandidates) {
		undoStack.push(sudoku.clone());
		boolean changed = false;
		if (cells == null || cells.isEmpty()) {
			int index = Sudoku2.getIndex(fallbackRow, fallbackCol);
			if (sudoku.getValue(index) == 0) {
				boolean present = sudoku.isCandidate(index, candidate, userCandidates);
				sudoku.setCandidate(index, candidate, !present, userCandidates);
				changed = true;
			}
		} else {
			boolean candPresent = false;
			for (int index : cells) {
				if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, userCandidates)) {
					candPresent = true;
					break;
				}
			}
			for (int index : cells) {
				if (candPresent) {
					if (sudoku.getValue(index) == 0 && sudoku.isCandidate(index, candidate, userCandidates)) {
						sudoku.setCandidate(index, candidate, false, userCandidates);
						changed = true;
					}
				} else {
					if (sudoku.getValue(index) == 0 && !sudoku.isCandidate(index, candidate, userCandidates)) {
						sudoku.setCandidate(index, candidate, true, userCandidates);
						changed = true;
					}
				}
			}
		}
		if (changed) {
			redoStack.clear();
		} else {
			undoStack.pop();
		}
		return changed;
	}

	/**
	 * Finds the next hint step for the current board and stores it as {@link #activeStep}.
	 */
	public SolutionStep findNextHint(boolean singlesOnly) {
		activeStep = solver.getHint(sudoku, singlesOnly);
		chainIndex = -1;
		return activeStep;
	}

	/**
	 * Executes the specified {@link SolutionStep} as an undoable move.
	 */
	public boolean applyStep(SolutionStep stepToApply) {
		if (stepToApply == null) {
			return false;
		}
		undoStack.push(sudoku.clone());
		redoStack.clear();
		solver.doStep(sudoku, stepToApply);
		activeStep = null;
		chainIndex = -1;
		return true;
	}

	/**
	 * Fast-forwards a newly started {@link GameMode#LEARNING} puzzle up to (but
	 * excluding) the first step marked {@code isEnabledTraining()}.
	 */
	public static void prepareLearningMode(Sudoku2 targetSudoku, List<SolutionStep> solutionSteps,
			SudokuSolver targetSolver) {
		if (targetSudoku == null || solutionSteps == null || targetSolver == null) {
			return;
		}
		for (SolutionStep step : solutionSteps) {
			if (step.getType().getStepConfig().isEnabledTraining()) {
				break;
			}
			targetSolver.doStep(targetSudoku, step);
		}
	}

	/**
	 * Solves the given board until the first non-progress step (in
	 * {@link GameMode#PLAYING}) or the first training step (in
	 * {@link GameMode#LEARNING}/{@link GameMode#PRACTISING}).
	 *
	 * @return {@code true} if at least one step was executed
	 */
	public static boolean solveUpTo(Sudoku2 targetSudoku, SudokuSolver targetSolver, GameMode gameMode) {
		SolutionStep actStep;
		boolean changed = false;
		while ((actStep = targetSolver.getHint(targetSudoku, false)) != null) {
			if (actStep.isGiveUp()) {
				break;
			} else if (gameMode == GameMode.PLAYING) {
				if (!actStep.getType().getStepConfig().isEnabledProgress()) {
					break;
				}
			} else {
				if (actStep.getType().getStepConfig().isEnabledTraining()) {
					break;
				}
			}
			targetSolver.doStep(targetSudoku, actStep);
			changed = true;
		}
		return changed;
	}

	/**
	 * Instance convenience wrapper around {@link #solveUpTo(Sudoku2, SudokuSolver, GameMode)}
	 * that records an undo snapshot when changes are made.
	 */
	public boolean solveUpTo(GameMode gameMode) {
		undoStack.push(sudoku.clone());
		boolean changed = solveUpTo(sudoku, solver, gameMode);
		if (changed) {
			redoStack.clear();
		} else {
			undoStack.pop();
		}
		activeStep = null;
		chainIndex = -1;
		return changed;
	}

	/**
	 * Checks current board validity and schedules asynchronous difficulty evaluation
	 * via {@code evaluator} when valid.
	 */
	public SudokuStatus validateAndCheckProgress(ProgressEvaluator evaluator) {
		int anz = sudoku.getSolvedCellsAnz();
		if (anz == 0) {
			sudoku.setStatus(SudokuStatus.EMPTY);
			sudoku.setStatusGivens(SudokuStatus.EMPTY);
		} else if (anz <= 17) {
			sudoku.setStatus(SudokuStatus.INVALID);
			sudoku.setStatusGivens(SudokuStatus.INVALID);
		} else if (sudoku.checkSudoku()) {
			int anzSol = solutionCounter.countSolutions(sudoku, 1000);
			sudoku.setStatus(anzSol);
			if (anzSol == 1 && evaluator != null) {
				evaluator.startCheck(sudoku);
			}
		}
		return sudoku.getStatus();
	}

	/**
	 * Exports headless session and solver state into a {@link GameStateSnapshot}.
	 */
	@SuppressWarnings("unchecked")
	public GameStateSnapshot exportSnapshot(boolean copy) {
		GameStateSnapshot snapshot = new GameStateSnapshot();
		snapshot.setChainIndex(chainIndex);
		snapshot.setUndoStack((Stack<Sudoku2>) undoStack.clone());
		snapshot.setRedoStack((Stack<Sudoku2>) redoStack.clone());
		snapshot.setSudoku(copy ? sudoku.clone() : sudoku);
		snapshot.setStep(copy && activeStep != null ? (SolutionStep) activeStep.clone() : activeStep);
		if (solver != null) {
			snapshot.setAnzSteps(copy ? solver.getAnzSteps().clone() : solver.getAnzSteps());
			snapshot.setSteps(copy ? new ArrayList<SolutionStep>(solver.getSteps()) : solver.getSteps());
		}
		return snapshot;
	}

	/**
	 * Restores headless session and solver state from a {@link GameStateSnapshot}.
	 */
	public void restoreSnapshot(GameStateSnapshot snapshot) {
		if (snapshot == null) {
			return;
		}
		chainIndex = snapshot.getChainIndex();
		undoStack = snapshot.getUndoStack() != null ? snapshot.getUndoStack() : new Stack<Sudoku2>();
		redoStack = snapshot.getRedoStack() != null ? snapshot.getRedoStack() : new Stack<Sudoku2>();
		sudoku = snapshot.getSudoku();
		if (sudoku != null) {
			sudoku.checkSudoku();
		}
		activeStep = snapshot.getStep();
		if (solver != null && snapshot.getAnzSteps() != null) {
			int[] targetAnz = solver.getAnzSteps();
			if (snapshot.getAnzSteps().length < targetAnz.length) {
				int[] tmp = new int[targetAnz.length];
				System.arraycopy(snapshot.getAnzSteps(), 0, tmp, 0, snapshot.getAnzSteps().length);
				solver.setAnzSteps(tmp);
			} else {
				solver.setAnzSteps(snapshot.getAnzSteps());
			}
			solver.setSteps(snapshot.getSteps());
			if (sudoku != null) {
				solver.setScore(sudoku.getScore());
			}
		}
	}

	public DifficultyLevel getLevel() {
		return sudoku != null ? sudoku.getLevel() : null;
	}

	public int getScore() {
		return sudoku != null ? sudoku.getScore() : 0;
	}
}
