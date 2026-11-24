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
 * Headless listener for {@link BackdoorSearcher} progress and discovered backdoors.
 */
public interface BackdoorSearchListener {

	enum SearchKind {
		CELLS,
		CANDIDATES
	}

	enum MeasureKind {
		SINGLES_ONLY,
		PROGRESS_MEASURE
	}

	/**
	 * Reports progress within a combinatorial backdoor search pass.
	 *
	 * @param searchKind  cells or candidates search
	 * @param measureKind singles-only or progress-measure solver
	 * @param depth       combination size (1, 2, or 3)
	 * @param current     attempts completed in this pass
	 * @param max         total combinations in this pass
	 */
	void onProgress(SearchKind searchKind, MeasureKind measureKind, int depth, int current, int max);

	/**
	 * Reports a discovered backdoor formatted as cell/candidate notation.
	 *
	 * @param measureKind singles-only or progress-measure solver
	 * @param description formatted backdoor string (e.g., "r1c2" or "r1c2<>5")
	 */
	void onBackdoorFound(MeasureKind measureKind, String description);

	/**
	 * Called when the entire search finishes normally (not invoked if interrupted).
	 */
	void onSearchFinished();
}
