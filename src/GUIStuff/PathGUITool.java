package GUIStuff;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse button is
 * clicked, it adds that point to a series of points, called a PathSegment, and generates a path accordingly. This is called
 * click mode. The path generation will either be based off of the generator that we currently use or I will redo it using cubic
 * spline interpolation. Either way, the filename is MPGen2D. Drag mode is toggleable off/on (Ctrl + D) and by default it is on,
 * which means you can use it in tandem with click mode, and it automatically switches depending on whether or not you hold the
 * left button on your mouse down or not. Draw mode essentially allows you to draw a path yourself without having to generate one
 * each time. It allows for more freedom of design regarding the path that the robot follows. The program also has various
 * keyboard shortcuts to make it more intuitive and easy to use. Ctrl + C to copy points on the path in proper Java syntax
 * for a 2D array, Ctrl + N to keep the old paths and start a new one if, for example, you want the robot to follow
 * multiple paths in auto, Ctrl + Z removes each point on the current path and Ctrl + Y adds that point back plus you can
 * hold down the keys and it will rapidly get rid of each point, and Ctrl + O to open and parse a file containing waypoints
 * in proper Java 2D array format, and then display the paths from that file.
 * <p>
 * For more information on the field, see: <a href="https://www.youtube.com/watch?v=HZbdwYiCY74">FRC 2018 Game Reveal</a> and
 * <a href="https://firstfrc.blob.core.windows.net/frc2018/Drawings/LayoutandMarkingDiagram.pdf">FRC 2018 Field Drawings</a>
 * <p>
 * For version history see: <a href="https://github.com/AaronPinto/PathGUI">PathGUI GitHub</a>
 * <p>
 * References:
 * This class refers to <a href="https://github.com/KHEngineering/SmoothPathPlanner/blob/master/src/usfirst/frc/team2168/robot/FalconLinePlot.java">
 * FalconLinePlot</a> and MPGen2D refers to
 * <a href="https://github.com/KHEngineering/SmoothPathPlanner/blob/master/src/usfirst/frc/team2168/robot/FalconPathPlanner.java">FalconPathPlanner</a>
 * I got Polygon2D.java from online, I can't find the exact class, though I deleted a bunch of code and added my own functions in the one I have.
 * I found <a href="https://docs.oracle.com/javase/tutorial/uiswing/examples/components/FileChooserDemoProject/src/components/FileChooserDemo.java">
 * FileChooserDemo</a> online as well to start me off with the Save and Open File Dialogs.
 *
 * @author Aaron Pinto
 * ICS4UR
 * Mr. Le
 * January 22nd, 2018
 * @see BetterArrayList
 * @see FieldGenerator
 * @see Polygon2D
 * @see MPGen2D
 * @see Point
 * @see PathSegment
 */
public class PathGUITool extends JPanel implements ClipboardOwner {
	//The path generator object that also stores the points of the generator path
	private static MPGen2D pathGen;

	//An object of this class, used for the Ctrl + C code
	private static PathGUITool fig;

	//The JFrame for this GUI. It actually displays the window
	private JFrame g = new JFrame("Path GUI Tool");

	//doubles for storing important values, xScale and yScale are the values for
	//pixels per foot for each axis, respectively, yTickYMax and Min are the max and min values of the y-axis in pixels,
	//rectWidth and Height are the width and height of the field border in pixels, robotTrkWidth is the track width of the
	//robot in feet, and ppiX and Y are the pixels per inch for each axis respectively.
	private double xScale, yScale, yTickYMax = 0, yTickYMin = 0, rectWidth, rectHeight, robotTrkWidth = 28.0 / 12.0, ppiX, ppiY;

	//Height is an integer which stores the height of this panel in pixels.
	private int height;

	//A BetterArrayList of PathSegments which stores the values of the current path.
	private BetterArrayList<PathSegment> currentPath = new BetterArrayList<>();

	//LinkedHashMap to store the name and path for all previous paths
	private LinkedHashMap<String, BetterArrayList<PathSegment>> paths = new LinkedHashMap<>();

	//booleans for handling different conditions, previousDraw stores the most recent draw mode state, drawMode stores the
	//current draw mode state, and firstUndoRedo is set to false when Undo is pressed for the first time
	private boolean previousDraw = false, drawMode = true, firstUndoRedo = false, shift = false;

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
		JMenuBar menuBar = new JMenuBar();
		MenuListener ml = new MenuListener();
		JMenu menu = new JMenu("File");
		menu.setMnemonic(KeyEvent.VK_F);
		menuBar.add(menu);

		//I cannot use menu item accelerators for the shortcuts because it makes it extremely laggy and unusable, so I specify the
		//keyboard shortcuts in the name of the menu item.
		JMenuItem menuItem = new JMenuItem("New Path (Ctrl + N)", KeyEvent.VK_N);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		menuItem = new JMenuItem("Open (Ctrl + O)", KeyEvent.VK_O);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		menuItem = new JMenuItem("Save (Ctrl + S)", KeyEvent.VK_S);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		//Build second menu in the menu bar.
		menu = new JMenu("Edit");
		menu.setMnemonic(KeyEvent.VK_E);
		menuBar.add(menu);

