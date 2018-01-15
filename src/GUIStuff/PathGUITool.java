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
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Aaron Pinto
 * ICS4UR
 * Mr. Le
 * January 22nd, 2018
 * <p>
 * The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse button is
 * clicked, it adds that point to a series of points, called a PathSegment, and generates a path accordingly. This is called
 * click mode. The path generation will either be based off of the generator that we currently use or I will redo it using cubic
 * spline interpolation. Either way, the filename is MPGen2D. Drag mode is toggleable off/on (Ctrl + D) and by default it is on,
 * which means you can use it in tandem with click mode, and it automatically switches depending on whether or not you hold the
 * left button on your mouse down or not. Drag mode essentially allows you to draw a path yourself without having to generate one
 * each time. It allows for more freedom of design regarding the path that the robot follows. The program also has various
 * keyboard shortcuts to make it more intuitive and easy to use. Ctrl + C to copy points on the path in proper Java syntax
 * for a 2D array, Ctrl + N to keep the old paths and start a new one if, for example, you want the robot to follow
 * multiple paths in auto, Ctrl + Z removes each point on the current path and Ctrl + Y adds that point back plus you can
 * hold down the keys and it will rapidly get rid of each point, and Ctrl + O to open and parse a file containing waypoints
 * in proper Java 2D array format, and then display the paths from that file.
 * <p>
 * For more information on the field, see: https://www.youtube.com/watch?v=HZbdwYiCY74 and
 * https://firstfrc.blob.core.windows.net/frc2018/Drawings/LayoutandMarkingDiagram.pdf
 * <p>
 * For version history see: https://github.com/AaronPinto/PathGUI
 * @see BetterArrayList
 * @see FieldGenerator
 * @see Polygon2D
 * @see MPGen2D
 * @see Point
 * @see PathSegment
 * <p>
 * References:
 * This class refers to https://github.com/KHEngineering/SmoothPathPlanner/blob/master/src/usfirst/frc/team2168/robot/FalconLinePlot.java
 * and MPGen2D refers to https://github.com/KHEngineering/SmoothPathPlanner/blob/master/src/usfirst/frc/team2168/robot/FalconPathPlanner.java
 * I got Polygon2D.java from online, I can't find the exact class though and I deleted a bunch of unnecessary code in the one I have.
 * I found FileChooserDemo.java online as well to start me off with the Save and Open File Dialogs.
 * https://docs.oracle.com/javase/tutorial/uiswing/examples/components/FileChooserDemoProject/src/components/FileChooserDemo.java
 */
public class PathGUITool extends JPanel implements ClipboardOwner {
	//The path generator object that also stores the points of the generator path
	private static MPGen2D pathGen;

	//An object of this class, used for the Ctrl + C code
	private static PathGUITool fig;

	//The JFrame for this GUI. It actually displays the window
	private JFrame g = new JFrame("Path GUI Tool");

	//doubles for storing difference size and scaling values
	private double upperXtic = -Double.MAX_VALUE, upperYtic = -Double.MAX_VALUE, height, xScale, yScale, yTickYMax = 0, yTickYMin = 0,
			rektWidth, rektHeight;

	//A BetterArrayList of PathSegments which is representative of the current path.
	private BetterArrayList<PathSegment> currentPath = new BetterArrayList<>();

	//LinkedHashMap to store the name and path for all previous paths
	private LinkedHashMap<String, BetterArrayList<PathSegment>> paths = new LinkedHashMap<>();

	//booleans for handling different conditions, previousDraw stores the most recent draw mode state, drawMode stores the
	//current draw mode state, shouldSmooth is true when the points should be smoothed from a drawn to clicked path, and
	//firstUndoRedo is set to false when Undo is pressed for the first time
	private boolean previousDraw = false, drawMode = true, shouldSmooth = false, firstUndoRedo = false;

	//Enum object to store the previous mode value
	private PrevMode pm;

	//An object array to store the index values necessary to locate a movable point in paths.
	private Object[] moveflag = new Object[]{-1, -1, -1};

	//Field generator object that contains all of the necessary shapes for the field drawing
	private FieldGenerator fg = new FieldGenerator();

	//ArrayDeque for storing the Ctrl + Z'd points
	//add() adds last, peek() gets first
	private Deque<PathSegment> redoBuffer = new ArrayDeque<>();

	//A String that stores the name of the field element you cannot create a point/path in/through.
	private String invalidElementName = "";

	/**
	 * Constructor.
	 */
	private PathGUITool() {
		//Set the properties of this JFrame
		g.add(this);
		g.setSize(1280, 720);//Be nice to people who don't have 1080p displays and set the size to 720p :P
		g.setLocationByPlatform(true);
		g.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		g.setVisible(true);

		//Add listeners to this JFrame. Listeners, well, listen for their respective events to be triggered and call the
		//corresponding function. Ex: if a mouse button is pressed the MouseListener calls mousePressed(MouseEvent e)
		g.addWindowListener(new WindowListener());
		g.addMouseListener(new MouseListener());
		g.addMouseMotionListener(new MouseListener());
		g.addKeyListener(new KeyboardListener());
	}

