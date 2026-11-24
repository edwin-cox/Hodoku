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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;
import kernel.SudokuCoreUtil;

/**
 *
 * @author hobiwan
 */
public class SudokuUtil {
	/** The correct line separator for the current platform */
	public static String NEW_LINE = SudokuCoreUtil.NEW_LINE;

	/**
	 * A global PrinterJob; is here and not in {@link Options} because it needs a
	 * getter but should not be written to a configuration file.
	 */
	private static PrinterJob printerJob;
	/**
	 * A global PageFormat; is here and not in {@link Options} because it needs a
	 * getter but should not be written to a configuration file.
	 */
	private static PageFormat pageFormat;

	/**
	 * Clears the list. To avoid memory leaks all steps in the list are explicitly
	 * nullified.
	 * 
	 * @param steps
	 */
	public static void clearStepListWithNullify(List<SolutionStep> steps) {
		SudokuCoreUtil.clearStepListWithNullify(steps);
	}

	/**
	 * Clears the list. The steps are not nullfied, but the list items are.
	 * 
	 * @param steps
	 */
	public static void clearStepList(List<SolutionStep> steps) {
		SudokuCoreUtil.clearStepList(steps);
	}

	/**
	 * Calculates n over k
	 *
	 * @param n
	 * @param k
	 * @return
	 */
	public static int combinations(int n, int k) {
		return SudokuCoreUtil.combinations(n, k);
	}

	/**
	 * @return the printerJob
	 */
	public static PrinterJob getPrinterJob() {
		if (printerJob == null) {
			printerJob = PrinterJob.getPrinterJob();
		}
		return printerJob;
	}

	/**
	 * @return the pageFormat
	 */
	public static PageFormat getPageFormat() {
		if (pageFormat == null) {
			pageFormat = getPrinterJob().defaultPage();
		}
		return pageFormat;
	}

	/**
	 * HiRes printing in Java is difficult: The whole printing engine (except
	 * apparently text printing) is scaled down to 72dpi. This is done by applying
	 * an AffineTransform object with a scale to the Graphics2D object of the
	 * printer. To make things more complicated just reversing the scale is not
	 * enough: for Landscape printing a rotation is applied after the scale.<br>
	 * <br>
	 * 
	 * The easiest way to really achieve hires printing is to directly manipulate
	 * the transformation matrix. The default matrix looks like this:<br>
	 * 
	 * <pre>
	 *      Portrait     Landscape
	 *    [ d  0  x ]   [  0  d  x ]
	 *    [ 0  d  y ]   [ -d  0  y ]
	 *    [ 0  0  1 ]   [  0  0  1 ]
	 * 
	 *    d = printerResolution / 72.0
	 * </pre>
	 * 
	 * x and y are set by the printer engine and should not be changed.<br>
	 * <br>
	 * 
	 * The values from the {@link PageFormat} object are scaled down to 72dpi as
	 * well and have to be multiplied with d to get the correct hires values.
	 * 
	 * @param g2
	 * @return The scale factor
	 */
	public static double adjustGraphicsForPrinting(Graphics2D g2) {
		AffineTransform at = g2.getTransform();
		double[] matrix = new double[6];
		at.getMatrix(matrix);
		// System.out.println("matrix: " + Arrays.toString(matrix));
		double scale = matrix[0];
		if (scale != 0) {
			// Portrait
			matrix[0] = 1;
			matrix[3] = 1;
		} else {
			// Landscape
			scale = matrix[2];
			matrix[1] = -1;
			matrix[2] = 1;
		}
//        int resolution = (int)(72.0 * scale);
		AffineTransform newAt = new AffineTransform(matrix);
		g2.setTransform(newAt);
		return scale;
	}

	/**
	 * Sets the Look and Feel to the class stored in {@link Options#laf}. To make
	 * HoDoKu behave nicely for visually impaired users, a non standard font size
	 * {@link Options#customFontSize} can be used for all GUI elements, if
	 * {@link Options#useDefaultFontSize} is set to <code>false</code>.<br>
	 * <br>
	 * 
	 * This is where the problems starts: On most LaFs (GTK excluded, nothing can be
	 * changed in GTK LaF) changing the font size works by changing all Font
	 * instances in <code>UIManager.getDefaults()</code>. Not with Nimbus though:
	 * Due to late initialization issues the standard method leads to unpredictable
	 * results (see
	 * http://stackoverflow.com/questions/949353/java-altering-ui-fonts-nimbus-doesnt-work
	 * for details).<br>
	 * <br>
	 * 
	 * Changing the font size in Nimbus can be done in one of two ways:
	 * 
	 * <ul>
	 * <li>Subclass <code>NimbusLookAndFeel</code> and override
	 * <code>getDefaults()</code></li>
	 * <li>Obtain an instance of <code>NimbusLookAndFeel</code> and set the
	 * <code>defaultFont</code> option on the instance directly (<b>not</b> on
	 * <code>UIManager.getDefaults()</code>).</li>
	 * </ul>
	 * 
	 * Although both methods seem to be simple enough, there is a slight
	 * complication: The package of the <code>NimbusLookAndFeel</code> class changed
	 * between Java 1.6 (<code>sun.swing.plaf.nimbus</code>) and 1.7
	 * (<code>javax.swing.plaf.nimbus</code>). That means, that if the class is
	 * subclassed or instantiated directly, code compiled with 1.7 will not start on
	 * 1.6 and vice versa (and of course the program will not start on all JRE
	 * versions, that dont have Nimbus included). And we have to think of the
	 * possiblilty, that the class stored in {@link Options#laf} doesnt exist at
	 * all, if the hcfg file is moved between platforms.<br>
	 * <br>
	 * 
	 */
	public static void setLookAndFeel() {
		// ok: start by getting the correct AND existing LaF class
		LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
		boolean found = false;
		String className = Options.getInstance().getLaf();
		String oldClassName = className;
		if (!className.isEmpty()) {
			String lafName = className.substring(className.lastIndexOf('.') + 1);
			for (int i = 0; i < lafs.length; i++) {
				if (lafs[i].getClassName().equals(className)) {
					found = true;
					break;
				} else if (lafs[i].getClassName().endsWith(lafName)) {
					// same class, different package
					className = lafs[i].getClassName();
					Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "laf package changed from {0} to {1}",
							new Object[] { oldClassName, className });
					found = true;
					break;
				}
			}
		}
		if (!found) {
			// class not present or default requested
			Options.getInstance().setLaf("");
			className = UIManager.getSystemLookAndFeelClassName();
		} else {
			if (!oldClassName.equals(className)) {
				Options.getInstance().setLaf(className);
			}
		}

