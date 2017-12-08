package GUIStuff;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PathGUITool extends JPanel implements ClipboardOwner {
	private static final long serialVersionUID = 3205256608145459434L;
	private static MPGen2D selectedPath;
	private static PathGUITool fig;
	private JFrame g = new JFrame("Path GUI Tool");
	private int undoRedoCounter = 0;
	private boolean draw;
	private double upperXtic, upperYtic, height, xScale, yScale,
			yTickYMax = 0, yTickYMin = 0, x1vert = 0;
	private Rectangle rect;
	private ArrayList<Point> inputs = new ArrayList<>(), inputsBuffer = new ArrayList<>(), currentPath = new ArrayList<>(),
			previousPath = new ArrayList<>();
	private LinkedHashMap<String, ArrayList<Point>> paths = new LinkedHashMap<>();

	/**
	 * Constructor.
	 *
	 * @param d is the boolean that handles if the GUI is in mouse-draw mode or not.
	 */
	private PathGUITool(boolean d) {
		upperXtic = -Double.MAX_VALUE;
		upperYtic = -Double.MAX_VALUE;
		draw = d;

		//Set the properties of this JFrame
		g.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		g.add(this);
		g.setSize(1280, 720);
		g.setLocationByPlatform(true);
		g.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		g.setVisible(true);

		//Add listeners to this JFrame
		g.addWindowListener(new WindowListener());
		g.addMouseListener(new MouseListener());
		g.addMouseMotionListener(new MouseListener());
		g.addKeyListener(new KeyboardListener());
	}

	private static double constrainTo(double value, double maxConstrain) {
		return Math.max(0.0, Math.min(maxConstrain, value));
	}

	private static ArrayList<Double> getXVector(double[][] arr) {
		ArrayList<Double> temp = new ArrayList<>();
		double[] xVals = Arrays.stream(arr).mapToDouble(doubles -> doubles[0]).toArray();
		IntStream.range(0, xVals.length).forEach(i -> temp.add(i, xVals[i]));
		return temp;
	}

	private static ArrayList<Double> getYVector(double[][] arr) {
		ArrayList<Double> temp = new ArrayList<>();
		double[] xVals = Arrays.stream(arr).mapToDouble(doubles -> doubles[1]).toArray();
		IntStream.range(0, xVals.length).forEach(i -> temp.add(i, xVals[i]));
		return temp;
	}

	public static void main(String[] args) {
		fig = new PathGUITool(true);
	}

	/**
	 * Main method which paints the panel and shows the figure.
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		double width = getWidth();
		height = getHeight();

		// Draw X and Y lines axis.
		Line2D.Double yaxis = new Line2D.Double(30, 10, 30, height - 30);
		Line2D.Double xaxis = new Line2D.Double(30, height - 30, width, height - 30);
		g2.draw(yaxis);
		g2.draw(xaxis);

		//draw ticks
		double yMax = 27.0, xMax = 54.33333;
		drawYTickRange(g2, yaxis, yMax);
		drawXTickRange(g2, xaxis, xMax);

		//plot field
		plotFieldBorder(g2);
		rect = new Rectangle((int) yaxis.getX1(), (int) yaxis.getY1(), (int) (x1vert - xaxis.getX1()), (int) (yaxis.getY2()
				- yaxis.getY1()));
		g2.setColor(Color.black);
		g2.draw(rect);

		xScale = rect.getWidth() / 54.3333333;
		yScale = rect.getHeight() / 27.0;

		plotFieldElements(g2);

		//plot data
		plot(g2);
	}

	private void plot(Graphics2D g2) {
		int h = super.getHeight();
		Color tempC = g2.getColor();

		IntStream.range(0, currentPath.size() - 1).forEach(i -> {
			double x1 = 30 + xScale * currentPath.get(i).x, x2 = 30 + xScale * currentPath.get(i + 1).x;
			double y1 = h - 30 - yScale * currentPath.get(i).y, y2 = h - 30 - yScale * currentPath.get(i + 1).y;
			g2.setPaint(Color.green);
			g2.draw(new Line2D.Double(x1, y1, x2, y2));
			g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
			g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));
		});
		//loop through paths and plot each
		paths.forEach((key, value) -> IntStream.range(0, value.size() - 1).forEach(i -> {
			double x1 = 30 + xScale * value.get(i).x, x2 = 30 + xScale * value.get(i + 1).x;
			double y1 = h - 30 - yScale * value.get(i).y, y2 = h - 30 - yScale * value.get(i + 1).y;
			g2.setPaint(Color.green);
			g2.draw(new Line2D.Double(x1, y1, x2, y2));
			g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
			g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));
		}));
		g2.setColor(tempC);
	}

	private void plotFieldElements(Graphics2D g2) {
		HashMap<String, double[][]> elements = new HashMap<>();
		elements.put("blueAirship", new double[][]{
				{15.44166, 11.79},//3.42 ft
				{15.44166, 15.21},//5.9241666666666666666666666666666
				{12.47926, 16.90583},
				{9.517493, 15.21},//9.5174933333333333333333333333333
				{7.788327, 16.185},
				{9.517493, 15.21},
				{9.517493, 11.79},//2.9625 , 1.695833333
				{7.788327, 10.815},
				{9.517493, 11.79},
				{12.47926, 10.09417},//1.729166, 0.975
				{15.44166, 11.79},
		});
		elements.put("redAirship", new double[][]{
				{38.89167, 11.79},//3.42 ft
				{38.89167, 15.21},//5.9241666666666666666666666666666
				{41.85407, 16.90583},
				{44.81584, 15.21},//9.5174933333333333333333333333333
				{46.545, 16.185},
				{44.81584, 15.21},
				{44.81584, 11.79},//2.9625 , 1.695833333
				{46.545, 10.815},
				{44.81584, 11.79},
				{41.85407, 10.09417},//1.7291666, 0.975
				{38.89167, 11.79},
		});
		elements.put("blueAutoLine", new double[][]{
				{7.77500, 0.0},
				{7.77500, 27.0},
				{7.94166, 27.0},
				{7.94166, 0.0},
		});
		elements.put("redAutoLine", new double[][]{
				{46.55833, 0.0},
				{46.55833, 27.0},
				{46.39166, 27.0},
				{46.39166, 0.0},
		});
		elements.put("blueKeyLine", new double[][]{
				{0.0, 17.7316},//9.0375 from top 9.4425 from left outer line
				{9.4425, 27.0},//-0.23083 feet inner line -0.24083 feet
				{9.2016, 27.0},
				{0.0, 17.9625},
		});
		elements.put("redKeyLine", new double[][]{
				{54.33333, 17.7316},//9.0375 from top 9.4425 from left outer line
				{44.89083, 27.0},//-0.23083 feet inner line -0.24083 feet
				{45.13173, 27.0},
				{54.33333, 17.9625},
		});
		elements.put("blueLoadingZoneLine", new double[][]{
				{54.33333, 7.05083},
				{40.3025, 0.0},//-0.18666666666666666666666666666667 for y -0.37083333333333333333333333333333 for x
				{40.67333, 0.0},
				{54.33333, 6.864163},
		});
		elements.put("redLoadingZoneLine", new double[][]{
				{0.0, 7.05083},
				{14.03083, 0.0},//-0.18666666666666666666666666666667 for y -0.37083333333333333333333333333333 for x
				{13.659996, 0.0},
				{0.0, 6.864163},
		});
		elements.put("blueBoilerLine", new double[][]{
				{3.0814052429175126996746554085053, 27.0},
				{0.0, 24.051223364526279556078066141698},//-0.0094280904158206 feet for x +0.0553900311929462 feet for y
				{0.0, 27.0},
		});
		elements.put("redBoilerLine", new double[][]{
				{51.251928090415787300325344591495, 27.0},
				{54.33333333, 24.051223364526279556078066141698},//-0.0094280904158206 feet for x +0.0553900311929462 feet for y
				{54.33333333, 27.0},
		});
		elements.put("redLoadingLine", new double[][]{
				{0.0, 3.14583},//3.14583
				{6.16166, 0.0},
				{0.0, 0.0},//6.16166
		});
		elements.put("blueLoadingLine", new double[][]{
				{54.33333, 3.14583},//3.14583
				{48.171673, 0.0},
				{54.33333, 0.0},//6.16166
		});
		int h = super.getHeight();

		for(Entry<String, double[][]> entry : elements.entrySet()) {
			if(entry.getKey().contains("Line")) {
				double[] xPoints = new double[entry.getValue().length], yPoints = new double[entry.getValue().length];
				IntStream.range(0, entry.getValue().length).forEach(i -> {
					xPoints[i] = 30 + xScale * entry.getValue()[i][0];
					yPoints[i] = h - 30 - yScale * entry.getValue()[i][1];
				});
				Polygon2D p = new Polygon2D(xPoints, yPoints, xPoints.length);
				if(entry.getKey().contains("blue"))
					g2.setPaint(Color.blue);
				else
					g2.setPaint(Color.red);
				g2.fill(p);
				continue;
			}

			for(int i = 0; i < entry.getValue().length - 1; i++)
				for(int j = 0; j < entry.getValue()[0].length - 1; j++) {
					double x1 = 30 + xScale * entry.getValue()[i][j];
					double x2 = 30 + xScale * entry.getValue()[i + 1][j];
					double y1 = h - 30 - yScale * entry.getValue()[i][j + 1];
					double y2 = h - 30 - yScale * entry.getValue()[i + 1][j + 1];

					g2.setPaint(Color.black);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
				}
		}
	}

	private void drawYTickRange(Graphics2D g2, Line2D yAxis, double Max) {
		upperYtic = Max < 0 ? Math.floor(Max) : Math.ceil(Max);

		double x0 = yAxis.getX1();
		double y0 = yAxis.getY1();
		double xf = yAxis.getX2();
		double yf = yAxis.getY2();

		//calculate stepsize between ticks and length of Y axis using distance formula
		double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperYtic;

		double upper = upperYtic, newY = 0;
		yTickYMax = y0;
		for(int i = 0; i <= upperYtic; i++) {
			newY = y0;
			//calculate width of number for proper drawing
			String number = new DecimalFormat("#.#").format(upper);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(number);

			g2.draw(new Line2D.Double(x0, newY, x0 - 10, newY));
			g2.drawString(number, (float) x0 - 15 - width, (float) newY + 5);

			//add grid lines to chart
			if(i != upperYtic) {
				Stroke tempS = g2.getStroke();
				Color tempC = g2.getColor();

				g2.setColor(Color.lightGray);
				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));

				g2.draw(new Line2D.Double(30, newY, getWidth(), newY));

				g2.setColor(tempC);
				g2.setStroke(tempS);
			}
			upper -= 1;
			y0 = newY + distance;
		}
		yTickYMin = newY;
	}

	private void drawXTickRange(Graphics2D g2, Line2D xaxis, double Max) {
		upperXtic = Max < 0 ? Math.floor(Max) : Math.ceil(Max);

		double x0 = xaxis.getX1();
		double y0 = xaxis.getY1();
		double xf = xaxis.getX2();
		double yf = xaxis.getY2();

		//calculate stepsize between ticks and length of Y axis using distance formula
		double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperXtic;

		double lower = 0;
		for(int i = 0; i <= upperXtic; i++) {
			double newX = x0;

			//calculate width of number for proper drawing
			String number = new DecimalFormat("#.#").format(lower);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(number);

			g2.draw(new Line2D.Double(newX, yf, newX, yf + 10));

			//Dont label every x tic to prevent clutter based off of window width
//			System.out.println(getWidth());
			if(i % (double) 1 == 0) g2.drawString(number, (float) (newX - (width / 2.0)), (float) yf + 25);

			//add grid lines to chart
			if(i != 0) {
				Stroke tempS = g2.getStroke();
				Color tempC = g2.getColor();

				g2.setColor(Color.lightGray);
				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));

				g2.draw(new Line2D.Double(newX, yTickYMax, newX, getHeight() - (getHeight() - yTickYMin)));

				g2.setColor(tempC);
				g2.setStroke(tempS);
			}
			lower += 1;
			x0 = newX + distance;
		}
	}

	private void plotFieldBorder(Graphics2D g2) {
		double[][] border = new double[][]{
				{54.3333, 0},
				{54.3333, 27.0},
				{0, 27.0},
		};

		int w = super.getWidth(), h = super.getHeight();

		// Draw lines.
		double xScale = (double) w / upperXtic, yScale = (double) h / upperYtic;

		for(int i = 0; i < border.length - 1; i++)
			for(int j = 0; j < border[0].length - 1; j++) {
				double x1 = xScale * border[i][j];
				double x2 = xScale * border[i + 1][j];
				double y1 = h - yScale * border[i][j + 1];
				double y2 = h - yScale * border[i + 1][j + 1];

				g2.setPaint(Color.black);
				if(x1 == x2) {
					x1vert = x1;
					g2.draw(new Line2D.Double(x1, y1 - (getHeight() - yTickYMin), x2, y2 + yTickYMax));
				} else if(y1 == y2)
					g2.draw(new Line2D.Double(x1, y1 + yTickYMax, x2 + 30, y2 + yTickYMax));
			}
	}

	@Override
	public void lostOwnership(Clipboard clip, Transferable transferable) {
		//We must keep the object we placed on the system clipboard
		//until this method is called.
	}

	private double[][] convertPointArray(ArrayList<Point> p) {
		double[][] temp = new double[p.size()][2];
		IntStream.range(0, p.size()).forEach(i -> {
			temp[i][0] = p.get(i).x;
			temp[i][1] = p.get(i).y;
		});
		return temp;
	}

	private String output2DArray() {
		StringBuilder output = new StringBuilder();
		for(Entry<String, ArrayList<Point>> entry : paths.entrySet()) {
			output.append(String.format("public static double[][] %s = new double[][]{", entry.getKey()));
			output.append(entry.getValue().stream().map(point -> "{" + (point.x - entry.getValue().get(0).x)
					+ ", " + (point.y - entry.getValue().get(0).y) + "},\n").collect(Collectors
					.joining("", "", "};\n")));
		}
		System.out.println(output);
		return output.toString();
	}

	private void updatePath(ArrayList<Point> p) {
		System.out.println("This shouldn't run");
		selectedPath = new MPGen2D(convertPointArray(p), 5.0, 0.02, 3.867227572441874);
		selectedPath.calculate();
		if(selectedPath.smoothPath != null)
			IntStream.range(0, selectedPath.smoothPath.length).forEach(i -> {
				currentPath.add(new Point(selectedPath.smoothPath[i][0], selectedPath.smoothPath[i][1]));
//				System.out.println(currentPath.get(i).x + " " + currentPath.get(i).y);
			});
		fig.repaint();
	}

	private void updateDrawnPath(Point p) {
		currentPath.add(p);
		fig.repaint();
	}

	private class Point {
		double x, y;

		Point(double x, double y) {
			this.x = x;
			this.y = y;
		}
	}

	class WindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			if(!paths.isEmpty() || !inputs.isEmpty()) {
				int response = JOptionPane.showConfirmDialog(e.getComponent(), "Do you want to save your points?", "Point Saver",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if(response == JOptionPane.YES_OPTION) {
					try {
						Calendar c = Calendar.getInstance();
						JFileChooser jfc = new JFileChooser();
						jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "text"));
						jfc.setSelectedFile(new File(String.format("%tB%te%tY-%tl%tM%tS%tp.txt", c, c, c, c, c, c, c)));
						if(jfc.showSaveDialog(e.getComponent()) == JFileChooser.APPROVE_OPTION) {
							if(!jfc.getSelectedFile().getAbsolutePath().endsWith(".txt") && !jfc.getSelectedFile().getAbsolutePath().endsWith(".TXT"))
								jfc.setSelectedFile(new File(jfc.getSelectedFile().getAbsolutePath() + ".txt"));
							FileWriter fw = new FileWriter(jfc.getSelectedFile().getAbsolutePath());
							if(inputs.size() != 0) updatePath(inputs);
							fw.write(output2DArray());
							fw.flush();
							fw.close();
							JOptionPane.showConfirmDialog(e.getComponent(), "Points Saved Successfully!", "Points Saver",
									JOptionPane.DEFAULT_OPTION);
							System.exit(0);
						}
					} catch(IOException e1) {
						e1.printStackTrace();
					}
				} else if(response == JOptionPane.NO_OPTION) System.exit(0);
			} else System.exit(0);
		}
	}

	class KeyboardListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			System.out.println(e.isControlDown() + " " + e.getExtendedKeyCode());
			if(e.isControlDown() && (e.getExtendedKeyCode() == 90)) {//CTRL + Z
				if(inputs.size() > 0) {
					undoRedoCounter++;
					inputs.remove(inputs.size() - 1);
					currentPath.remove(currentPath.size() - 1);
					updatePath(inputs);
				} else {
					JOptionPane.showConfirmDialog(e.getComponent(), "No More Undos!", "", JOptionPane.DEFAULT_OPTION);
					fig.repaint();
				}
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 89)) {//CTRL + Y
				if(undoRedoCounter != 0 && !(undoRedoCounter > inputsBuffer.size())) {
					inputs.add(inputsBuffer.get(inputsBuffer.size() - undoRedoCounter));
					currentPath.remove(currentPath.size() - 1);
					updatePath(inputs);
					undoRedoCounter--;
				} else {
					JOptionPane.showConfirmDialog(e.getComponent(), "No More Redos!", "", JOptionPane.DEFAULT_OPTION);
					fig.repaint();
				}
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 67)) {//CTRL + C, Keycode for S = 83
				if(inputs.size() != 0) updatePath(inputs);
//				paths.put(new ArrayList<>(), new ArrayList<>());//Put a dummy entry in to protect the original path
				Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
				c.setContents(new StringSelection(output2DArray()), fig);
				JOptionPane.showConfirmDialog(e.getComponent(), "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 78) && inputs.size() != 0) {//CTRL + N
				paths.put(String.format("path%d", paths.size() + 1), currentPath);
				inputs.clear();
				currentPath.clear();
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 68)) {
				draw = !draw;
				JOptionPane.showMessageDialog(e.getComponent(), String.format("Draw mode set to %b", draw), "Draw Mode Status",
						JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private boolean previousDraw = false;

	class MouseListener extends MouseAdapter {
		public void mouseDragged(MouseEvent ev) {
			//Draw mode essentially allows you to specify a free-flowing path for the robot to follow.
			//Instead of clicking multiple waypoints, it automatically takes your cursor location as
			//a waypoint, adds that to the list of waypoints and then updates the GUI.
			if(draw) updateWaypoints(true);
		}

		public void mouseClicked(MouseEvent ev) {
			updateWaypoints(false);
		}

		private void updateWaypoints(boolean draw) {
			//Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
			java.awt.Point p = g.getRootPane().getMousePosition();
			double x = constrainTo((p.getX() - 30), rect.getWidth()) / xScale;
			double y = constrainTo(((height - 30) - p.getY()), rect.getHeight()) / yScale;
			if(draw) {
				if(previousDraw) {//Handles staying at draw mode
					System.out.println("spicy");
					updateDrawnPath(new Point(x, y));
				} else {//Handles going from click to draw mode
					System.out.println("pls");
					currentPath.clear();
					inputs.add(new Point(x, y));
					updatePath(inputs);
				}
			} else {
				if(previousDraw) {//Handles going from draw to click mode
					System.out.println("SAJEGNJKGNJKASN");
					inputs.clear();
					inputs.add(currentPath.get(currentPath.size() - 1));
					inputs.add(new Point(x, y));
					selectedPath = new MPGen2D(convertPointArray(inputs), 5.0, 0.02, 3.867227572441874);
					selectedPath.calculate();
					currentPath = new ArrayList<>(previousPath);
					if(selectedPath.smoothPath != null)
						IntStream.range(0, selectedPath.smoothPath.length).forEach(i -> {
							currentPath.add(new Point(selectedPath.smoothPath[i][0], selectedPath.smoothPath[i][1]));
						});
					fig.repaint();
				} else {//Handles staying at click mode
					System.out.println("dank");
					currentPath.clear();
					inputs.add(new Point(x, y));
					updatePath(inputs);
				}
			}
			//Every time a new point is added, clear the undo/redo buffers and re-add all the points in the current path to them
			undoRedoCounter = 0;
			inputsBuffer.clear();
			IntStream.range(0, inputs.size()).forEach(i -> inputsBuffer.add(i, inputs.get(i)));
			System.out.println(previousDraw + " " + draw);
			previousDraw = draw;
			previousPath = new ArrayList<>(currentPath);
		}
	}
}