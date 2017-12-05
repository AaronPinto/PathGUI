package GUIStuff;

import javax.swing.*;
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
	private boolean drag = false;
	private double upperXtic = 54.33333, upperYtic = 27, height, xScale, yScale,
			yTickYMax = 0, yTickYMin = 0, x1vert = 0;
	private Rectangle rect;
	private ArrayList<Double> xInputs = new ArrayList<>(), yInputs = new ArrayList<>(),
			xInputsBuffer = new ArrayList<>(), yInputsBuffer = new ArrayList<>(),
			xDragInputs = new ArrayList<>(), yDragInputs = new ArrayList<>();
	private LinkedHashMap<ArrayList<Double>, ArrayList<Double>> paths = new LinkedHashMap<>();

	//Link List to hold all different plots on one graph.
	private LinkedList<xyNode> link = new LinkedList<>();

	/**
	 * Constructor which Plots only Y-axis data.
	 *
	 * @param yData is a array of doubles representing the Y-axis values of the data to be plotted.
	 */
	public PathGUITool(double[] yData) {
		this(null, yData, Color.red);
	}

	public PathGUITool(double[] yData, Color lineColor, Color marker) {
		this(null, yData, lineColor, marker, false);
	}

	/**
	 * Constructor which Plots chart based on provided x and y data. X and Y arrays must be of the same length.
	 *
	 * @param xData is an array of doubles representing the X-axis values of the data to be plotted.
	 * @param yData is an array of double representing the Y-axis values of the data to be plotted.
	 */
	public PathGUITool(double[] xData, double[] yData) {
		this(xData, yData, Color.red, null, false);
	}

	/**
	 * Constructor which Plots chart based on provided x and y axis data.
	 *
	 * @param data is a 2D array of doubles of size Nx2 or 2xN. The plot assumes X is the first dimension, and y data
	 *             is the second dimension.
	 */
	public PathGUITool(double[][] data) {
		this(getXVector(data), getYVector(data), Color.red, null, false);
	}

	/**
	 * Constructor which plots charts based on provided x and y axis data in a single two dimensional array.
	 *
	 * @param data        is a 2D array of doubles of size Nx2 or 2xN. The plot assumes X is the first dimension, and y data
	 *                    is the second dimension.
	 * @param lineColor   is the color the user wishes to be displayed for the line connecting each datapoint
	 * @param markerColor is the color the user which to be used for the data point. Make this null if the user wishes to
	 *                    not have datapoint markers.
	 */
	public PathGUITool(double[][] data, Color lineColor, Color markerColor) {
		this(getXVector(data), getYVector(data), lineColor, markerColor, false);
	}

	/**
	 * Constructor which plots charts based on provided x and y axis data provided as separate arrays. The user can also
	 * specify the color of the adjoining line.
	 * Data markers are not displayed.
	 *
	 * @param xData     is an array of doubles representing the X-axis values of the data to be plotted.
	 * @param yData     is an array of double representing the Y-axis values of the data to be plotted.
	 * @param lineColor is the color the user wishes to be displayed for the line connecting each datapoint
	 */
	public PathGUITool(double[] xData, double[] yData, Color lineColor) {
		this(xData, yData, lineColor, null, false);
	}

	public PathGUITool(double[][] data, Color lineColor) {
		this(getXVector(data), getYVector(data), lineColor, null, false);
	}

	/**
	 * Constructor which plots charts based on x and y axis data selected on the Field GUI.
	 *
	 * @param dragMode is laggy af so dw about it
	 */
	public PathGUITool(boolean dragMode) {
		this(null, null, null, null, dragMode);
	}

	/**
	 * Constructor which plots charts based on provided x and y axis data, provided as separate arrays. The user
	 * can also specify the color of the adjoining line and the color of the datapoint maker.
	 *
	 * @param xData       is an array of doubles representing the X-axis values of the data to be plotted.
	 * @param yData       is an array of double representing the Y-axis values of the data to be plotted.
	 * @param lineColor   is the color the user wishes to be displayed for the line connecting each datapoint
	 * @param markerColor is the color the user which to be used for the data point. Make this null if the user wishes to
	 *                    not have datapoint markers.
	 * @param d           is the boolean that handles if the GUI is in mouse-drag mode or not.
	 */
	public PathGUITool(double[] xData, double[] yData, Color lineColor, Color markerColor, boolean d) {
		upperXtic = -Double.MAX_VALUE;
		upperYtic = -Double.MAX_VALUE;
		drag = d;

		link.add(new xyNode(new double[]{0}, new double[]{0}, false, null, null));

		if(yData != null) addData(xData, yData, lineColor, markerColor);

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

	private static double[] getXVector(double[][] arr) {
		return Arrays.stream(arr).mapToDouble(doubles -> doubles[0]).toArray();
	}

	private static double[] getYVector(double[][] arr) {
		return Arrays.stream(arr).mapToDouble(doubles -> doubles[1]).toArray();
	}

	private static double constrainTo(double value, double maxConstrain) {
		return Math.max(0.0, Math.min(maxConstrain, value));
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

	public void addData(double[] y, Color lineColor) {
		addData(y, lineColor, null);
	}

	public void addData(double[] y, Color lineColor, Color marker) {
		//cant add y only data unless all other data is y only data
		link.stream().filter(data -> data.x != null).forEach(data -> {
			throw new Error("All previous chart series need to have only Y data arrays");
		});

		addData(null, y, lineColor, marker);
	}

	public void addData(double[] x, double[] y, Color lineColor) {
		addData(x, y, lineColor, null);
	}

	public void addData(double[][] data, Color lineColor) {
		addData(getXVector(data), getYVector(data), lineColor, null);
	}

	public void addData(double[][] data, Color lineColor, Color marker) {
		if(data != null)
			addData(getXVector(data), getYVector(data), lineColor, marker);
	}

	public void addData(double[] x, double[] y, Color lineColor, Color marker) {
		xyNode Data = new xyNode();

		//copy y array into node
		Data.y = new double[y.length];
		Data.lineColor = lineColor;

		if(marker == null)
			Data.lineMarker = false;
		else {
			Data.lineMarker = true;
			Data.markerColor = marker;
		}
		System.arraycopy(y, 0, Data.y, 0, y.length);

		//if X is not null, copy x
		if(x != null) {
			//cant add x, and y data unless all other data has x and y data
			link.stream().filter(data -> data.x == null).forEach(data -> {
				throw new Error("All previous chart series need to have both X and Y data arrays");
			});
			if(x.length != y.length)
				throw new Error("X dimension must match Y dimension");
			Data.x = new double[x.length];
			System.arraycopy(x, 0, Data.x, 0, x.length);
		}
		link.add(Data);
	}

	private void plot(Graphics2D g2) {
		int h = super.getHeight();
		Color tempC = g2.getColor();

		//loop through list and plot each
		for(xyNode aLink : link)
			for(int j = 0; j < aLink.y.length - 1; j++) {
				double x1, x2;

				if(aLink.x == null) {
					x1 = 30 + j * xScale;
					x2 = 30 + (j + 1) * xScale;
				} else {
					x1 = 30 + xScale * aLink.x[j];
					x2 = 30 + xScale * aLink.x[j + 1];
				}

				double y1 = h - 30 - yScale * aLink.y[j], y2 = h - 30 - yScale * aLink.y[j + 1];
				g2.setPaint(aLink.lineColor);
				g2.draw(new Line2D.Double(x1, y1, x2, y2));

				if(aLink.lineMarker) {
					g2.setPaint(aLink.markerColor);
					g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
					g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));
				}
			}

		paths.forEach((key, value) -> {
			selectedPath = new MPGen2D(mergeArrays(key, value), 3.0, 0.02, 3.867227572441874);
			selectedPath.calculate();
			IntStream.range(0, selectedPath.smoothPath.length - 1).forEach(i -> {
				double x1 = 30 + xScale * selectedPath.smoothPath[i][0], x2 = 30 + xScale * selectedPath.smoothPath[i + 1][0];
				double y1 = h - 30 - yScale * selectedPath.smoothPath[i][1], y2 = h - 30 - yScale * selectedPath.smoothPath[i + 1][1];
				g2.setPaint(Color.green);
				g2.draw(new Line2D.Double(x1, y1, x2, y2));
				g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
				g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));
			});
		});
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

			//dont label every x tic to prevent clutter
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

	public void updateData(int series, double[][] data, Color line, Color marker) {
		addData(data, line, marker);

		link.get(series).x = link.getLast().x.clone();
		link.get(series).y = link.getLast().y.clone();
		link.removeLast();
	}

	@Override
	public void lostOwnership(Clipboard clip, Transferable transferable) {
		//We must keep the object we placed on the system clipboard
		//until this method is called.
	}

	private double[][] mergeArrays(ArrayList<Double> x, ArrayList<Double> y) {
		double[][] temp = new double[x.size()][2];
		IntStream.range(0, x.size()).forEach(i -> {
			temp[i][0] = x.get(i);
			temp[i][1] = y.get(i);
		});
		return temp;
	}

	private void updatePath() {
		selectedPath = new MPGen2D(mergeArrays(xDragInputs, yDragInputs), 3.0, 0.02, 3.867227572441874);
		selectedPath.calculate();
		fig.addData(selectedPath.smoothPath, Color.green, Color.green);
		fig.repaint();
	}

	private class xyNode {
		double[] x;
		double[] y;
		boolean lineMarker;
		Color lineColor;
		Color markerColor;

		xyNode() {
			x = null;
			y = null;
			lineMarker = false;
		}

		xyNode(double[] xArr, double[] yArr, boolean lm, Color l, Color m) {
			x = xArr;
			y = yArr;
			lineMarker = lm;
			lineColor = l;
			markerColor = m;
		}
	}

	class WindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			int response = JOptionPane.showConfirmDialog(e.getComponent(), "Do you want to save your points?", "Point Saver",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if(response == JOptionPane.YES_OPTION) {
				try {
					Calendar c = Calendar.getInstance();
					File f = new File(System.getenv("USERPROFILE") + "\\Documents\\PathGUIToolSaves");
					if(!f.exists()) f.mkdir();
					FileWriter fw = new FileWriter(f.getPath() + String.format("\\%tB%te%tY-%tl%tM%tS%tp.txt", c, c, c, c, c, c, c));
					if(xInputs.size() != 0) paths.put(new ArrayList<>(xInputs), new ArrayList<>(yInputs));
					StringBuilder output = new StringBuilder();
					paths.forEach((key, value) -> {
						IntStream.range(0, value.size()).forEach(i -> output.append("{").append(key.get(i)).append(", ").append(value.get(i)).append("},\n"));
						output.append("\n");
					});
					System.out.println(output);
					fw.write(output.toString());
					fw.flush();
					fw.close();
					JOptionPane.showConfirmDialog(e.getComponent(), "Points Saved Successfully!", "Points Saver",
							JOptionPane.DEFAULT_OPTION);
					System.exit(0);
				} catch(IOException e1) {
					e1.printStackTrace();
				}
			} else if(response == JOptionPane.NO_OPTION) System.exit(0);
		}
	}

	class KeyboardListener extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			System.out.println(e.isControlDown() + " " + e.getExtendedKeyCode());
			if(e.isControlDown() && (e.getExtendedKeyCode() == 90)) {//CTRL + Z
				if(xInputs.size() > 0) {
					undoRedoCounter++;
					xInputs.remove(xInputs.size() - 1);
					yInputs.remove(yInputs.size() - 1);
					link.removeLast();
					updatePath();
				} else {
					JOptionPane.showConfirmDialog(e.getComponent(), "No More Undos!", "", JOptionPane.DEFAULT_OPTION);
					fig.repaint();
				}
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 89)) {//CTRL + Y
				if(undoRedoCounter != 0 && !(undoRedoCounter > xInputsBuffer.size())) {
					xInputs.add(xInputsBuffer.get(xInputsBuffer.size() - undoRedoCounter));
					yInputs.add(yInputsBuffer.get(yInputsBuffer.size() - undoRedoCounter));
					if(!link.isEmpty())
						link.removeLast();
					updatePath();
					undoRedoCounter--;
				} else {
					JOptionPane.showConfirmDialog(e.getComponent(), "No More Redos!", "", JOptionPane.DEFAULT_OPTION);
					fig.repaint();
				}
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 67)) {//CTRL + C, Keycode for S = 83
				StringBuilder output = new StringBuilder();
				if(xInputs.size() != 0) paths.put(new ArrayList<>(xInputs), new ArrayList<>(yInputs));
				if(drag && xDragInputs.size() != 0) paths.put(new ArrayList<>(xDragInputs), new ArrayList<>(yDragInputs));
				for(Entry<ArrayList<Double>, ArrayList<Double>> entry : paths.entrySet())
					output.append(IntStream.range(0, entry.getValue().size()).mapToObj(i -> "{" + (entry.getKey().get(i) - entry.getKey().get(0))
							+ ", " + (entry.getValue().get(i) - entry.getValue().get(0)) + "},\n").collect(Collectors.joining("", "", "\n")));
				System.out.println(output);
				Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
				c.setContents(new StringSelection(output.toString()), fig);
				JOptionPane.showConfirmDialog(e.getComponent(), "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 78) && xInputs.size() != 0) {//CTRL + N
				paths.put(new ArrayList<>(xInputs), new ArrayList<>(yInputs));
				fig.repaint();
				xInputs.clear();
				yInputs.clear();
				if(drag && xDragInputs.size() != 0) {
					paths.put(new ArrayList<>(xDragInputs), new ArrayList<>(yDragInputs));
					xDragInputs.clear();
					yDragInputs.clear();
				}
			}
			if(e.isControlDown() && (e.getExtendedKeyCode() == 68)) {
				drag = !drag;
				JOptionPane.showMessageDialog(e.getComponent(), String.format("Drag mode set to %b", drag), "Drag Mode Status", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	class MouseListener extends MouseAdapter {
		public void mouseDragged(MouseEvent ev) {
			//Drag mode essentially allows you to specify a free-flowing path for the robot to follow
			//Instead of clicking multiple waypoints, it automatically takes your cursor location as
			//A waypoint, adds that to the list of waypoints and then updates the GUI.
			//It does not generate a new path every time because of the amount of waypoints that accumulate over time
			//But if it were to generate a path, it would look very similar
			if(drag) {
				Point p = g.getRootPane().getMousePosition();
				double x = (constrainTo((p.getX() - 30), rect.getWidth())) / xScale;
				double y = (constrainTo(((height - 30) - p.getY()), rect.getHeight())) / yScale;
				xDragInputs.add(x);
				yDragInputs.add(y);
				System.out.println("(" + x + ", " + y + ") " + xScale + " " + yScale + " " + g.getRootPane().getMousePosition());
			}
		}

		public void mouseClicked(MouseEvent ev) {
			Point p = g.getRootPane().getMousePosition();
			double x = constrainTo((p.getX() - 30), rect.getWidth());
			double y = constrainTo(((height - 30) - p.getY()), rect.getHeight());
			double xField = x / xScale, yField = y / yScale;
			xInputs.add(xField);
			yInputs.add(yField);
			if(!link.isEmpty()) link.removeLast();
			updatePath();
			//Every time a new point is added, clear the undo/redo buffers and re-add all the points in the current path to them
			undoRedoCounter = 0;
			xInputsBuffer.clear();
			yInputsBuffer.clear();
			IntStream.range(0, xInputs.size()).forEach(i -> {
				xInputsBuffer.add(i, xInputs.get(i));
				yInputsBuffer.add(i, yInputs.get(i));
			});
			System.out.println("(" + x + ", " + y + ") " + xField + " " + yField + " " + xScale + " " + yScale + " " +
					g.getRootPane().getMousePosition());
		}
	}
}