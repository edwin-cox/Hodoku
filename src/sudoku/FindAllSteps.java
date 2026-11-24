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

import java.util.List;
import kernel.AllStepsFinder;
import kernel.StepSearchProgressListener;

/**
 * Compatibility wrapper around {@link kernel.AllStepsFinder}.
 *
 * @author hobiwan
 */
public class FindAllSteps extends AllStepsFinder {

	public FindAllSteps() {
		super();
	}

	public FindAllSteps(List<SolutionStep> steps, Sudoku2 sudoku, StepSearchProgressListener listener) {
		super(steps, sudoku, listener);
	}
}
