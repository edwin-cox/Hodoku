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

/**
 * Headless progress callback for exhaustive step searches ({@link AllStepsFinder},
 * {@link solver.FishSolver}, and {@link solver.SudokuSolver#getProgressScore}).
 */
public interface StepSearchProgressListener {

	/**
	 * Notifies the listener that a search stage has begun.
	 *
	 * @param stage     the current search stage
	 * @param candidate candidate digit (1..9) for fish/kraken-fish stages, or -1 if N/A
	 * @param stepIndex sequential progress step index (0..27)
	 */
	void onStageStarted(StepSearchStage stage, int candidate, int stepIndex);

	/**
	 * Resets the secondary sub-progress counter (e.g., fish base combinations or
	 * progress score steps).
	 *
	 * @param maxValue maximum value for the sub-progress bar
	 */
	void resetSubProgress(int maxValue);

	/**
	 * Updates the secondary sub-progress counter.
	 *
	 * @param currentValue current progress value
	 */
	void updateSubProgress(int currentValue);

	/**
	 * Called when the full step search completes normally.
	 */
	void onSearchCompleted();
}