		menuItem = new JMenuItem("Undo (Ctrl + Z)", KeyEvent.VK_U);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		menuItem = new JMenuItem("Redo (Ctrl + Y)", KeyEvent.VK_R);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		menuItem = new JMenuItem("Toggle Draw Mode (Ctrl + D)", KeyEvent.VK_D);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		menuItem = new JMenuItem("Copy Points (Ctrl + C)", KeyEvent.VK_C);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		//Build third menu in the menu bar.
		menu = new JMenu("Instructions");
		menu.setMnemonic(KeyEvent.VK_I);
		menuBar.add(menu);

		menuItem = new JMenuItem("View Instructions (Ctrl + I)", KeyEvent.VK_I);
		menuItem.addActionListener(ml);
		menu.add(menuItem);

		//Set the properties of this JFrame
		g.add(this);
		g.setJMenuBar(menuBar);
		g.setSize(1280, 753);//Be nice to people who don't have 1080p displays and set the size to 720p excluding the menu bar at the top :P
		g.setMinimumSize(new Dimension(1280, 753));
		g.setLocationRelativeTo(null);
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
	 * A simple function that constrains a value from the specified min bound to the specified max bound
	 *
	 * @param value        The value to constrain
	 * @param minConstrain The minimum bound (in the same units as the value)
	 * @param maxConstrain The maximum bound (in the same units as the value)
	 * @return the constrained value
	 */
	private static double constrainTo(double value, double minConstrain, double maxConstrain) {
		return Math.max(minConstrain, Math.min(maxConstrain, value));
	}

	/**
	 * Main function
	 */
	public static void main(String[] args) {
		fig = new PathGUITool();
	}

	/**
	 * A function that iterates throughout the entire 2D array given, and maps/adds each value to a BetterArrayList
	 *
	 * @param p the 2D array to converts
	 * @return the {@code BetterArrayList} containing all the data, if the given array is not null
	 */
	public static BetterArrayList<Point> convert2DArray(double[][] p) {
		if(p == null) return new BetterArrayList<>(0);
		return Arrays.stream(p).map(aP -> new Point(aP[0], aP[1])).collect(Collectors.toCollection(BetterArrayList::new));
	}

	public static double[][] convertPointArray(BetterArrayList<Point> p) {
		double[][] temp = new double[p.size()][2];
		IntStream.range(0, p.size()).forEach(i -> {
			temp[i][0] = p.get(i).x;
			temp[i][1] = p.get(i).y;
		});
		return temp;
	}

