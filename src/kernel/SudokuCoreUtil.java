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

import java.math.BigInteger;
import java.util.List;
import sudoku.SolutionStep;

/**
 * Pure, UI-independent utility methods extracted from {@link sudoku.SudokuUtil}.
 */
public final class SudokuCoreUtil {
	/** The correct line separator for the current platform. */
	public static final String NEW_LINE = System.getProperty("line.separator");

	private SudokuCoreUtil() {
	}

	/**
	 * Clears the list. To avoid memory leaks all steps in the list are explicitly
	 * nullified.
	 *
	 * @param steps list of steps to reset and clear
	 */
	public static void clearStepListWithNullify(List<SolutionStep> steps) {
		if (steps != null) {
			for (int i = 0; i < steps.size(); i++) {
				steps.get(i).reset();
				steps.set(i, null);
			}
			steps.clear();
		}
	}

	/**
	 * Clears the list. The steps are not nullified, but the list items are.
	 *
	 * @param steps list of steps to clear
	 */
	public static void clearStepList(List<SolutionStep> steps) {
		if (steps != null) {
			for (int i = 0; i < steps.size(); i++) {
				steps.set(i, null);
			}
			steps.clear();
		}
	}

	/**
	 * Calculates binomial coefficient n choose k.
	 *
	 * @param n total items
	 * @param k chosen items
	 * @return n over k
	 */
	public static int combinations(int n, int k) {
		if (n <= 167) {
			double fakN = 1;
			for (int i = 2; i <= n; i++) {
				fakN *= i;
			}
			double fakNMinusK = 1;
			for (int i = 2; i <= n - k; i++) {
				fakNMinusK *= i;
			}
			double fakK = 1;
			for (int i = 2; i <= k; i++) {
				fakK *= i;
			}
			return (int) (fakN / (fakNMinusK * fakK));
		} else {
			BigInteger fakN = BigInteger.ONE;
			for (int i = 2; i <= n; i++) {
				fakN = fakN.multiply(new BigInteger(i + ""));
			}
			BigInteger fakNMinusK = BigInteger.ONE;
			for (int i = 2; i <= n - k; i++) {
				fakNMinusK = fakNMinusK.multiply(new BigInteger(i + ""));
			}
			BigInteger fakK = BigInteger.ONE;
			for (int i = 2; i <= k; i++) {
				fakK = fakK.multiply(new BigInteger(i + ""));
			}
			fakNMinusK = fakNMinusK.multiply(fakK);
			fakN = fakN.divide(fakNMinusK);
			return fakN.intValue();
		}
	}

	/**
	 * Reformats a sudoku given by an 81-character string to SimpleSudoku format.
	 *
	 * @param values 81-character values string
	 * @return SimpleSudoku formatted grid
	 */
	public static String getSSFormatted(String values) {
		StringBuilder tmp = new StringBuilder();
		values = values.replace('0', '.');
		tmp.append(" *-----------*");
		tmp.append(NEW_LINE);
		writeSSLine(tmp, values, 0);
		writeSSLine(tmp, values, 9);
		writeSSLine(tmp, values, 18);
		tmp.append(" |---+---+---|");
		tmp.append(NEW_LINE);
		writeSSLine(tmp, values, 27);
		writeSSLine(tmp, values, 36);
		writeSSLine(tmp, values, 45);
		tmp.append(" |---+---+---|");
		tmp.append(NEW_LINE);
		writeSSLine(tmp, values, 54);
		writeSSLine(tmp, values, 63);
		writeSSLine(tmp, values, 72);
		tmp.append(" *-----------*");
		tmp.append(NEW_LINE);
		return tmp.toString();
	}

	private static void writeSSLine(StringBuilder tmp, String clues, int startIndex) {
		tmp.append(" |");
		tmp.append(clues.substring(startIndex + 0, startIndex + 3));
		tmp.append("|");
		tmp.append(clues.substring(startIndex + 3, startIndex + 6));
		tmp.append("|");
		tmp.append(clues.substring(startIndex + 6, startIndex + 9));
		tmp.append("|");
		tmp.append(NEW_LINE);
	}

	/**
	 * Reformats a HoDoKu PM grid to SimpleSudoku format.
	 *
	 * @param grid pencilmark grid string
	 * @return SimpleSudoku formatted PM grid
	 */
	public static String getSSPMGrid(String grid) {
		String[] parts = grid.split(" ");
		String[] cells = new String[81];
		int maxLength = 0;
		for (int i = 0, j = 0; i < parts.length; i++) {
			if (parts[i].isEmpty()) {
				continue;
			}
			char ch = parts[i].charAt(0);
			if (Character.isDigit(ch)) {
				cells[j++] = parts[i];
				if (parts[i].length() > maxLength) {
					maxLength = parts[i].length();
				}
			}
		}
		for (int i = 0; i < cells.length; i++) {
			if (cells[i].length() < maxLength) {
				int anz = maxLength - cells[i].length();
				for (int j = 0; j < anz; j++) {
					cells[i] += " ";
				}
			}
		}
		StringBuilder tmp = new StringBuilder();
		writeSSPMFrameLine(tmp, maxLength, true);
		writeSSPMLine(tmp, cells, 0);
		writeSSPMLine(tmp, cells, 9);
		writeSSPMLine(tmp, cells, 18);
		writeSSPMFrameLine(tmp, maxLength, false);
		writeSSPMLine(tmp, cells, 27);
		writeSSPMLine(tmp, cells, 36);
		writeSSPMLine(tmp, cells, 45);
		writeSSPMFrameLine(tmp, maxLength, false);
		writeSSPMLine(tmp, cells, 54);
		writeSSPMLine(tmp, cells, 63);
		writeSSPMLine(tmp, cells, 72);
		writeSSPMFrameLine(tmp, maxLength, true);

		return tmp.toString();
	}

	private static void writeSSPMLine(StringBuilder tmp, String[] cells, int index) {
		tmp.append(" | ");
		tmp.append(cells[index + 0]);
		tmp.append("  ");
		tmp.append(cells[index + 1]);
		tmp.append("  ");
		tmp.append(cells[index + 2]);
		tmp.append("  | ");
		tmp.append(cells[index + 3]);
		tmp.append("  ");
		tmp.append(cells[index + 4]);
		tmp.append("  ");
		tmp.append(cells[index + 5]);
		tmp.append("  | ");
		tmp.append(cells[index + 6]);
		tmp.append("  ");
		tmp.append(cells[index + 7]);
		tmp.append("  ");
		tmp.append(cells[index + 8]);
		tmp.append("  |");
		tmp.append(NEW_LINE);
	}

	private static void writeSSPMFrameLine(StringBuilder tmp, int maxLength, boolean outer) {
		tmp.append(" *");
		for (int i = 0; i < 3 * maxLength + 7; i++) {
			tmp.append("-");
		}
		if (outer) {
			tmp.append("-");
		} else {
			tmp.append("+");
		}
		for (int i = 0; i < 3 * maxLength + 7; i++) {
			tmp.append("-");
		}
		if (outer) {
			tmp.append("-");
		} else {
			tmp.append("+");
		}
		for (int i = 0; i < 3 * maxLength + 7; i++) {
			tmp.append("-");
		}
		if (outer) {
			tmp.append("*");
		} else {
			tmp.append("|");
		}
		tmp.append(NEW_LINE);
	}
}