		// ok, the correct class name is now in className
		// -> obtain an instance of the LaF class
		ClassLoader classLoader = MainFrame.class.getClassLoader();
		Class<?> lafClass = null;
		try {
			lafClass = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error changing LaF 1", e);
			return;
		}
		LookAndFeel instance = null;
		try {
			instance = (LookAndFeel) lafClass.newInstance();
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error changing LaF 2", ex);
			return;
		}

		// we have a LaF instance: try setting it
		try {
			int fontSize = Options.getInstance().getCustomFontSize();
			if (!Options.getInstance().isUseDefaultFontSize()) {
				// first change the defaults for Nimbus
				UIDefaults def = instance.getDefaults();
				Object value = null;
				if ((value = def.get("defaultFont")) != null) {
					// exists on Nimbus and triggers inheritance
					Font font = (Font) value;
					if (font.getSize() != fontSize) {
//                        System.out.println("Changing fontSize (1) from " + font.getSize() + " to " + fontSize);
						def.put("defaultFont", new FontUIResource(font.getName(), font.getStyle(), fontSize));
					}
				}
			}

			// set the new LaF
			UIManager.setLookAndFeel(instance);
			Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "laf={0}", UIManager.getLookAndFeel().getName());

			if (!Options.getInstance().isUseDefaultFontSize()) {
				// change the defaults for all other LaFs
				UIDefaults def = UIManager.getDefaults();
				// def.keySet() doesnt seem to work -> use def.keys() instead!
				Enumeration<Object> keys = def.keys();
				while (keys.hasMoreElements()) {
					Object key = keys.nextElement();
					Font font = def.getFont(key);
					if (font != null) {
						if (font.getSize() != fontSize) {
//                            System.out.println("Changing fontSize (2) from " + font.getSize() + " to " + fontSize);
							def.put(key, new FontUIResource(font.getName(), font.getStyle(), fontSize));
						}
					}
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Error changing LaF 3", ex);
		}
	}

	/**
	 * Prints the default font settings to stdout. Used for debugging only.
	 */
	public static void printFontDefaults() {
		System.out.println("Default font settings: UIManager");
		UIDefaults def = UIManager.getDefaults();
		SortedMap<String, String> items = new TreeMap<String, String>();
		// def.keySet() doesnt seem to work -> use def.keys() instead!
		Enumeration<Object> keys = def.keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Font font = def.getFont(key);
			if (font != null) {
				items.put(key.toString(), font.getName() + "/" + font.getStyle() + "/" + font.getSize());
			}
		}
		Set<Entry<String, String>> entries = items.entrySet();
		for (Entry<String, String> act : entries) {
			System.out.println("     " + act.getKey() + ": " + act.getValue());
		}
	}

	/**
	 * Reformat a sudoku given by a 81 character string to SimpleSudoku format.
	 * 
	 * @param values
	 * @return
	 */
	public static String getSSFormatted(String values) {
		return SudokuCoreUtil.getSSFormatted(values);
	}

	/**
	 * Reformat a HoDoKu PM grid to SimpleSudoku format.
	 * 
	 * @param grid
	 * @return
	 */
	public static String getSSPMGrid(String grid) {
		return SudokuCoreUtil.getSSPMGrid(grid);
	}

	/**
	 * STUB!!
	 * 
	 * Is meant for replacing candidate numbers with colors for colorKu. Doesnt do
	 * anything meaningful right now.
	 * 
	 * @param candidate
	 * @return
	 */
	public static String getCandString(int candidate) {
		if (Options.getInstance().isShowColorKuAct()) {
			// return some color name here
			return String.valueOf(candidate);
		} else {
			return String.valueOf(candidate);
		}
	}

	/**
	 * testing...
	 * 
	 * @param args
	 */
	/*
	public static void main(String[] args) {
		String grid = ".---------------.------------.-------------." + "| 1   78    38  | 2   49  6  | 47  39  5   |"
				+ "| 9   67    5   | 3   8   14 | 47  16  2   |" + "| 36  4     2   | 19  7   5  | 8   36  19  |"
				+ ":---------------+------------+-------------:" + "| 8   9     7   | 5   6   2  | 13  4   13  |"
				+ "| 25  25    1   | 4   3   8  | 9   7   6   |" + "| 4   3     6   | 7   1   9  | 5   2   8   |"
				+ ":---------------+------------+-------------:" + "| 36  16    4   | 8   5   7  | 2   19  139 |"
				+ "| 7   158   89  | 19  2   3  | 6   58  4   |" + "| 25  1258  389 | 6   49  14 | 3   58  7   |"
				+ "'---------------'------------'-------------'";
		getSSPMGrid(grid);
	}*/
}
