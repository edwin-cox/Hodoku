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
import kernel.BackdoorSearchListener.MeasureKind;
import kernel.BackdoorSearchListener.SearchKind;
import solver.SudokuSolver;
import sudoku.Candidate;
import sudoku.SolutionStep;
import sudoku.StepConfig;
import sudoku.Sudoku2;

/**
 * Headless combinatorial backdoor search engine extracted from
 * {@link sudoku.BackdoorSearchDialog}. Searches combinations of 1, 2, or 3 cells
 * and/or candidate eliminations that unlock a puzzle using singles only or a
 * configurable progress-measure technique set.
 */
public class BackdoorSearcher implements Runnable {

	public static final int DEFAULT_MAX_FOUND = 100;

	private final Sudoku2 orgSudoku;
	private final Sudoku2 workingSudoku;
	private final SudokuSolver solver;
	private final boolean searchCells;
	private final boolean searchCandidates;
	private final int maxCandidateDepth;
	private final StepConfig[] progressSteps;
	private final BackdoorSearchListener listener;
	private final int maxFound;

	private int foundInPass;

	public BackdoorSearcher(Sudoku2 puzzle, SudokuSolver solver, boolean searchCells, boolean searchCandidates,
			int maxCandidateDepth, StepConfig[] progressSteps, BackdoorSearchListener listener) {
		this(puzzle, solver, searchCells, searchCandidates, maxCandidateDepth, progressSteps, listener,
				DEFAULT_MAX_FOUND);
	}

	public BackdoorSearcher(Sudoku2 puzzle, SudokuSolver solver, boolean searchCells, boolean searchCandidates,
			int maxCandidateDepth, StepConfig[] progressSteps, BackdoorSearchListener listener, int maxFound) {
		this.orgSudoku = puzzle.clone();
		this.workingSudoku = this.orgSudoku.clone();
		this.solver = solver;
		this.searchCells = searchCells;
		this.searchCandidates = searchCandidates;
		this.maxCandidateDepth = Math.max(1, Math.min(3, maxCandidateDepth));
		this.progressSteps = progressSteps;
		this.listener = listener;
		this.maxFound = maxFound;
	}