	/**
	 * A simple function that constrains a value form 0.0 to the specified maximum bound
	 *
	 * @param value        The value to constrain
	 * @param maxConstrain The maximum bound
	 * @return the constrained value
	 */
	private static double constrainTo(double value, double maxConstrain) {
		return Math.max(0.0, Math.min(maxConstrain, value));
	}

	/**
	 * Main function
	 */
	public static void main(String[] args) {
		fig = new PathGUITool();
	}

	/**
	 * The central method which paints the panel and shows the figure and is called every time to update the GUI.
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
		Line2D.Double xaxis = new Line2D.Double(30, height - 30, width - 12, height - 30);
		g2.draw(yaxis);
		g2.draw(xaxis);

		//draw ticks (the light gray grid lines and the numbers)
		double yMax = 27.0, xMax = 54.0;
		drawYTickRange(g2, yaxis, yMax);
		drawXTickRange(g2, xaxis, xMax);

		//draw the field and everything on it
		plotFieldBorder(g2, xMax, yMax);
		rektWidth = (xaxis.getX2() - xaxis.getX1());
		rektHeight = (yaxis.getY2() - yaxis.getY1());
		xScale = rektWidth / xMax;
		yScale = rektHeight / yMax;
		fg.plotFieldElements(g2, super.getHeight(), xScale, yScale);
		g2.setColor(Color.black);
		Rectangle rekt = new Rectangle((int) yaxis.getX1(), (int) yaxis.getY1(), (int) rektWidth, (int) rektHeight);
		g2.draw(rekt);

		//Hides excess grid lines
		g2.setColor(super.getBackground());
		g2.fillRect(rekt.x + rekt.width + 1, rekt.y, (int) width - rekt.width - 30, rekt.height + 1);

		//plot data
		plot(g2);
	}

	private void plotPath(Graphics2D g2, int h, BetterArrayList<PathSegment> path) {
		System.out.println("number of path segments: " + path.size() + " " + pm);
		path.forEach(aPath -> {
			System.out.println(aPath.pathSegPoints.size() + " " + aPath.clickPoints.size() + " " + aPath.isDrawn);
			if(aPath.clickPoints.size() > 1)
				IntStream.range(0, aPath.clickPoints.size() - 1).forEach(j -> {
					double x1 = 30 + xScale * aPath.clickPoints.get(j).x, x2 = 30 + xScale * aPath.clickPoints.get(j + 1).x;
					double y1 = h - 30 - yScale * aPath.clickPoints.get(j).y, y2 = h - 30 - yScale * aPath.clickPoints.get(j + 1).y;
					g2.setPaint(Color.magenta);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
					g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
					g2.fill(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
				});
			else if(aPath.clickPoints.size() == 1) {
				double x1 = 30 + xScale * aPath.clickPoints.get(0).x, y1 = h - 30 - yScale * aPath.clickPoints.get(0).y;
				g2.setPaint(Color.magenta);
				g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
			}

			if(aPath.pathSegPoints.size() > 1)
				IntStream.range(0, aPath.pathSegPoints.size() - 1).forEach(j -> {
					double x1 = 30 + xScale * aPath.pathSegPoints.get(j).x, x2 = 30 + xScale * aPath.pathSegPoints.get(j + 1).x;
					double y1 = h - 30 - yScale * aPath.pathSegPoints.get(j).y, y2 = h - 30 - yScale * aPath.pathSegPoints.get(j + 1).y;
					g2.setPaint(Color.green);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
					g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
					g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));
				});
			else if(aPath.pathSegPoints.size() == 1) {
				double x1 = 30 + xScale * aPath.pathSegPoints.get(0).x, y1 = h - 30 - yScale * aPath.pathSegPoints.get(0).y;
				g2.setPaint(Color.green);
				g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
			}
		});
	}

	private void plot(Graphics2D g2) {
		int h = super.getHeight();
		Color tempC = g2.getColor();

		//plot the current path then loop through paths and plot each
		plotPath(g2, h, currentPath);
		paths.forEach((key, value) -> plotPath(g2, h, value));
		g2.setColor(tempC);
	}

	/**
	 * Just so you don't get confused, this draws the horizontal light gray lines.
	 *
	 * @param g2    The Graphics2D object for this window
	 * @param yaxis The line that represents the y-axis
	 * @param Max   The width of the field in feet
	 */
	private void drawYTickRange(Graphics2D g2, Line2D yaxis, double Max) {
		upperYtic = Math.ceil(Max);

		double x0 = yaxis.getX1();
		double y0 = yaxis.getY1();
		double xf = yaxis.getX2();
		double yf = yaxis.getY2();

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

	/**
	 * Just so you don't get confused, this draws the vertical light gray lines.
	 *
	 * @param g2    The Graphics2D object for this window
	 * @param xaxis The line that represents the x-axis
	 * @param Max   The width of the field in feet
	 */
	private void drawXTickRange(Graphics2D g2, Line2D xaxis, double Max) {
		upperXtic = Math.ceil(Max);

		double x0 = xaxis.getX1();
		double y0 = xaxis.getY1();
		double xf = xaxis.getX2();
		double yf = xaxis.getY2();

		//calculate stepsize between ticks and length of Y axis using distance formula
		double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperXtic;

		double lower = 0;
		for(int i = 0; i <= Max; i++) {
			double newX = x0;

			//calculate width of number for proper drawing
			String number = new DecimalFormat("#.#").format(lower);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(number);

			g2.draw(new Line2D.Double(newX, yf, newX, yf + 10));

			//Don't label every x tic to prevent clutter based off of window width
			//TODO: WHAT THAT COMMENT ABOVE SAYS LOL
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

	private void plotFieldBorder(Graphics2D g2, double xMax, double yMax) {
		double[][] border = new double[][]{
				{xMax, 0},
				{xMax, yMax},
				{0, yMax},
		};

		int w = super.getWidth(), h = super.getHeight();

		// Draw lines.
		double xScale = (double) w / upperXtic, yScale = (double) h / upperYtic;

		for(int i = 0; i < border.length - 1; i++)
			for(int j = 0; j < border[0].length - 1; j++) {
				double x1 = xScale * border[i][j], x2 = xScale * border[i + 1][j], y1 = h - yScale * border[i][j + 1],
						y2 = h - yScale * border[i + 1][j + 1];
				g2.setPaint(Color.black);
				if(x1 == x2)
					g2.draw(new Line2D.Double(x1, y1 - (getHeight() - yTickYMin), x2, y2 + yTickYMax));
				else if(y1 == y2)
					g2.draw(new Line2D.Double(x1, y1 + yTickYMax, x2 + 30, y2 + yTickYMax));
			}
	}

	@Override
	public void lostOwnership(Clipboard clip, Transferable transferable) {
		//We must keep the object we placed on the system clipboard
		//until this method is called.
	}

	private double[][] convertPointArray(BetterArrayList<Point> p) {
		double[][] temp = new double[p.size()][2];
		IntStream.range(0, p.size()).forEach(i -> {
			temp[i][0] = p.get(i).x;
			temp[i][1] = p.get(i).y;
		});
		return temp;
	}

	private BetterArrayList<Point> convert2DArray(double[][] p) {
		if(p == null)
			return new BetterArrayList<>(0);
		return Arrays.stream(p).map(aP -> new Point(aP[0], aP[1])).collect(Collectors.toCollection(BetterArrayList::new));
	}

	private String output2DArray() {
		StringBuilder output = new StringBuilder();
		output.append("public static Object[][] currentPath = new Object[][]{\n");
		outputPath(output, currentPath);

		paths.forEach((key, value) -> {
			output.append(String.format("public static Object[][] %s = new Object[][]{\n", key));
			outputPath(output, value);
		});
		System.out.println(output);
		return output.toString();
	}

	private void outputPath(StringBuilder output, BetterArrayList<PathSegment> value) {
		for(PathSegment path : value)
			if(path.isDrawn) {
				output.append("{").append(true).append(", ").append(true).append("},\n");//Indicator that the path is drawn (used for parsing)
				output.append(path.pathSegPoints.stream().map(pathSegPoint -> "{" + pathSegPoint.values() + "},\n").collect(Collectors.joining()));
			} else {
				output.append("{").append(false).append(", ").append(false).append("},\n");//Indicator that the path is not drawn (used for parsing)
				output.append(path.clickPoints.stream().map(clickPoint -> "{" + clickPoint + "},\n").collect(Collectors.joining()));
			}
		output.append("};\n");
	}

	private boolean genPath(InputEvent e) {
		pathGen = new MPGen2D(convertPointArray(currentPath.getLast().clickPoints), 5.0, 0.02, 3.867227572441874);
		pathGen.calculate();
		if(pathGen.smoothPath != null)
			if(validatePathSegment(convert2DArray(pathGen.smoothPath))) {
				currentPath.getLast().pathSegPoints = convert2DArray(pathGen.smoothPath);
				return true;
			} else {
				showFieldError(e);
				currentPath.getLast().clickPoints.removeLast();
				return false;
			}
		return true;
	}

	private boolean validatePath(BetterArrayList<PathSegment> path) {
		return path.stream().allMatch(pathSegment -> validatePathSegment(pathSegment.pathSegPoints));
	}

	private boolean validatePathSegment(BetterArrayList<Point> b) {
		return IntStream.range(0, b.size() - 1).allMatch(i -> validatePoint(b.get(i).x, b.get(i).y, b.get(i + 1)));
	}

	private boolean validatePoint(double x, double y, Point next) {
		for(Polygon2D p : fg.invalidAreas)
			if(p.contains(x, y) || p.intersects(x, y, next)) {
				invalidElementName = p.name;
				return false;
			}
		return true;
	}

	private enum PrevMode {
		DRAW, CLICKDRAW, DRAWCLICK, CLICK, UNDO, REDO
	}

	/**
	 * A path segment can either be drawn or clicked and we need to keep track of that for the different keyboard shortcuts
	 * It also needs to store the points of a path segment and if the path segment is clicked, then it needs to store those waypoints as well
	 */
	static class PathSegment {
		boolean isDrawn;
		BetterArrayList<Point> pathSegPoints;
		BetterArrayList<Point> clickPoints;

		PathSegment(boolean isDrawn) {
			this.isDrawn = isDrawn;
			this.pathSegPoints = new BetterArrayList<>();
			this.clickPoints = new BetterArrayList<>(0);
		}
	}

	/**
	 * A Point is simply a point in 2D space on the field, with double values because
	 * they are also used for storing the value of each point in a path.
	 */
	static class Point {
		double x, y;
		boolean movable;

		Point(double x, double y) {
			this.x = x;
			this.y = y;
			this.movable = true;
		}

		Point(double x, double y, boolean move) {
			this.x = x;
			this.y = y;
			this.movable = move;
		}

		Point(Point p, boolean move) {
			this.x = p.x;
			this.y = p.y;
			this.movable = move;
		}

		@Override
		public String toString() {
			return x + ", " + y + ", " + movable;
		}

		String values() {
			return x + ", " + y;
		}
	}

	class WindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			if(!paths.isEmpty() || (!currentPath.isEmpty() && !currentPath.get(0).pathSegPoints.isEmpty())) {
				int response = JOptionPane.showConfirmDialog(e.getComponent(), "Do you want to save your points?", "Point Saver",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if(response == JOptionPane.YES_OPTION)
					try {
						Calendar c = Calendar.getInstance();
						JFileChooser jfc = new JFileChooser();
						jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "TXT"));
						jfc.setSelectedFile(new File(String.format("%tB%te%tY-%tl%tM%tS%tp.txt", c, c, c, c, c, c, c)));
						if(jfc.showSaveDialog(e.getComponent()) == JFileChooser.APPROVE_OPTION) {
							if(!jfc.getSelectedFile().getAbsolutePath().endsWith(".txt") && !jfc.getSelectedFile().getAbsolutePath().endsWith(".TXT"))
								jfc.setSelectedFile(new File(jfc.getSelectedFile().getAbsolutePath() + ".txt"));
							FileWriter fw = new FileWriter(jfc.getSelectedFile().getAbsolutePath());
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
				else if(response == JOptionPane.NO_OPTION) System.exit(0);
			} else System.exit(0);
		}
	}

	class KeyboardListener extends KeyAdapter {
		private void addPathSegment() {
			if(!currentPath.isEmpty())
				redoBuffer.add(new PathSegment(currentPath.getLast().isDrawn));
		}

		private void removeEmptyPaths() {
			if(currentPath.getLast().pathSegPoints.isEmpty()) {
				currentPath.removeLast();
				addPathSegment();
			} else if(currentPath.getLast().clickPoints.isEmpty() && !currentPath.getLast().isDrawn) {
				currentPath.removeLast();
				addPathSegment();
			}
		}

		private void outputRedoBuffer() {
			for(PathSegment ps : redoBuffer)
				if(ps.isDrawn)
					ps.pathSegPoints.forEach(System.out::println);
				else
					ps.clickPoints.forEach(System.out::println);
		}

		private void addToPathsAndClear() {
			if(!currentPath.isEmpty()) {
				paths.put(String.format("path%d", paths.size() + 1), currentPath);
				currentPath = new BetterArrayList<>();
			}
		}

		public void keyPressed(KeyEvent e) {
			System.out.println(e.isControlDown() + " " + e.getExtendedKeyCode());
			if(e.isControlDown()) {
				if(e.getExtendedKeyCode() == 90) {//CTRL + Z
					if(!currentPath.isEmpty()) {
						System.out.println(currentPath.getLast().pathSegPoints.size());
						removeEmptyPaths();
						if(!currentPath.getLast().pathSegPoints.isEmpty()) {
							if(firstUndoRedo || redoBuffer.isEmpty()) {
								firstUndoRedo = false;
								addPathSegment();
							}
							if(currentPath.getLast().isDrawn)
								redoBuffer.peekLast().pathSegPoints.add(currentPath.getLast().pathSegPoints.removeLast());
							else {
								redoBuffer.peekLast().clickPoints.add(currentPath.getLast().clickPoints.removeLast());
								genPath(e);
							}
						}
						removeEmptyPaths();
						outputRedoBuffer();
						fig.repaint();
						pm = PrevMode.UNDO;
					} else
						JOptionPane.showConfirmDialog(e.getComponent(), "No More Undos!", "Undo Status", JOptionPane.DEFAULT_OPTION);
				} else if(e.getExtendedKeyCode() == 89) {//CTRL + Y
					if(!redoBuffer.isEmpty()) {
						/*
						  You only want to add a path segment to the current path if the current path's current path segment
						  isn't of the same type (drawn or not) as the one in the buffer and of course if the buffer isn't empty
						 */
						if(!redoBuffer.peekLast().clickPoints.isEmpty() && !redoBuffer.peekLast().isDrawn && (currentPath.isEmpty() ||
								currentPath.getLast().isDrawn))
							currentPath.add(new PathSegment(false));
						else if(!redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().isDrawn && (currentPath.isEmpty() ||
								!currentPath.getLast().isDrawn))
							currentPath.add(new PathSegment(true));

						if(currentPath.getLast().isDrawn)
							currentPath.getLast().pathSegPoints.add(redoBuffer.peekLast().pathSegPoints.removeLast());
						else {
							currentPath.getLast().clickPoints.add(redoBuffer.peekLast().clickPoints.removeLast());
							genPath(e);
						}

						if(redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().clickPoints.isEmpty())
							redoBuffer.removeLast();

						outputRedoBuffer();
						fig.repaint();
						pm = PrevMode.REDO;
					} else
						JOptionPane.showConfirmDialog(e.getComponent(), "No More Redos!", "Redo Status", JOptionPane.DEFAULT_OPTION);
				} else if(e.getExtendedKeyCode() == 67) {//CTRL + C, Keycode for S = 83
					Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
					c.setContents(new StringSelection(output2DArray()), fig);
					JOptionPane.showConfirmDialog(e.getComponent(), "Waypoints copied to clipboard!", "Points Copier",
							JOptionPane.DEFAULT_OPTION);
				} else if(e.getExtendedKeyCode() == 78 && !currentPath.isEmpty()) {//CTRL + N
					addToPathsAndClear();
					fig.repaint();
				} else if(e.getExtendedKeyCode() == 68) {//CTRL + D
					drawMode = !drawMode;
					JOptionPane.showMessageDialog(e.getComponent(), String.format("Draw mode set to %b", drawMode), "Draw Mode Status",
							JOptionPane.INFORMATION_MESSAGE);
				} else if(e.getExtendedKeyCode() == 79) {//CTRL + O
					JFileChooser jfc = new JFileChooser();
					jfc.setFileFilter(new FileNameExtensionFilter("Text or Java Files", "txt", "TXT", "java"));
					if(jfc.showOpenDialog(e.getComponent()) == JFileChooser.APPROVE_OPTION) {
						String fAP = jfc.getSelectedFile().getAbsolutePath();//File absolute path
						if(fAP.endsWith(".txt") || fAP.endsWith(".TXT") || fAP.endsWith(".java"))
							try {
								Scanner fileReader = new Scanner(jfc.getSelectedFile());
								addToPathsAndClear();
								while(fileReader.hasNextLine()) {
									String s = fileReader.nextLine();
									if(s.contains("Object[][]") && s.contains("= new Object[][]{"))
										if(currentPath.isEmpty()) {
											System.out.println("Should only run once");
											continue;
										} else {
											System.out.println("Should run every new 2D array");
											if(!genPath(e)) throw new Exception();
											addToPathsAndClear();
											continue;
										}
									s = s.replaceAll("\\{", " ").replaceAll("},", " ").
											replaceAll("}", " ").replaceAll(";", " ");
									s = s.trim();
									String[] o = s.split(", ");
									if(o[0].equals("true")) {
										currentPath.add(new PathSegment(true));
									} else if(o[0].equals("false")) {
										currentPath.add(new PathSegment(false));
									} else if(!currentPath.isEmpty() && o.length > 1)
										if(currentPath.getLast().isDrawn)
											currentPath.getLast().pathSegPoints.add(new Point(Double.parseDouble(o[0]), Double.parseDouble(o[1])));
										else
											currentPath.getLast().clickPoints.add(new Point(Double.parseDouble(o[0]), Double.parseDouble(o[1]),
													Boolean.parseBoolean(o[2])));
									if(!currentPath.getLast().isDrawn) {
										if(!genPath(e)) throw new Exception();
									} else if(!validatePath(currentPath)) throw new Exception();
								}
								fileReader.close();
								System.out.println("File imported successfully!");
							} catch(FileNotFoundException ex) {
								System.err.println("The file has magically disappeared");
							} catch(Exception ex) {
								System.err.println("Please format the data correctly!");
								ex.printStackTrace();
								currentPath.clear();
							}
						addToPathsAndClear();
						fig.repaint();
					}
				}
			}
		}
	}

	private void showFieldError(InputEvent e) {
		JOptionPane.showMessageDialog(e.getComponent(), String.format("You cannot create a point here as the generated " +
				"path would go through the %s!", invalidElementName), "Point Validator", JOptionPane.ERROR_MESSAGE);
	}

	class MouseListener extends MouseAdapter {
		Image curImg;
		Cursor cursor;

		{
			curImg = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/GUIStuff/cursor.png"));
			cursor = Toolkit.getDefaultToolkit().createCustomCursor(curImg, new java.awt.Point(7, 7), "dank");
		}

		/**
		 * Draw mode essentially allows you to specify a free-flowing path for the robot to follow.
		 * Instead of clicking multiple waypoints, it automatically takes your cursor location as
		 * a waypoint, adds that to the list of waypoints and then updates the GUI.
		 * If you've clicked on a valid point to move, instead of drawing a path it will move that
		 * point instead, to whatever location your cursor is at and then update the path and GUI accordingly
		 **/
		@Override
		public void mouseDragged(MouseEvent e) {
			System.out.println(moveflag[0]);
			int mf1 = (int) moveflag[1], mf2 = (int) moveflag[2];
			if(mf1 > -1) {
				java.awt.Point p = g.getRootPane().getMousePosition();
				if(p != null) {
					double x = constrainTo((p.getX() - 30), rektWidth) / xScale;
					double y = constrainTo(((height - 30) - p.getY()), rektHeight) / yScale;
					if(moveflag[0].equals("current")) {
						Point prevPoint = new Point(currentPath.get(mf1).clickPoints.get(mf2), true);
						currentPath.get(mf1).clickPoints.get(mf2).x = x;
						currentPath.get(mf1).clickPoints.get(mf2).y = y;
						pathGen = new MPGen2D(convertPointArray(currentPath.get(mf1).clickPoints), 5.0, 0.02, 3.867227572441874);
						pathGen.calculate();
						if(pathGen.smoothPath != null && validatePathSegment(convert2DArray(pathGen.smoothPath)))
							currentPath.get(mf1).pathSegPoints = convert2DArray(pathGen.smoothPath);
						else {
							showFieldError(e);
							currentPath.get(mf1).clickPoints.get(mf2).x = prevPoint.x;
							currentPath.get(mf1).clickPoints.get(mf2).y = prevPoint.y;
						}
					} else {
						Point prevPoint = new Point(paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2), true);
						paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).x = x;
						paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).y = y;
						pathGen = new MPGen2D(convertPointArray(paths.get(moveflag[0].toString()).get(mf1).clickPoints), 5.0, 0.02,
								3.867227572441874);
						pathGen.calculate();
						if(pathGen.smoothPath != null && validatePathSegment(convert2DArray(pathGen.smoothPath)))
							paths.get(moveflag[0].toString()).get(mf1).pathSegPoints = convert2DArray(pathGen.smoothPath);
						else {
							showFieldError(e);
							paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).x = prevPoint.x;
							paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).y = prevPoint.y;
						}
					}
					fig.repaint();
				} else
					JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor",
							JOptionPane.ERROR_MESSAGE);
			} else if(drawMode && mf1 > -2)
				updateWaypoints(true, e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			java.awt.Point p = g.getRootPane().getMousePosition();
			if(p != null) {
				p.x = (int) constrainTo((p.getX() - 30), rektWidth);
				p.y = (int) constrainTo(((height - 30) - p.getY()), rektHeight);
				if(!findPoint(currentPath, e, p)) {
					if(paths.isEmpty()) e.getComponent().setCursor(Cursor.getDefaultCursor());
					for(Map.Entry<String, BetterArrayList<PathSegment>> en : paths.entrySet())
						if(findPoint(en.getValue(), e, p)) return;
					e.getComponent().setCursor(Cursor.getDefaultCursor());
				}
			}
		}

		private boolean findPoint(BetterArrayList<PathSegment> path, MouseEvent e, java.awt.Point p) {
			java.awt.Point temp = new java.awt.Point();
			for(PathSegment ps : path)
				if(!ps.isDrawn)
					for(Point po : ps.clickPoints)
						for(int k = -3; k < 4; k++) {
							temp.x = (int) (po.x * xScale) + k;
							for(int l = -3; l < 4; l++) {
								temp.y = (int) (po.y * yScale) + l;
								if(p.equals(temp))
									if(po.movable) {
										e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
										return true;
									} else {
										e.getComponent().setCursor(cursor);
										return true;
									}
							}
						}
			for(Polygon2D poly : fg.invalidAreas)
				if(poly.contains(p.x / xScale, p.y / yScale)) {
					e.getComponent().setCursor(cursor);
					return true;
				}
			return false;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			updateWaypoints(false, e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			java.awt.Point p = g.getRootPane().getMousePosition();
			if(p != null) {
				p.x = (int) constrainTo((p.getX() - 30), rektWidth);
				p.y = (int) constrainTo(((height - 30) - p.getY()), rektHeight);
				moveflag = new Object[]{-1, -1, -1};
				//Check the current path to see if the clicked point is a part of it and if it isn't then check all the other paths
				//Only check the other paths if you can't find it in the current one to save time and resources instead of needlessly
				//searching through extra paths
				if(!findClickedPoint("current", currentPath, e, p)) {
					System.out.println(moveflag[0]);
					for(Map.Entry<String, BetterArrayList<PathSegment>> en : paths.entrySet())
						if(findClickedPoint(en.getKey(), en.getValue(), e, p))
							break;
				}
			} else
				JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor",
						JOptionPane.ERROR_MESSAGE);
		}

		private boolean findClickedPoint(String name, BetterArrayList<PathSegment> path, MouseEvent e, java.awt.Point p) {
			int i, j = i = 0;
			java.awt.Point temp = new java.awt.Point();
			for(PathSegment ps : path) {
				if(ps.isDrawn) {
					i++;
					j = 0;
					continue;
				} else
					for(Point po : ps.clickPoints) {
						for(int k = -4; k < 5; k++) {
							temp.x = (int) (po.x * xScale) + k;
							for(int l = -4; l < 5; l++) {
								temp.y = (int) (po.y * yScale) + l;
								if(p.equals(temp))
									if(po.movable) {
										moveflag = new Object[]{name, i, j};
										return true;
									} else {
										JOptionPane.showMessageDialog(e.getComponent(), "You cannot move this point!", "Point Mover",
												JOptionPane.ERROR_MESSAGE);
										moveflag = new Object[]{-2, -2, -2};
										return true;
									}
							}
						}
						j++;
					}
				i++;
				j = 0;
			}
			return false;
		}

		private boolean smoothThings(double x, double y, InputEvent e) {
			if(currentPath.size() > 1 && shouldSmooth) {
				shouldSmooth = false;
				System.out.println("its ya boi");
				if(currentPath.get2ndLast().clickPoints.size() > 1)
					currentPath.get2ndLast().clickPoints.getLast().movable = false;
				if(currentPath.getLast().isDrawn) {
					System.out.println("its actually ya boi");
					currentPath.get2ndLast().clickPoints.add(new Point(x, y, false));
					pathGen = new MPGen2D(convertPointArray(currentPath.get2ndLast().clickPoints), 5.0, 0.02, 3.867227572441874);
					pathGen.calculate();
					if(pathGen.smoothPath != null && validatePathSegment(convert2DArray(pathGen.smoothPath)))
						currentPath.get2ndLast().pathSegPoints = convert2DArray(pathGen.smoothPath);
					else {
						showFieldError(e);
						currentPath.get2ndLast().clickPoints.removeLast();
						return false;
					}
					return true;
				}
			}
			return false;
		}

		private void simClick(double x, double y, InputEvent e) {
			if(currentPath.isEmpty())
				currentPath.add(new PathSegment(false));
			currentPath.getLast().clickPoints.add(new Point(x, y));
			genPath(e);
		}

		private void simDrawClick(double x, double y, InputEvent e) {
			if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
				currentPath.removeLast();
			currentPath.add(new PathSegment(false));
			if(!smoothThings(x, y, e))
				if(currentPath.size() > 1 && !currentPath.get2ndLast().pathSegPoints.isEmpty()) {
					int numToRemove = (int) constrainTo(currentPath.get2ndLast().pathSegPoints.size(), 10);
					if(!currentPath.get2ndLast().isDrawn)
						if(currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty() &&
								!currentPath.getLast().isDrawn) {
							System.out.println("normal click sorta");
							currentPath.removeLast();
							simClick(x, y, e);
						} else {
							System.out.println("blah");
							currentPath.get2ndLast().clickPoints.add(new Point(x, y));
							pathGen = new MPGen2D(convertPointArray(currentPath.get2ndLast().clickPoints), 5.0, 0.02,
									3.867227572441874);
							pathGen.calculate();
							if(pathGen.smoothPath != null && validatePathSegment(convert2DArray(pathGen.smoothPath)))
								currentPath.getLast().pathSegPoints = convert2DArray(pathGen.smoothPath);
							else {
								showFieldError(e);
								currentPath.get2ndLast().clickPoints.removeLast();
							}
						}
					else {
						if(numToRemove > 1 && currentPath.get2ndLast().isDrawn)
							currentPath.getLast().clickPoints.add(new Point(currentPath.get2ndLast().pathSegPoints.get(currentPath.get2ndLast().
									pathSegPoints.size() - numToRemove), false));
						currentPath.getLast().clickPoints.add(new Point(currentPath.get2ndLast().pathSegPoints.getLast(), false));
						System.out.println("WEWWWWWWWWWWWWWWW");
						if(currentPath.get2ndLast().isDrawn)
							for(int i = 0; i < numToRemove - 1; i++)
								currentPath.get2ndLast().pathSegPoints.removeLast();
						if(numToRemove == 1 && currentPath.get2ndLast().pathSegPoints.size() == 1 && currentPath.get2ndLast().clickPoints.size() == 1
								&& !currentPath.get2ndLast().isDrawn)
							currentPath.remove(currentPath.size() - 2);
						currentPath.getLast().clickPoints.add(new Point(x, y));
						genPath(e);
					}
				} else if(currentPath.size() == 1 && currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty() &&
						!currentPath.getLast().isDrawn)
					simClick(x, y, e);
		}

		private void updateWaypoints(boolean drawMode, MouseEvent e) {
			//Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
			java.awt.Point p = g.getRootPane().getMousePosition();
			if(p != null) {
				double x = constrainTo((p.getX() - 30), rektWidth) / xScale;
				double y = constrainTo(((height - 30) - p.getY()), rektHeight) / yScale;
				if(validatePoint(x, y, currentPath.isEmpty() ? null : currentPath.getLast().pathSegPoints.isEmpty() ? null : currentPath.getLast().
						isDrawn ? currentPath.getLast().pathSegPoints.getLast() : currentPath.getLast().clickPoints.getLast())) {
					if(drawMode)
						if(previousDraw) {//Handles staying at draw mode
							System.out.println("spicy");
							if(currentPath.isEmpty())
								currentPath.add(new PathSegment(true));
							if(!currentPath.getLast().isDrawn && !currentPath.isEmpty()) {
								System.out.println("why u no work?");
								currentPath.add(new PathSegment(true));
								currentPath.getLast().pathSegPoints.add(currentPath.get2ndLast().pathSegPoints.getLast());
								currentPath.get2ndLast().clickPoints.getLast().movable = false;
							}
							smoothThings(x, y, e);
							currentPath.getLast().pathSegPoints.add(new Point(x, y));
							fig.repaint();
							pm = PrevMode.DRAW;
						} else {//Handles going from click to draw mode
							System.out.println("plsssssssssssssssssssssssssssssssssssssssssssss");
							if(pm == PrevMode.DRAWCLICK) {
								if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
									currentPath.removeLast();
								simClick(x, y, e);
								currentPath.add(new PathSegment(true));
								if(currentPath.size() > 1)
									shouldSmooth = true;
							} else if(pm == PrevMode.UNDO || pm == PrevMode.REDO)
								simDrawClick(x, y, e);
							else {
								if(currentPath.isEmpty())
									currentPath.add(new PathSegment(false));
								currentPath.getLast().clickPoints.add(new Point(x, y, false));
								System.out.println(currentPath.getLast().clickPoints.size() + " fefw");
								if(currentPath.getLast().clickPoints.size() > 1) {
									currentPath.getLast().clickPoints.get2ndLast().movable = false;
									genPath(e);
								} else if(currentPath.getLast().clickPoints.size() == 1)
									genPath(e);
								currentPath.add(new PathSegment(true));
								if(currentPath.size() > 1)
									shouldSmooth = true;
							}
							fig.repaint();
							pm = PrevMode.CLICKDRAW;
						}
					else if(previousDraw) {//Handles going from draw to click mode
						System.out.println("SAJEGNJKGNJKASN");
						if(pm == PrevMode.CLICKDRAW) {
							if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
								currentPath.removeLast();
							simClick(x, y, e);
						} else
							simDrawClick(x, y, e);
						fig.repaint();
						pm = PrevMode.DRAWCLICK;
					} else {//Handles staying at click mode
						System.out.println("dank");
						if(pm == PrevMode.UNDO || pm == PrevMode.REDO)
							simDrawClick(x, y, e);
						else
							simClick(x, y, e);
						fig.repaint();
						pm = PrevMode.CLICK;
					}
					//Every time a new point is added, clear the redo buffer, give the first-time boolean its virginity back and update the
					//previous draw state
					redoBuffer.clear();
					firstUndoRedo = true;
					System.out.println(previousDraw + " " + drawMode + " shdSmth " + shouldSmooth);
					previousDraw = drawMode;
				} else
					showFieldError(e);
			} else
				JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor",
						JOptionPane.ERROR_MESSAGE);
		}
	}
}