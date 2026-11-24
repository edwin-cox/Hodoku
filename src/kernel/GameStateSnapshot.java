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

import java.util.Date;
import java.util.List;
import java.util.Stack;
import sudoku.SolutionStep;
import sudoku.Sudoku2;

/**
 * Pure, Swing/AWT-free snapshot of a game session and solver state (without
 * UI colors or component references).
 */
public class GameStateSnapshot {
	private Sudoku2 sudoku;
	private Stack<Sudoku2> undoStack;
	private Stack<Sudoku2> redoStack;
	private SolutionStep step;
	private int chainIndex = -1;
	private List<SolutionStep> steps;
	private int[] anzSteps;
	private List<String> titles;
	private List<List<SolutionStep>> tabSteps;
	private String name;
	private Date timestamp;

	public GameStateSnapshot() {
	}

	public Sudoku2 getSudoku() {
		return sudoku;
	}

	public void setSudoku(Sudoku2 sudoku) {
		this.sudoku = sudoku;
	}

	public Stack<Sudoku2> getUndoStack() {
		return undoStack;
	}

	public void setUndoStack(Stack<Sudoku2> undoStack) {
		this.undoStack = undoStack;
	}

	public Stack<Sudoku2> getRedoStack() {
		return redoStack;
	}

	public void setRedoStack(Stack<Sudoku2> redoStack) {
		this.redoStack = redoStack;
	}

	public SolutionStep getStep() {
		return step;
	}

	public void setStep(SolutionStep step) {
		this.step = step;
	}

	public int getChainIndex() {
		return chainIndex;
	}

	public void setChainIndex(int chainIndex) {
		this.chainIndex = chainIndex;
	}

	public List<SolutionStep> getSteps() {
		return steps;
	}

	public void setSteps(List<SolutionStep> steps) {
		this.steps = steps;
	}

	public int[] getAnzSteps() {
		return anzSteps;
	}

	public void setAnzSteps(int[] anzSteps) {
		this.anzSteps = anzSteps;
	}

	public List<String> getTitles() {
		return titles;
	}

	public void setTitles(List<String> titles) {
		this.titles = titles;
	}

	public List<List<SolutionStep>> getTabSteps() {
		return tabSteps;
	}

	public void setTabSteps(List<List<SolutionStep>> tabSteps) {
		this.tabSteps = tabSteps;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
