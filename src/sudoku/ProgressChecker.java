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
package sudoku;

import java.awt.EventQueue;
import kernel.ProgressEvaluator;

/**
 * Swing adapter around {@link kernel.ProgressEvaluator} that forwards
 * background progress evaluation results to {@link MainFrame} on the EDT.
 *
 * @author hobiwan
 */
public class ProgressChecker {

	private final ProgressEvaluator evaluator;

	public ProgressChecker(final MainFrame mainFrame) {
		this.evaluator = new ProgressEvaluator(new ProgressEvaluator.ProgressListener() {
			@Override
			public void onProgressEvaluated(DifficultyLevel level, int score) {
				mainFrame.setCurrentLevel(level);
				mainFrame.setCurrentScore(score);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						mainFrame.setProgressLabel();
					}
				});
			}
		});
	}

	public void startCheck(Sudoku2 actSudoku) {
		evaluator.startCheck(actSudoku);
	}
}
