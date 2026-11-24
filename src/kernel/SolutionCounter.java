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

import sudoku.Sudoku2;

/**
 * Abstraction for counting valid solutions of a Sudoku grid, decoupling the
 * domain model ({@link Sudoku2}) from {@link generator.SudokuGenerator}.
 */
@FunctionalInterface
public interface SolutionCounter {

	/**
	 * Counts solutions up to a threshold (e.g., 1 to check uniqueness).
	 *
	 * @param sudoku       the grid to evaluate
	 * @param maxSolutions upper bound or search limit expected by implementation
	 * @return number of solutions found (0, 1, or 2+)
	 */
	int countSolutions(Sudoku2 sudoku, int maxSolutions);
}