	private void undo() {
		if(!currentPath.isEmpty()) {
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
					genPath(currentPath.getLast(), true);
				}
			}
			removeEmptyPaths();
//			outputRedoBuffer();
			fig.repaint();
			pm = PrevMode.UNDO;
		} else
			JOptionPane.showConfirmDialog(g, "No More Undos!", "Undo Status", JOptionPane.DEFAULT_OPTION);
	}

	private void redo() {
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

			if(currentPath.getLast().isDrawn) {
				currentPath.getLast().pathSegPoints.add(redoBuffer.peekLast().pathSegPoints.removeLast());
			} else {
				currentPath.getLast().clickPoints.add(redoBuffer.peekLast().clickPoints.removeLast());
				genPath(currentPath.getLast(), true);
			}

			if(redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().clickPoints.isEmpty())
				redoBuffer.removeLast();

			outputRedoBuffer();
			fig.repaint();
			pm = PrevMode.REDO;
		} else
			JOptionPane.showConfirmDialog(g, "No More Redos!", "Redo Status", JOptionPane.DEFAULT_OPTION);
	}

	private void copy() {
		Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
		c.setContents(new StringSelection(output2DArray()), fig);
		JOptionPane.showConfirmDialog(g, "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
	}

	private void newPath() {
		addToPathsAndClear();
		fig.repaint();
	}

	private void toggleDrawMode() {
		drawMode = !drawMode;
		JOptionPane.showMessageDialog(g, String.format("Draw mode set to %b", drawMode), "Draw Mode Status", JOptionPane.INFORMATION_MESSAGE);
	}

	private void open() {
		JFileChooser jfc = new JFileChooser();
		jfc.setFileFilter(new FileNameExtensionFilter("Text or Java Files", "txt", "TXT", "java"));
		if(jfc.showOpenDialog(g) == JFileChooser.APPROVE_OPTION) {
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
								if(!genPath(currentPath.getLast(), true)) throw new Exception();
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
							if(!genPath(currentPath.getLast(), true)) throw new Exception();
						} else if(!validatePath(currentPath)) throw new Exception();
					}
					fileReader.close();
					System.out.println("File imported successfully!");
				} catch(FileNotFoundException ex) {
					System.err.println("The file has magically disappeared");
				} catch(Exception ex) {
					System.err.println("Please format the data correctly!");
					currentPath.clear();
				}
			addToPathsAndClear();
			fig.repaint();
		}
	}

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

	/**
	 * The central method which paints the panel and shows the figure and is called every time an event occurs to update the GUI.
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		double width = getWidth();
		height = getHeight();

		//Draw X and Y lines axis.
		Line2D.Double yaxis = new Line2D.Double(30, 10, 30, height - 30);
		Line2D.Double xaxis = new Line2D.Double(30, height - 30, width - 12, height - 30);
		g2.draw(yaxis);
		g2.draw(xaxis);

		//draw ticks (the light gray grid lines and the numbers)
		double yMax = 27.0, xMax = 54.0;
		drawYTickRange(g2, yaxis, yMax);
		drawXTickRange(g2, xaxis, xMax);

		//draw the field and everything on it
		rectWidth = (xaxis.getX2() - xaxis.getX1());
		rectHeight = (yaxis.getY2() - yaxis.getY1());
		xScale = rectWidth / xMax;
		yScale = rectHeight / yMax;
		ppiX = 1.0 / 12.0 * xScale;
		ppiY = 1.0 / 12.0 * yScale;
		fg.plotField(g2, height, xScale, yScale);
		g2.setColor(Color.black);
		Rectangle rekt = new Rectangle((int) yaxis.getX1(), (int) yaxis.getY1(), (int) rectWidth, (int) rectHeight);
		g2.draw(rekt);

		//Hides excess grid lines on the rightmost side of the field
		g2.setColor(super.getBackground());
		g2.fillRect(rekt.x + rekt.width + 1, rekt.y, (int) width - rekt.width - 30, rekt.height + 1);

		//plot data
		plot(g2);

		g2.setColor(Color.yellow);
		for(Polygon2D poly : fg.invalidAreas) {
			double[] xPoints = new double[poly.npoints], yPoints = new double[poly.npoints];
			IntStream.range(0, poly.npoints).forEach(i -> {
				xPoints[i] = 30 + xScale * poly.xpoints[i];
				yPoints[i] = height - 30 - yScale * poly.ypoints[i];
			});
			Area a = new Area(new Polygon2D(xPoints, yPoints, xPoints.length, ""));
			g2.draw(a);
		}
	}

	private void plotPath(Graphics2D g2, BetterArrayList<PathSegment> path) {
		System.out.println("number of path segments: " + path.size() + " " + pm);
		path.forEach(aPath -> {
			System.out.println(aPath.pathSegPoints.size() + " " + aPath.clickPoints.size() + " " + aPath.isDrawn);
			if(aPath.clickPoints.size() > 1)
				IntStream.range(0, aPath.clickPoints.size() - 1).forEach(j -> {
					double x1 = 30 + xScale * aPath.clickPoints.get(j).x, y1 = height - 30 - yScale * aPath.clickPoints.get(j).y,
							x2 = 30 + xScale * aPath.clickPoints.get(j + 1).x, y2 = height - 30 - yScale * aPath.clickPoints.get(j + 1).y;
					g2.setPaint(Color.magenta);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
					g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
					g2.fill(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
				});
			else if(aPath.clickPoints.size() == 1) {
				double x1 = 30 + xScale * aPath.clickPoints.get(0).x, y1 = height - 30 - yScale * aPath.clickPoints.get(0).y;
				g2.setPaint(Color.magenta);
				g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
			}

			if(aPath.pathSegPoints.size() > 1)
				IntStream.range(0, aPath.pathSegPoints.size() - 1).forEach(j -> {
					double x1 = 30 + xScale * aPath.pathSegPoints.get(j).x, y1 = height - 30 - yScale * aPath.pathSegPoints.get(j).y,
							x2 = 30 + xScale * aPath.pathSegPoints.get(j + 1).x, y2 = height - 30 - yScale * aPath.pathSegPoints.get(j + 1).y;
					g2.setPaint(Color.green);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
					g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
					g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));

					if(aPath.leftPSPoints != null && !aPath.leftPSPoints.isEmpty()) {
						if(j < aPath.leftPSPoints.size() - 1) {
							double lx1 = 30 + xScale * aPath.leftPSPoints.get(j).x, ly1 = height - 30 - yScale * aPath.leftPSPoints.get(j).y,
									lx2 = 30 + xScale * aPath.leftPSPoints.get(j + 1).x, ly2 = height - 30 - yScale * aPath.leftPSPoints.get(j + 1).y,
									rx1 = 30 + xScale * aPath.rightPSPoints.get(j).x, ry1 = height - 30 - yScale * aPath.rightPSPoints.get(j).y,
									rx2 = 30 + xScale * aPath.rightPSPoints.get(j + 1).x, ry2 = height - 30 - yScale * aPath.rightPSPoints.get(j + 1).y;
							g2.setPaint(Color.gray);
							g2.draw(new Line2D.Double(lx1, ly1, lx2, ly2));
							g2.fill(new Ellipse2D.Double(lx1 - 2, ly1 - 2, 4, 4));
							g2.fill(new Ellipse2D.Double(lx2 - 2, ly2 - 2, 4, 4));
							g2.setPaint(Color.lightGray);
							g2.draw(new Line2D.Double(rx1, ry1, rx2, ry2));
							g2.fill(new Ellipse2D.Double(rx1 - 2, ry1 - 2, 4, 4));
							g2.fill(new Ellipse2D.Double(rx2 - 2, ry2 - 2, 4, 4));
						}
					}
				});
			else if(aPath.pathSegPoints.size() == 1) {
				double x1 = 30 + xScale * aPath.pathSegPoints.get(0).x, y1 = height - 30 - yScale * aPath.pathSegPoints.get(0).y;
				g2.setPaint(Color.green);
				g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
			}
		});
	}

	private void plot(Graphics2D g2) {
		Color tempC = g2.getColor();

		//plot the current path then loop through paths and plot each
		plotPath(g2, currentPath);
		paths.forEach((key, value) -> plotPath(g2, value));
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
		double upperYtic = Math.ceil(Max);

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
		double upperXtic = Math.ceil(Max);

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

			if(i % (double) 1 == 0) g2.drawString(number, (float) (newX - (width / 2.0)), (float) yf + 25);

			//add grid lines to chart
			if(i != 0) {
				Stroke tempS = g2.getStroke();
				Color tempC = g2.getColor();

				g2.setColor(Color.lightGray);
				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));

				g2.draw(new Line2D.Double(newX, yTickYMax, newX, height - (height - yTickYMin)));

				g2.setColor(tempC);
				g2.setStroke(tempS);
			}
			lower += 1;
			x0 = newX + distance;
		}
	}

	@Override
	public void lostOwnership(Clipboard clip, Transferable transferable) {
		//We must keep the object we placed on the system clipboard
		//until this method is called.
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

	private BetterArrayList<BetterArrayList<Point>> leftRight(BetterArrayList<Point> points, double robotTrkWidth) {
		BetterArrayList<BetterArrayList<Point>> temp = new BetterArrayList<>();
		temp.add(new BetterArrayList<>(points.size()));//Left
		temp.add(new BetterArrayList<>(points.size()));//Right
		double[] heading = new double[points.size()];

		System.out.println(points.size());

		if(points.size() > 1) {
			IntStream.range(0, points.size() - 1).forEach(i -> {
				double x1 = points.get(i).x, x2 = points.get(i + 1).x, y1 = points.get(i).y, y2 = points.get(i + 1).y;
				heading[i] = Math.atan2(y2 - y1, x2 - x1);
			});

			//Makes the last heading value = to the 2nd last for a smoother path.
			if(heading.length > 1)
				heading[heading.length - 1] = heading[heading.length - 2];

			//convert to degrees 0 to 360 where 0 degrees is +x-axis, accumulated to align with our gyro
			IntStream.range(0, heading.length).forEach(i -> {
				temp.get(0).add(i, new PathGUITool.Point(robotTrkWidth / 2 * Math.cos(heading[i] + Math.PI / 2) + points.get(i).x,
						robotTrkWidth / 2 * Math.sin(heading[i] + Math.PI / 2) + points.get(i).y));
				temp.get(1).add(i, new PathGUITool.Point(robotTrkWidth / 2 * Math.cos(heading[i] - Math.PI / 2) + points.get(i).x,
						robotTrkWidth / 2 * Math.sin(heading[i] - Math.PI / 2) + points.get(i).y));
			});
		} else if(points.size() == 1) temp.get(0).add(0, new Point(points.get(0).x, points.get(0).y));

		return temp;
	}

	private boolean genPath(PathSegment ps, boolean remove) {
		pathGen = new MPGen2D(convertPointArray(ps.clickPoints), 5.0, 0.02, robotTrkWidth);
		pathGen.calculate();
		if(pathGen.smoothPath != null) {
			BetterArrayList<Point> temp = convert2DArray(pathGen.smoothPath);
			BetterArrayList<BetterArrayList<Point>> lAndR = leftRight(temp, robotTrkWidth);
			if(checkCircleArea(temp) && validatePathSegment(lAndR.get(0)) && validatePathSegment(lAndR.get(1))) {
				ps.pathSegPoints = temp;
				ps.leftPSPoints = lAndR.get(0);
				ps.rightPSPoints = lAndR.get(1);
				return true;
			} else {
				if(remove)
					ps.clickPoints.removeLast();
				showFieldError();
				return false;
			}
		}
		return true;
	}

	private boolean checkCircleArea(BetterArrayList<Point> temp) {
		return temp.stream().allMatch(this::checkCircleArea);
	}

	private boolean checkCircleArea(double x, double y) {
		return checkCircleArea(new Point(x, y));
	}

	private boolean checkCircleArea(Point po) {
		Area a = new Area(new Ellipse2D.Double(po.x - robotTrkWidth / 2.0, po.y - robotTrkWidth / 2.0, robotTrkWidth, robotTrkWidth));
		for(Polygon2D p : fg.invalidAreas) {
			Area element = new Area(p);
			element.intersect(a);
			if(!p.name.equals("field border") && !element.isEmpty()) {
				System.out.println("pls work lol");
				invalidElementName = p.name;
				return false;
			}
		}
		return true;
	}

	private boolean validatePath(BetterArrayList<PathSegment> path) {
		return path.stream().allMatch(pathSegment -> validatePathSegment(pathSegment.pathSegPoints));
	}

	private boolean validatePathSegment(BetterArrayList<Point> b) {
		if(b.size() == 1) return validatePoint(b.get(0).x, b.get(0).y, null);
		return IntStream.range(0, b.size() - 1).allMatch(i -> validatePoint(b.get(i).x, b.get(i).y, b.get(i + 1)));
	}

	private boolean validatePoint(double x, double y, Point next) {
		for(Polygon2D p : fg.invalidAreas) {
			if(next != null) {
				if(p.name.equals("field border") ? p.out(x, y) | p.out(next.x, next.y) : (p.contains(x, y) | p.contains(next.x, next.y)) ||
						p.intersects(x, y, next)) {
					invalidElementName = p.name;
					return false;
				}
			} else if(p.name.equals("field border") ? p.out(x, y) : p.contains(x, y)) {
				invalidElementName = p.name;
				return false;
			}
		}
		return true;
	}

	private void showFieldError() {
		System.out.println("error " + invalidElementName + " " + pm);
		JOptionPane.showMessageDialog(g, String.format("You cannot create a point here as the robot following the generated path " +
				"would go through the %s!", invalidElementName), "Point Validator", JOptionPane.ERROR_MESSAGE);
	}

	private double[] getCursorFeet(java.awt.Point p) {
		if(p != null) {
			double[] temp = new double[2];
			temp[0] = constrainTo(p.getX() - 30, ppiX, rectWidth - ppiX) / xScale;
			temp[1] = constrainTo(((height - 30 + g.getJMenuBar().getHeight()) - p.getY()), ppiY, rectHeight - ppiY) / yScale;
			return temp;
		}
		return null;
	}

	private int[] getCursorPixels(java.awt.Point p) {
		if(p != null) {
			int[] temp = new int[2];
			temp[0] = (int) constrainTo(p.getX() - 30, ppiX, rectWidth - ppiX);
			temp[1] = (int) constrainTo(((height - 30 + g.getJMenuBar().getHeight()) - p.getY()), ppiY, rectHeight - ppiY);
			return temp;
		}
		return null;
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
		BetterArrayList<Point> pathSegPoints, clickPoints, leftPSPoints, rightPSPoints;

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

	class MenuListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("Undo (Ctrl + Z)")) {//CTRL + Z
				undo();
			} else if(e.getActionCommand().equals("Redo (Ctrl + Y)")) {//CTRL + Y
				redo();
			} else if(e.getActionCommand().equals("Copy Points (Ctrl + C)")) {//CTRL + C, Keycode for S = 83
				copy();
			} else if(e.getActionCommand().equals("New Path (Ctrl + N)") && !currentPath.isEmpty()) {//CTRL + N
				newPath();
			} else if(e.getActionCommand().equals("Toggle Draw Mode (Ctrl + D)")) {//CTRL + D
				toggleDrawMode();
			} else if(e.getActionCommand().equals("Open (Ctrl + O)")) {//CTRL + O
				open();
			}
		}
	}

	class WindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			if(!paths.isEmpty() || (!currentPath.isEmpty() && !currentPath.get(0).pathSegPoints.isEmpty())) {
				int response = JOptionPane.showConfirmDialog(g, "Do you want to save your points?", "Point Saver",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if(response == JOptionPane.YES_OPTION)
					try {
						Calendar c = Calendar.getInstance();
						JFileChooser jfc = new JFileChooser();
						jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "TXT"));
						jfc.setSelectedFile(new File(String.format("%tB%te%tY-%tl%tM%tS%tp.txt", c, c, c, c, c, c, c)));
						if(jfc.showSaveDialog(g) == JFileChooser.APPROVE_OPTION) {
							if(!jfc.getSelectedFile().getAbsolutePath().endsWith(".txt") && !jfc.getSelectedFile().getAbsolutePath().endsWith(".TXT"))
								jfc.setSelectedFile(new File(jfc.getSelectedFile().getAbsolutePath() + ".txt"));
							FileWriter fw = new FileWriter(jfc.getSelectedFile().getAbsolutePath());
							fw.write(output2DArray());
							fw.flush();
							fw.close();
							JOptionPane.showConfirmDialog(g, "Points Saved Successfully!", "Points Saver",
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
		@Override
		public void keyPressed(KeyEvent e) {
			if(e.isControlDown())
				if(e.getExtendedKeyCode() == 90)//Ctrl + Z
					undo();
				else if(e.getExtendedKeyCode() == 89) //CTRL + Y
					redo();
				else if(e.getExtendedKeyCode() == 67) //CTRL + C, Keycode for S = 83
					copy();
				else if(e.getExtendedKeyCode() == 78 && !currentPath.isEmpty()) //CTRL + N
					newPath();
				else if(e.getExtendedKeyCode() == 68) //CTRL + D
					toggleDrawMode();
				else if(e.getExtendedKeyCode() == 79) //CTRL + O
					open();
			if(e.getExtendedKeyCode() == 16) shift = true;
		}

		@Override
		public void keyReleased(KeyEvent e) {
			if(e.getExtendedKeyCode() == 16) shift = false;
		}
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
			if(!shift) {
				System.out.println(moveflag[0]);
				int mf1 = (int) moveflag[1], mf2 = (int) moveflag[2];
				if(mf1 > -1) {
					double[] point;
					if((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
						if(moveflag[0].equals("current")) {
							Point prevPoint = new Point(currentPath.get(mf1).clickPoints.get(mf2), true);
							currentPath.get(mf1).clickPoints.get(mf2).x = point[0];
							currentPath.get(mf1).clickPoints.get(mf2).y = point[1];
							if(!genPath(currentPath.get(mf1), false)) {
								currentPath.get(mf1).clickPoints.get(mf2).x = prevPoint.x;
								currentPath.get(mf1).clickPoints.get(mf2).y = prevPoint.y;
							}
						} else {
							Point prevPoint = new Point(paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2), true);
							paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).x = point[0];
							paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).y = point[1];
							if(!genPath(paths.get(moveflag[0].toString()).get(mf1), false)) {
								paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).x = prevPoint.x;
								paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).y = prevPoint.y;
							}
						}
						fig.repaint();
					} else
						JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
								JOptionPane.ERROR_MESSAGE);
				} else if(drawMode && mf1 > -2)
					updateWaypoints(true);
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int[] point;
			if((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
				java.awt.Point p = g.getRootPane().getMousePosition();
				if(p != null) {
					p.x = p.x - 30;
					p.y = (height - 30 + g.getJMenuBar().getHeight()) - p.y;
					if(!findPoint(currentPath, point[0], point[1], p)) {
						if(paths.isEmpty()) g.setCursor(Cursor.getDefaultCursor());
						for(Map.Entry<String, BetterArrayList<PathSegment>> en : paths.entrySet())
							if(findPoint(en.getValue(), point[0], point[1], p)) return;
						g.setCursor(Cursor.getDefaultCursor());
					}
				}
			}
		}

		private boolean findPoint(BetterArrayList<PathSegment> path, int x, int y, java.awt.Point actPos) {
			int tx, ty;
			for(PathSegment ps : path)
				if(!ps.isDrawn)
					for(Point po : ps.clickPoints)
						for(int k = -3; k < 4; k++) {
							tx = (int) (po.x * xScale) + k;
							for(int l = -3; l < 4; l++) {
								ty = (int) (po.y * yScale) + l;
								if(x == tx && y == ty)
									if(po.movable) {
										g.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
										return true;
									} else {
										g.setCursor(cursor);
										return true;
									}
							}
						}
			for(Polygon2D poly : fg.invalidAreas) {
				if(poly.name.equals("field border") ? !path.isEmpty() && poly.out(actPos.x / xScale, actPos.y / yScale) :
						poly.contains(x / xScale, y / yScale)) {
					g.setCursor(cursor);
					return true;
				}
			}
			return false;
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if(shift) {
				AtomicBoolean b = new AtomicBoolean(false);
				int[] point;
				if((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
					java.awt.Point p = new java.awt.Point(point[0], point[1]), temp = new java.awt.Point();
					System.out.println(p.x + " " + p.y);
					paths.forEach((key, value) -> new Thread(() -> {
						for(PathSegment ps : value)
							for(Point po : ps.pathSegPoints)
								for(int k = -3; k < 4; k++) {
									temp.x = (int) (po.x * xScale) + k;
									for(int l = -3; l < 4; l++) {
										temp.y = (int) (po.y * yScale) + l;
										if(p.equals(temp))
											if(b.compareAndSet(false, true)) {
												System.out.println("96-33");
												BetterArrayList<PathSegment> swap = currentPath;
												currentPath = paths.get(key);
												paths.put(key, swap);
												redoBuffer.clear();
												return;
											} else return;
									}
								}
					}).start());
				}
			} else
				updateWaypoints(false);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			System.out.println("IIIIIIIIIIIIIITTTTTTTTTTTTTTTTTTTTTTTTTTTTSSSSSSSSSSSSSSSSSSSSSSSS LITTTTTTTTTTTTTTTTTTTTTTT");
			if(!currentPath.isEmpty() && currentPath.getLast().isDrawn && !currentPath.getLast().pathSegPoints.isEmpty()) {
				currentPath.getLast().pathSegPoints = pathGen.smoother(currentPath.getLast().pathSegPoints, 0.09, 0.9, 0.00001);
				BetterArrayList<BetterArrayList<Point>> lr = leftRight(currentPath.getLast().pathSegPoints, robotTrkWidth);
				currentPath.getLast().leftPSPoints = lr.get(0);
				currentPath.getLast().rightPSPoints = lr.get(1);
			}
			fig.repaint();
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int[] point;
			if((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
				java.awt.Point p = new java.awt.Point(point[0], point[1]);
				moveflag = new Object[]{-1, -1, -1};
				//Check the current path to see if the clicked point is a part of it and if it isn't then check all the other paths
				//Only check the other paths if you can't find it in the current one to save time and resources instead of needlessly
				//searching through extra paths
				if(!findClickedPoint("current", currentPath, p)) {
					System.out.println(moveflag[0]);
					for(Map.Entry<String, BetterArrayList<PathSegment>> en : paths.entrySet())
						if(findClickedPoint(en.getKey(), en.getValue(), p))
							break;
				}
			} else
				JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
						JOptionPane.ERROR_MESSAGE);
		}

		private boolean findClickedPoint(String name, BetterArrayList<PathSegment> path, java.awt.Point p) {
			int i, j = i = 0;
			java.awt.Point temp = new java.awt.Point();
			for(PathSegment ps : path) {
				if(ps.isDrawn) {
					i++;
					j = 0;
					continue;
				} else
					for(Point po : ps.clickPoints) {
						for(int k = -3; k < 4; k++) {
							temp.x = (int) (po.x * xScale) + k;
							for(int l = -3; l < 4; l++) {
								temp.y = (int) (po.y * yScale) + l;
								if(p.equals(temp))
									if(po.movable) {
										moveflag = new Object[]{name, i, j};
										return true;
									} else {
										JOptionPane.showMessageDialog(g, "You cannot move this point!", "Point Mover",
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

//		private boolean smoothThings(double x, double y) {
//			if(currentPath.size() > 1 && shouldSmooth) {
//				shouldSmooth = false;
//				System.out.println("its ya boi");
//				if(currentPath.get2ndLast().clickPoints.size() > 1)
//					currentPath.get2ndLast().clickPoints.getLast().movable = false;
//				if(currentPath.getLast().isDrawn) {
//					System.out.println("its actually ya boi");
//					currentPath.get2ndLast().clickPoints.add(new Point(x, y, false));
//					if(genPath(currentPath.get2ndLast(), true))
//						return true;
//					else
//						shouldSmooth = true;
//					return false;
//				}
//			}
//			return false;
//		}

		private void simClick(double x, double y) {
			if(currentPath.isEmpty())
				currentPath.add(new PathSegment(false));
			currentPath.getLast().clickPoints.add(new Point(x, y));
			genPath(currentPath.getLast(), true);
		}

		private void simDrawClick(double x, double y) {
			if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
				currentPath.removeLast();
			currentPath.add(new PathSegment(false));
			if(currentPath.size() > 1 && !currentPath.get2ndLast().pathSegPoints.isEmpty()) {
				int numToRemove = (int) constrainTo(currentPath.get2ndLast().pathSegPoints.size(), 0.0, 10.0);
				if(!currentPath.get2ndLast().isDrawn)
					if(currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty() &&
							!currentPath.getLast().isDrawn) {
						System.out.println("normal click sorta");
						currentPath.removeLast();
						simClick(x, y);
					} else {
						System.out.println("blah");
						currentPath.get2ndLast().clickPoints.add(new Point(x, y));
						pathGen = new MPGen2D(convertPointArray(currentPath.get2ndLast().clickPoints), 5.0, 0.02,
								robotTrkWidth);
						pathGen.calculate();
						if(pathGen.smoothPath != null) {
							BetterArrayList<Point> temp = convert2DArray(pathGen.smoothPath);
							BetterArrayList<BetterArrayList<Point>> lAndR = leftRight(temp, robotTrkWidth);
							if(checkCircleArea(temp) && validatePathSegment(lAndR.get(0)) && validatePathSegment(lAndR.get(1))) {
								currentPath.getLast().pathSegPoints = temp;
								currentPath.getLast().leftPSPoints = lAndR.get(0);
								currentPath.getLast().rightPSPoints = lAndR.get(1);
							}
						} else {
							showFieldError();
							currentPath.get2ndLast().clickPoints.removeLast();
						}
					}
				else {
					if(numToRemove > 1 && currentPath.get2ndLast().isDrawn)
						currentPath.getLast().clickPoints.add(new Point(currentPath.get2ndLast().pathSegPoints.get(currentPath.get2ndLast().
								pathSegPoints.size() - numToRemove), false));
					currentPath.getLast().clickPoints.add(new Point(currentPath.get2ndLast().pathSegPoints.getLast(), false));
					System.out.println("WEWWWWWWWWWWWWWWW");
					currentPath.getLast().clickPoints.add(new Point(x, y));
					if(genPath(currentPath.getLast(), true)) {
						System.out.println("alright");
						if(currentPath.get2ndLast().isDrawn)
							for(int i = 0; i < numToRemove - 1; i++)
								currentPath.get2ndLast().pathSegPoints.removeLast();
						if(numToRemove == 1 && currentPath.get2ndLast().pathSegPoints.size() == 1 && currentPath.get2ndLast().clickPoints.size() == 1
								&& !currentPath.get2ndLast().isDrawn)
							currentPath.remove(currentPath.size() - 2);
					} else if(!currentPath.getLast().isDrawn) {
						System.out.println("god damn it");
						currentPath.removeLast();
					}
				}
			} else if(currentPath.size() == 1 && currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty() &&
					!currentPath.getLast().isDrawn)
				simClick(x, y);
		}

		private void updateWaypoints(boolean drawMode) {
			//Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
			double[] point;
			if((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
				Point prev = currentPath.isEmpty() ? null : currentPath.getLast().pathSegPoints.isEmpty() ? null : currentPath.getLast().isDrawn ?
						currentPath.getLast().pathSegPoints.getLast() : currentPath.getLast().clickPoints.getLast();
				if(checkCircleArea(point[0], point[1]) && validatePoint(point[0], point[1], prev)) {
					if(drawMode)
						if(previousDraw) {//Handles staying at draw mode
							System.out.println("spicy");
							if(currentPath.isEmpty() || !currentPath.getLast().isDrawn)
								currentPath.add(new PathSegment(true));
							if(pm == PrevMode.CLICKDRAW) {
								currentPath.getLast().pathSegPoints.add(currentPath.get2ndLast().pathSegPoints.getLast());
								currentPath.get2ndLast().clickPoints.getLast().movable = false;
							}
							currentPath.getLast().pathSegPoints.add(new Point(point[0], point[1]));
							pm = PrevMode.DRAW;
						} else {//Handles going from click to draw mode
							System.out.println("plsssssssssssssssssssssssssssssssssssssssssssss");
							if((!currentPath.isEmpty() && currentPath.getLast().isDrawn) || pm == PrevMode.UNDO || pm == PrevMode.REDO) {
								simDrawClick(point[0], point[1]);
							} else if(pm == PrevMode.DRAWCLICK) {
								if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
									currentPath.removeLast();
								simClick(point[0], point[1]);
								currentPath.add(new PathSegment(true));
							} else {
								if(currentPath.isEmpty())
									currentPath.add(new PathSegment(false));
								currentPath.getLast().clickPoints.add(new Point(point[0], point[1], false));
								System.out.println(currentPath.getLast().clickPoints.size() + " fefw");
								if(currentPath.getLast().clickPoints.size() > 1) {
									currentPath.getLast().clickPoints.get2ndLast().movable = false;
									genPath(currentPath.getLast(), true);
								} else if(currentPath.getLast().clickPoints.size() == 1)
									genPath(currentPath.getLast(), true);
								currentPath.add(new PathSegment(true));
							}
							pm = PrevMode.CLICKDRAW;
						}
					else if(previousDraw) {//Handles going from draw to click mode
						System.out.println("SAJEGNJKGNJKASN");
						if(pm == PrevMode.CLICKDRAW && !currentPath.getLast().isDrawn) {
							if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
								currentPath.removeLast();
							simClick(point[0], point[1]);
						} else
							simDrawClick(point[0], point[1]);
						pm = PrevMode.DRAWCLICK;
					} else {//Handles staying at click mode
						System.out.println("dank");
						if(pm == PrevMode.UNDO || pm == PrevMode.REDO)
							simDrawClick(point[0], point[1]);
						else if(!currentPath.isEmpty() && currentPath.getLast().isDrawn)
							simDrawClick(point[0], point[1]);
						else
							simClick(point[0], point[1]);
						pm = PrevMode.CLICK;
					}
					fig.repaint();
					//Every time a new point is added, clear the redo buffer, set the firstTime boolean to true because when undo or redo is
					//called it will be the first time it has been called for this path with a new point, and update the previous draw state
					redoBuffer.clear();
					firstUndoRedo = true;
					System.out.println(previousDraw + " " + drawMode + " " + pm + " " + point[0] + " " + point[1]);
					previousDraw = drawMode;
				} else
					showFieldError();
			} else
				JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
						JOptionPane.ERROR_MESSAGE);
		}
	}
}