	@Override
	public void run() {
		try {
			if (searchCells) {
				int anz = orgSudoku.getUnsolvedCellsAnz();
				foundInPass = 0;
				if (!checkSingles(1, anz, null)) {
					int maxAnz = SudokuCoreUtil.combinations(anz, 2);
					if (!checkSingles(2, maxAnz, null)) {
						maxAnz = SudokuCoreUtil.combinations(anz, 3);
						checkSingles(3, maxAnz, null);
					}
				}
				if (progressSteps != null) {
					foundInPass = 0;
					if (!checkSingles(1, anz, progressSteps)) {
						int maxAnz = SudokuCoreUtil.combinations(anz, 2);
						if (!checkSingles(2, maxAnz, progressSteps)) {
							maxAnz = SudokuCoreUtil.combinations(anz, 3);
							checkSingles(3, maxAnz, progressSteps);
						}
					}
				}
			}

			if (searchCandidates) {
				List<Candidate> candidates = new ArrayList<Candidate>();
				for (int i = 0; i < Sudoku2.LENGTH; i++) {
					if (workingSudoku.getValue(i) == 0) {
						int[] cands = workingSudoku.getAllCandidates(i);
						for (int j = 0; j < cands.length; j++) {
							if (cands[j] != workingSudoku.getSolution(i)) {
								candidates.add(new Candidate(i, cands[j]));
							}
						}
					}
				}
				int anz = candidates.size();
				foundInPass = 0;
				if (!checkCandidates(1, anz, candidates, null) && maxCandidateDepth > 1) {
					int maxAnz = SudokuCoreUtil.combinations(anz, 2);
					if (!checkCandidates(2, maxAnz, candidates, null) && maxCandidateDepth > 2) {
						maxAnz = SudokuCoreUtil.combinations(anz, 3);
						checkCandidates(3, maxAnz, candidates, null);
					}
				}
				if (progressSteps != null) {
					foundInPass = 0;
					if (!checkCandidates(1, anz, candidates, progressSteps) && maxCandidateDepth > 1) {
						int maxAnz = SudokuCoreUtil.combinations(anz, 2);
						if (!checkCandidates(2, maxAnz, candidates, progressSteps) && maxCandidateDepth > 2) {
							maxAnz = SudokuCoreUtil.combinations(anz, 3);
							checkCandidates(3, maxAnz, candidates, progressSteps);
						}
					}
				}
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		}
		if (listener != null) {
			listener.onSearchFinished();
		}
	}

	private boolean checkCandidates(int depth, int max, List<Candidate> candidates, StepConfig[] stepConfigs)
			throws InterruptedException {
		boolean found = false;
		int end = candidates.size();
		int counter = 0;
		MeasureKind measure = (stepConfigs == null) ? MeasureKind.SINGLES_ONLY : MeasureKind.PROGRESS_MEASURE;
		for (int i = 0; i < end; i++) {
			if (depth == 1) {
				if (checkSingleOrCandidate(-1, -1, -1, candidates.get(i), null, null, stepConfigs)) {
					found = true;
					foundInPass++;
					if (foundInPass > maxFound) {
						return true;
					}
				}
				notifyProgress(SearchKind.CANDIDATES, measure, depth, ++counter, max);
			} else {
				for (int j = i + 1; j < end; j++) {
					if (depth == 2) {
						if (checkSingleOrCandidate(-1, -1, -1, candidates.get(i), candidates.get(j), null,
								stepConfigs)) {
							found = true;
							foundInPass++;
							if (foundInPass > maxFound) {
								return true;
							}
						}
						notifyProgress(SearchKind.CANDIDATES, measure, depth, ++counter, max);
					} else {
						for (int k = j + 1; k < end; k++) {
							if (checkSingleOrCandidate(-1, -1, -1, candidates.get(i), candidates.get(j),
									candidates.get(k), stepConfigs)) {
								found = true;
								foundInPass++;
								if (foundInPass > maxFound) {
									return true;
								}
							}
							notifyProgress(SearchKind.CANDIDATES, measure, depth, ++counter, max);
						}
					}
				}
			}
		}
		return found;
	}

	private boolean checkSingles(int depth, int max, StepConfig[] stepConfigs) throws InterruptedException {
		boolean found = false;
		int end = orgSudoku.getCells().length;
		int counter = 0;
		MeasureKind measure = (stepConfigs == null) ? MeasureKind.SINGLES_ONLY : MeasureKind.PROGRESS_MEASURE;
		for (int i = 0; i < end; i++) {
			if (orgSudoku.getValue(i) != 0) {
				continue;
			}
			if (depth == 1) {
				if (checkSingleOrCandidate(i, -1, -1, null, null, null, stepConfigs)) {
					found = true;
					foundInPass++;
					if (foundInPass > maxFound) {
						return true;
					}
				}
				notifyProgress(SearchKind.CELLS, measure, depth, ++counter, max);
			} else {
				for (int j = i + 1; j < end; j++) {
					if (orgSudoku.getValue(j) != 0) {
						continue;
					}
					if (depth == 2) {
						if (checkSingleOrCandidate(i, j, -1, null, null, null, stepConfigs)) {
							found = true;
							foundInPass++;
							if (foundInPass > maxFound) {
								return true;
							}
						}
						notifyProgress(SearchKind.CELLS, measure, depth, ++counter, max);
					} else {
						for (int k = j + 1; k < end; k++) {
							if (orgSudoku.getValue(k) != 0) {
								continue;
							}
							if (checkSingleOrCandidate(i, j, k, null, null, null, stepConfigs)) {
								found = true;
								foundInPass++;
								if (foundInPass > maxFound) {
									return true;
								}
							}
							notifyProgress(SearchKind.CELLS, measure, depth, ++counter, max);
						}
					}
				}
			}
		}
		return found;
	}

	private void notifyProgress(SearchKind searchKind, MeasureKind measureKind, int depth, int current, int max) {
		if (listener != null) {
			listener.onProgress(searchKind, measureKind, depth, current, max);
		}
	}

	private boolean checkSingleOrCandidate(int index1, int index2, int index3, Candidate cand1, Candidate cand2,
			Candidate cand3, StepConfig[] stepConfigs) throws InterruptedException {
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException();
		}
		workingSudoku.set(orgSudoku);
		if (cand1 == null) {
			workingSudoku.setCell(index1, workingSudoku.getSolution(index1));
			if (index2 >= 0) {
				workingSudoku.setCell(index2, workingSudoku.getSolution(index2));
			}
			if (index3 >= 0) {
				workingSudoku.setCell(index3, workingSudoku.getSolution(index3));
			}
		} else {
			workingSudoku.setCandidate(cand1.getIndex(), cand1.getValue(), false);
			if (cand2 != null) {
				workingSudoku.setCandidate(cand2.getIndex(), cand2.getValue(), false);
			}
			if (cand3 != null) {
				workingSudoku.setCandidate(cand3.getIndex(), cand3.getValue(), false);
			}
		}

		boolean isSolved;
		if (stepConfigs == null) {
			isSolved = solver.solveSinglesOnly(workingSudoku);
		} else {
			isSolved = solver.solveWithSteps(workingSudoku, stepConfigs);
		}

		if (isSolved) {
			String cellString;
			if (cand1 == null) {
				cellString = SolutionStep.getCellPrint(index1, false);
				if (index2 >= 0) {
					cellString += ", " + SolutionStep.getCellPrint(index2, false);
				}
				if (index3 >= 0) {
					cellString += ", " + SolutionStep.getCellPrint(index3, false);
				}
			} else {
				cellString = SolutionStep.getCellPrint(cand1.getIndex(), false) + "<>" + cand1.getValue();
				if (cand2 != null) {
					cellString += ", " + SolutionStep.getCellPrint(cand2.getIndex(), false) + "<>" + cand2.getValue();
				}
				if (cand3 != null) {
					cellString += ", " + SolutionStep.getCellPrint(cand3.getIndex(), false) + "<>" + cand3.getValue();
				}
			}
			if (listener != null) {
				MeasureKind measure = (stepConfigs == null) ? MeasureKind.SINGLES_ONLY : MeasureKind.PROGRESS_MEASURE;
				listener.onBackdoorFound(measure, cellString);
			}
			return true;
		}
		return false;
	}
}
