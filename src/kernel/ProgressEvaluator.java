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

import java.util.logging.Level;
import java.util.logging.Logger;
import solver.SudokuSolver;
import solver.SudokuSolverFactory;
import sudoku.ClipboardMode;
import sudoku.DifficultyLevel;
import sudoku.Sudoku2;
import sudoku.SudokuStatus;

/**
 * Headless background evaluator that computes the current {@link DifficultyLevel}
 * and score of a valid Sudoku puzzle whenever the board state changes.
 */
public class ProgressEvaluator implements Runnable {

	/**
	 * Callback invoked on the evaluator thread once difficulty and score have been computed.
	 */
	@FunctionalInterface
	public interface ProgressListener {
		void onProgressEvaluated(DifficultyLevel level, int score);
	}

	private final Sudoku2 sudoku = new Sudoku2();
	private final Sudoku2 passedInSudoku = new Sudoku2();
	private boolean passedIn = false;
	private final Thread thread;
	private boolean threadStarted = false;
	private final ProgressListener listener;
	private SudokuSolver solver = null;

	public ProgressEvaluator(ProgressListener listener) {
		this.listener = listener;
		this.thread = new Thread(this);
	}

	/**
	 * Schedules an asynchronous progress evaluation for {@code actSudoku}.
	 *
	 * @param actSudoku current puzzle state
	 */
	public void startCheck(Sudoku2 actSudoku) {
		if (thread == null || actSudoku == null) {
			return;
		}
		if (!threadStarted) {
			thread.start();
			threadStarted = true;
		}
		synchronized (thread) {
			passedInSudoku.set(actSudoku);
			passedIn = true;
			thread.notify();
		}
	}

	/**
	 * Interrupts the background evaluation thread.
	 */
	public void stop() {
		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	public void run() {
		while (!thread.isInterrupted()) {
			try {
				synchronized (thread) {
					if (!passedIn) {
						thread.wait();
					}
					if (passedIn) {
						sudoku.set(passedInSudoku);
						sudoku.checkSudoku();
						passedIn = false;
					} else {
						continue;
					}
				}
				if (sudoku.getStatus() != SudokuStatus.VALID) {
					Logger.getLogger(getClass().getName()).log(Level.SEVERE,
							"Progress check scheduled for invalid sudoku ({0})",
							sudoku.getSudoku(ClipboardMode.LIBRARY));
					continue;
				}
				if (solver == null) {
					solver = SudokuSolverFactory.getInstance();
				}
				solver.setSudoku(sudoku);
				if (solver.solve()) {
					if (listener != null) {
						listener.onProgressEvaluated(sudoku.getLevel(), sudoku.getScore());
					}
				}
				SudokuSolverFactory.giveBack(solver);
				solver = null;
			} catch (InterruptedException ex) {
				thread.interrupt();
			} catch (Exception ex) {
				Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error checking progress", ex);
			}
		}
	}
}
