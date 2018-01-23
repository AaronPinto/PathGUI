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
 * spline interpolation. Either way, the filename is MPGen2D. Draw mode is toggleable off/on (Ctrl + D) and by default it is on,
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
	private double xScale, yScale, rectWidth, rectHeight, robotTrkWidth = 24.0889 / 12.0, ppiX, ppiY;

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
	//moveFlag[0] is the path name, moveFlag[1] is the PathSegment index, moveFlag[2] is the Point index
	private Object[] moveFlag = new Object[]{-1, -1, -1};

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
		//Create the menuBar, all the menuItems and specify the mnemonics and ActionLiseners for each item.
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

		menuItem = new JMenuItem("Clear All (Ctrl + A)", KeyEvent.VK_A);
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

	/**
	 * A function that takes a BetterArrayList as an argument, and performs a deep copy of tall the values
	 * A deep copy means that an object is copied along with all the object to which it refers. In this case, all
	 * the values of the objects that the BetterArrayList refer to are copied into a new double array, which is then returned.
	 *
	 * @param p the BetterArrayList to convert to a 2d double array
	 * @return a 2d double array containing all the points from the BetterArrayList
	 */
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
				if(currentPath.getLast().isDrawn) {
					redoBuffer.peekLast().pathSegPoints.add(currentPath.getLast().pathSegPoints.removeLast());
					redoBuffer.peekLast().leftPSPoints.add(currentPath.getLast().leftPSPoints.removeLast());
					redoBuffer.peekLast().rightPSPoints.add(currentPath.getLast().rightPSPoints.removeLast());
				} else {
					redoBuffer.peekLast().clickPoints.add(currentPath.getLast().clickPoints.removeLast());
					if(!genPath(currentPath.getLast(), false))
						redo();
				}
			}
			removeEmptyPaths();
//			outputRedoBuffer();
			fig.repaint();
			pm = PrevMode.UNDO;
		} else
			JOptionPane.showConfirmDialog(g, "No More Undos!", "Undo Status", JOptionPane.DEFAULT_OPTION);
	}

	/**
	 * Function that creates a new JFrame with  JLabel to display the instructions on how to use this program.
	 */
	private void instructions() {
		JFrame j = new JFrame("Instructions");
		j.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		j.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				g.setEnabled(true);
				j.dispose();
			}
		});
		j.setSize(710, 400);
		j.setLayout(new BorderLayout(10, 10));
		j.setLocationRelativeTo(null);
		j.setResizable(false);
		String text = "The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse" +
				" button is clicked, it adds that point to a series of points, called a PathSegment, and generates a path accordingly. This is " +
				"called click mode. Draw mode is toggleable off/on (Ctrl + D) and by default it is on, which means you can use it in tandem with " +
				"click mode, and it automatically switches depending on whether or not you hold the left button on your mouse down or not. Draw " +
				"mode essentially allows you to draw a path yourself without having to generate one each time. It allows for more freedom of design " +
				"regarding the path that the robot follows. The program also has various keyboard shortcuts to make it more intuitive and easy to " +
				"use. Ctrl + C to copy points on the path in proper Java syntax for a 2D array (Ctrl + V to paste outside of the program), Ctrl + N " +
				"to keep the old paths and start a new one if, for example, you want the robot to follow multiple paths in auto, Ctrl + Z removes " +
				"each point on the current path and Ctrl + Y adds that point back plus you can hold down the keys and it will rapidly get rid of " +
				"each point, Ctrl + O to open and parse a file containing waypoints in proper Java 2D array format, and then display the paths" +
				" from that file, and Ctrl + S to save your points to a file, again in proper Java syntax for a 2D array. You can also shift-click " +
				"a path to start editing it again in case you realized you made a mistake or you want to change a curve. There may be some other " +
				"keyboard shortcuts not documented in these instructions/description but just check the menu and see what they do. The program " +
				"also has mnemonics that you can use if you don't know how to use keyboard shortcuts or something.";
		JLabel l = new JLabel("<html><div style='text-align: center;'>" + text + "</div></html>", SwingConstants.CENTER);
		l.setFont(new Font("Arial", Font.PLAIN, 16));
		l.setSize(710, 400);
		j.add(l);
		j.setVisible(true);
		g.setEnabled(false);
	}

	private void save() {
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
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void redo() {
		if(!redoBuffer.isEmpty()) {
			if(redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().clickPoints.isEmpty())
				redoBuffer.removeLast();
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

//			outputRedoBuffer();

			if(currentPath.getLast().isDrawn) {
				currentPath.getLast().pathSegPoints.add(redoBuffer.peekLast().pathSegPoints.removeLast());
				currentPath.getLast().leftPSPoints.add(redoBuffer.peekLast().leftPSPoints.removeLast());
				currentPath.getLast().rightPSPoints.add(redoBuffer.peekLast().rightPSPoints.removeLast());
			} else {
				currentPath.getLast().clickPoints.add(redoBuffer.peekLast().clickPoints.removeLast());
				genPath(currentPath.getLast(), true);
			}

			if(redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().clickPoints.isEmpty())
				redoBuffer.removeLast();

//			outputRedoBuffer();
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

	private void clear() {
		if(!currentPath.isEmpty() || !paths.isEmpty()) {
			int response = JOptionPane.showConfirmDialog(g, "Are you sure you want to clear everything?", "Window Clearer",
					JOptionPane.YES_NO_CANCEL_OPTION);
			if(response == JOptionPane.YES_OPTION) {
				currentPath.clear();
				paths.clear();
				redoBuffer.clear();
				previousDraw = firstUndoRedo = shift = false;
				fig.repaint();
			}
		}
	}

	/**
	 * This function imports a path into the program and displays it onto the field. It will
	 */
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
					JOptionPane.showMessageDialog(g, "File imported successfully!", "File Importer", JOptionPane.INFORMATION_MESSAGE);
				} catch(FileNotFoundException ex) {
					System.err.println("The file has magically disappeared");
				} catch(Exception ex) {
					System.err.println("Please format the data correctly!");
					JOptionPane.showMessageDialog(g, String.format("You cannot import this path as it would go through the %s!", invalidElementName),
							"File Importer", JOptionPane.ERROR_MESSAGE);
					currentPath.clear();
				}
			addToPathsAndClear();
			fig.repaint();
		}
	}

	/**
	 * Adds a new path segment to the redoBuffer in order to preserve the entire path as it is, instead of just adding all
	 * the points to one path segment.
	 */
	private void addPathSegment() {
		if(!currentPath.isEmpty())
			redoBuffer.add(new PathSegment(currentPath.getLast().isDrawn));
	}

	/**
	 * This function removes any empty paths that may be created whenever, most likely when mode switching and then not completing
	 * the mode switch, for example activate the clickDraw mode (creates an empty drawn path) and then undoing (calls this function).
	 */
	private void removeEmptyPaths() {
		if(currentPath.getLast().pathSegPoints.isEmpty()) {
			currentPath.removeLast();
			addPathSegment();
		} else if(currentPath.getLast().clickPoints.isEmpty() && !currentPath.getLast().isDrawn) {
			currentPath.removeLast();
			addPathSegment();
		}
	}

//	private void outputRedoBuffer() {
//		for(PathSegment ps : redoBuffer) {
//			if(ps.isDrawn)
//				ps.pathSegPoints.forEach(System.out::println);
//			else
//				ps.clickPoints.forEach(System.out::println);
//			System.out.println("new path seg");
//		}
//	}

	/**
	 * This function adds the current path to the LinkedHashMap paths and then clears the currentPath, if the currentPath isn't empty.
	 */
	private void addToPathsAndClear() {
		if(!currentPath.isEmpty()) {
			paths.put(String.format("path%d", paths.size() + 1), currentPath);
			currentPath = new BetterArrayList<>();
		}
	}

	/**
	 * The central method which paints the panel and shows the figure and is called every time an event occurs to update the GUI.
	 * In this method, we cast the Graphics object copy to a Graphics2D object, which is better suited for our purposes of drawing
	 * 2D shapes and objects. Then, we store the width and height of this JPanel in double variables so that they may be used later
	 * without having to call the functions multiple times. Next, we draw the axes, grid lines, field elements, field border, and then
	 * all of the path data. We also figure out the ratio for feet to pixels in both the x and y directions, which is used for drawing
	 * a path in the correct spot on the field, and also the ratio for pixels per inch in the x and y directions, which is used to provide
	 * a buffer area for paths so that you don't have to make it perfectly perpendicular to the border.
	 */
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		//Gotta enable antialiasing for that quality rendering of shapes :P
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
		drawXTickRange(g2, xaxis, yaxis.getY1(), yaxis.getY2(), xMax);

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
	}

	/**
	 * This is the function that plots everything in a path, so clickPoints (if applicable), pathSegPoints, leftPoints, and rightPoints.
	 * It draws a circle of radius 2 pixels around each non-clicked point and a circle of radius 3 around each clicked point. This is why
	 * a path may appear to be crossing a field element, when in reality it is not because the edge of the robot ends at the value of the point.
	 * This function also draws a line between each point to show that they are all connected in the same path.
	 *
	 * @param g2   the 2D graphics object used to draw everything
	 * @param path the path to draw/plot
	 */
	private void plotPath(Graphics2D g2, BetterArrayList<PathSegment> path) {
		System.out.println("number of path segments: " + path.size() + " " + pm);
		//For each path segment print out the corresponding points.
		path.forEach(aPath -> {
			System.out.println(aPath.pathSegPoints.size() + " " + aPath.clickPoints.size() + " " + aPath.isDrawn);
			//Draw clicked points
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

			//Draw all the pathSegPoints, leftPoints and rightPoints
			if(aPath.pathSegPoints.size() > 1)
				IntStream.range(0, aPath.pathSegPoints.size() - 1).forEach(j -> {
					double x1 = 30 + xScale * aPath.pathSegPoints.get(j).x, y1 = height - 30 - yScale * aPath.pathSegPoints.get(j).y,
							x2 = 30 + xScale * aPath.pathSegPoints.get(j + 1).x, y2 = height - 30 - yScale * aPath.pathSegPoints.get(j + 1).y;
					g2.setPaint(Color.green);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
					g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
					g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));

					if(!aPath.leftPSPoints.isEmpty()) {
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

	/**
	 * This function plots all the paths in the current user session. It stores the last color of the Graphics object and then resets
	 * the Graphics object to that color after.
	 *
	 * @param g2 the 2D graphics object used to draw everything
	 */
	private void plot(Graphics2D g2) {
		Color tempC = g2.getColor();

		//plot the current path then loop through paths and plot each
		plotPath(g2, currentPath);
		paths.forEach((key, value) -> plotPath(g2, value));
		g2.setColor(tempC);
	}

	/**
	 * Just so you don't get confused, this function draws the numbers and ticks along the y-axis and the horizontal light gray lines.
	 * <a href="http://www.purplemath.com/modules/distform.htm">Distance Formula</a>
	 *
	 * @param g2    The Graphics2D object for this window
	 * @param yaxis The line that represents the y-axis
	 * @param Max   The width of the field in feet
	 */
	private void drawYTickRange(Graphics2D g2, Line2D yaxis, double Max) {
		double upperYtic = Math.ceil(Max);

		//The starting and ending x and y values for the y-axis
		double x0 = yaxis.getX1();
		double y0 = yaxis.getY1();
		double xf = yaxis.getX2();
		double yf = yaxis.getY2();

		//calculate step-size between ticks and length of Y axis using distance formula
		//Total distance of the axis / number of ticks = distance per tick
		double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperYtic;

		double upper = upperYtic;
		//Iterates through each number from 0 to the max length of the y-axis in feet and draws the horizontal grid
		//lines for each iteration except the last one
		for(int i = 0; i <= upperYtic; i++) {
			double newY = y0;
			//calculate width of number for proper drawing
			String number = new DecimalFormat("#.#").format(upper);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(number);

			//Draws a tick line and the corresponding number at that value in black
			g2.draw(new Line2D.Double(x0, newY, x0 - 10, newY));
			g2.drawString(number, (float) x0 - 15 - width, (float) newY + 5);

			//add grid lines to chart
			if(i != upperYtic) {
				Stroke tempS = g2.getStroke();
				Color tempC = g2.getColor();

				//Sets the color and stroke to light gray dashes and draws them all the way across the JPanel
				g2.setColor(Color.lightGray);
				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
				g2.draw(new Line2D.Double(30, newY, getWidth(), newY));

				g2.setColor(tempC);
				g2.setStroke(tempS);
			}
			//Increment the number to draw and the position of the tick
			upper -= 1;
			y0 = newY + distance;
		}
	}

	/**
	 * Just so you don't get confused, this function draws the numbers and ticks along the x-axis and the vertical light gray lines.
	 * <a href="http://www.purplemath.com/modules/distform.htm">Distance Formula</a>
	 *
	 * @param g2    The Graphics2D object for this window
	 * @param xaxis The line that represents the x-axis
	 * @param Max   The width of the field in feet
	 */
	private void drawXTickRange(Graphics2D g2, Line2D xaxis, double yTickYMax, double yTickYMin, double Max) {
		double upperXtic = Math.ceil(Max);

		//The starting and ending x and y values for the x-axis
		double x0 = xaxis.getX1();
		double y0 = xaxis.getY1();
		double xf = xaxis.getX2();
		double yf = xaxis.getY2();

		//calculate step-size between ticks and length of Y axis using distance formula
		//Total distance of the axis / number of ticks = distance per tick
		double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperXtic;

		double lower = 0;
		//Iterates through each number from 0 to the max length of the x-axis in feet and draws the horizontal grid
		//lines for each iteration except the last one
		for(int i = 0; i <= upperXtic; i++) {
			double newX = x0;
			//calculate width of number for proper drawing
			String number = new DecimalFormat("#.#").format(lower);
			FontMetrics fm = getFontMetrics(getFont());
			int width = fm.stringWidth(number);

			//Draws a tick line and the corresponding number at that value in black
			g2.draw(new Line2D.Double(newX, yf, newX, yf + 10));
			g2.drawString(number, (float) (newX - (width / 2.0)), (float) yf + 25);

			//add grid lines to chart
			if(i != 0) {
				Stroke tempS = g2.getStroke();
				Color tempC = g2.getColor();

				//Sets the color and stroke to light gray dashes and draws them all the way down to the x-axis
				g2.setColor(Color.lightGray);
				g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
				g2.draw(new Line2D.Double(newX, yTickYMax, newX, height - (height - yTickYMin)));

				g2.setColor(tempC);
				g2.setStroke(tempS);
			}
			//Increment the number to draw and the position of the tick
			lower += 1;
			x0 = newX + distance;
		}
	}

	@Override
	public void lostOwnership(Clipboard clip, Transferable transferable) {
		//We must keep the object we placed on the system clipboard
		//until this method is called.
	}

	/**
	 * This function formats and appends all the values in all the paths into proper 2D array syntax and then returns that String.
	 *
	 * @return a String that contains all the values for all the paths
	 */
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

	/**
	 * This function does the actual formatting and appending for all the values in a path to a StringBuilder
	 *
	 * @param output the StringBuilder to append the values of the path to.
	 * @param value  the path to parse, format and get the values from.
	 */
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

	/**
	 * This function generates the left and right paths from a center path and returns those paths. All math is in radians
	 * because Java's math library uses radians for it's trigonometric calculations. First, it checks if the original path
	 * has more than one point because you can't calculate a heading from one point only. Then, it calculates the heading between
	 * 2 consecutive points in the original path and stores those values in an array. Next, it sets the last point to the same heading
	 * as the previous point so that the ending of the path is more accurate. Finally, it calculates the new point values (in feet) and returns
	 * those paths in the BetterArrayList of BetterArrayLists.
	 *
	 * @param points        the original center path to generate the left and right values from (point values are in feet)
	 * @param robotTrkWidth the robot track width (used as the actual width of the robot for simplicity)
	 * @return A BetterArrayList of the left and right paths (Stored in BetterArrayLists also) for the robot
	 */
	private BetterArrayList<BetterArrayList<Point>> leftRight(BetterArrayList<Point> points, double robotTrkWidth) {
		BetterArrayList<BetterArrayList<Point>> temp = new BetterArrayList<>();
		temp.add(new BetterArrayList<>(points.size()));//Left
		temp.add(new BetterArrayList<>(points.size()));//Right
		double[] heading = new double[points.size()];

		System.out.println(points.size());

		if(points.size() > 1) {
			//Heading calculation
			IntStream.range(0, points.size() - 1).forEach(i -> {
				double x1 = points.get(i).x, x2 = points.get(i + 1).x, y1 = points.get(i).y, y2 = points.get(i + 1).y;
				heading[i] = Math.atan2(y2 - y1, x2 - x1);
			});

			//Makes the last heading value = to the 2nd last for a smoother path.
			if(heading.length > 1)
				heading[heading.length - 1] = heading[heading.length - 2];

			//Point value calculation, temp.get(0) and temp.get(1) are the left and right paths respectively
			//Pi / 2 rads = 90 degrees
			//leftX = trackWidth / 2 * cos(calculatedAngleAtThatIndex + Pi / 2) + centerPathXValueAtThatIndex
			//leftY = trackWidth / 2 * sin(calculatedAngleAtThatIndex + Pi / 2) + centerPathYValueAtThatIndex
			//rightX = trackWidth / 2 * cos(calculatedAngleAtThatIndex - Pi / 2) + centerPathXValueAtThatIndex
			//rightY = trackWidth / 2 * sin(calculatedAngleAtThatIndex - Pi / 2) + centerPathYValueAtThatIndex
			IntStream.range(0, heading.length).forEach(i -> {
				temp.get(0).add(i, new PathGUITool.Point(robotTrkWidth / 2 * Math.cos(heading[i] + Math.PI / 2) + points.get(i).x,
						robotTrkWidth / 2 * Math.sin(heading[i] + Math.PI / 2) + points.get(i).y));
				temp.get(1).add(i, new PathGUITool.Point(robotTrkWidth / 2 * Math.cos(heading[i] - Math.PI / 2) + points.get(i).x,
						robotTrkWidth / 2 * Math.sin(heading[i] - Math.PI / 2) + points.get(i).y));
			});
		} else if(points.size() == 1) temp.get(0).add(0, new Point(points.get(0).x, points.get(0).y));

		return temp;
	}

	/**
	 * This function generates pathSegPoints from the clicked points and is only called with a clicked PathSegment. It generates
	 * the center path first and then if that wasn't null, it will generate the left and right paths, check if those are valid,
	 * assign those to the given pathSegment's left and right path BetterArrayLists respectively, and return true, else it will
	 * remove the last point if remove is true, show the fieldError and return false.
	 *
	 * @param ps     the pathSegment to get the clickPoints from and generate a paths from them
	 * @param remove true if it should remove the last added point if that point made the path invalid
	 * @return true if the generated path was valid else false
	 */
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

	/**
	 * This function checks the circular area around each point in a pathSegment to see if its valid.
	 * If it is valid, that means that it has enough room to generate the left and right paths for that pathSegment.
	 *
	 * @param ps the <code>PathSegment</code> to check
	 * @return true if its valid, false otherwise
	 */
	private boolean checkCircleArea(BetterArrayList<Point> ps) {
		return ps.stream().allMatch(this::checkCircleArea);
	}

	/**
	 * Instead of inputting a <code>Point</code>, you can input an x and y value instead of creating a new <code>Point</code>
	 *
	 * @param x the x value of a position to check
	 * @param y the y value of a position to check
	 * @return true if that position is valid, false otherwise
	 */
	private boolean checkCircleArea(double x, double y) {
		return checkCircleArea(new Point(x, y));
	}

	/**
	 * This function actually checks to see if the area of the circle with a diameter of the robotTrackWidth intersects
	 * the area of an invalid field element. If any part of the circle area overlaps with any part of the invalid element area
	 * this point is invalid so the function returns false, otherwise it returns true. The field border is a special case because
	 * the area of the field border is the entire field, so if we were to check whether any point's area overlapped with the entire
	 * field, it would always be invalid, therefore we skip the field border.
	 *
	 * @param po the point to check
	 * @return true if the points valid, otherwise false
	 */
	private boolean checkCircleArea(Point po) {
		Area a = new Area(new Ellipse2D.Double(po.x - robotTrkWidth / 2.0, po.y - robotTrkWidth / 2.0, robotTrkWidth, robotTrkWidth));
		for(Polygon2D p : fg.invalidAreas) {
			Area element = new Area(p);
			element.intersect(a);
			if(!p.name.equals("field border") && !element.isEmpty()) {
				invalidElementName = p.name;
				return false;
			}
		}
		return true;
	}

	/**
	 * This function checks an entire path to see if any point in it is in an invalid position, whether that may be intersecting an invalid field
	 * element, in an invalid field element, or outside the field border.
	 *
	 * @param path the path to check
	 * @return true if the entire path is valid, otherwise false
	 */
	private boolean validatePath(BetterArrayList<PathSegment> path) {
		return path.stream().allMatch(pathSegment -> validatePathSegment(pathSegment.pathSegPoints));
	}

	/**
	 * This function checks a PathSegment to see if any point in it is in an invalid position, whether that may be intersecting an invalid field
	 * element, in an invalid field element, or outside the field border.
	 *
	 * @param b the path segement to check
	 * @return true if the path segement is valid, otherwise false
	 */
	private boolean validatePathSegment(BetterArrayList<Point> b) {
		if(b.size() == 1) return validatePoint(b.get(0).x, b.get(0).y, null);
		return IntStream.range(0, b.size() - 1).allMatch(i -> validatePoint(b.get(i).x, b.get(i).y, b.get(i + 1)));
	}

	/**
	 * This function checks a Point to see if its in an invalid position, whether that may be in an invalid field element, or outside the field border.
	 * It also takes another point, which would usually be the next consecutive one in the PathSegment, and creates a line to it to check if that line
	 * intersects an invalid field element
	 *
	 * @param x    the x values of the point
	 * @param y    thhe y value of the point
	 * @param next the next point to create a line to
	 * @return false if the next point is invalid, false if the next point is null and the current point is invalid, true otherwise
	 */
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

	/**
	 * Function that displays a dialog letting the user know that their last inputted point was invalid.
	 */
	private void showFieldError() {
		System.out.println("error " + invalidElementName + " " + pm);
		JOptionPane.showMessageDialog(g, String.format("You cannot create a point here as the robot following the generated path " +
				"would go through the %s!", invalidElementName), "Point Validator", JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * This function gets the cursor position, constrains it to one inch inside the field and converts it to feet. The once in buffer
	 * is used to make generating a path that starts at the field border, easier to create.
	 *
	 * @param p the point where the cursor is on the JFrame
	 * @return an array with the x and y value of that point in feet
	 */
	private double[] getCursorFeet(java.awt.Point p) {
		if(p != null) {
			double[] temp = new double[2];
			temp[0] = constrainTo(p.getX() - 30, ppiX, rectWidth - ppiX) / xScale;
			temp[1] = constrainTo(((height - 30 + g.getJMenuBar().getHeight()) - p.getY()), ppiY, rectHeight - ppiY) / yScale;
			return temp;
		}
		return null;
	}

	/**
	 * This function does the same thing as the one above except it keeps the position in pixels.
	 *
	 * @param p the point where the cursor is on the JFrame
	 * @return an array with the x and y value of that point in pixels
	 */
	private int[] getCursorPixels(java.awt.Point p) {
		if(p != null) {
			int[] temp = new int[2];
			temp[0] = (int) constrainTo(p.getX() - 30, ppiX, rectWidth - ppiX);
			temp[1] = (int) constrainTo(((height - 30 + g.getJMenuBar().getHeight()) - p.getY()), ppiY, rectHeight - ppiY);
			return temp;
		}
		return null;
	}

	/**
	 * An enum with the different modes in this program. Based on the previous mode, a different set on instructions for PathSegement or Point
	 * joining is executed.
	 */
	private enum PrevMode {
		DRAW, CLICKDRAW, DRAWCLICK, CLICK, UNDO, REDO
	}

	/**
	 * A Point is simply a position in 2D space on the field, with double values because
	 * they are also used for storing the value of each point in a path.
	 */
	static class Point {
		double x, y;
		boolean movable;

		/**
		 * Constructor that takes the co-ordinate of the point. It initially sets the point to be movable by default.
		 *
		 * @param x the x value of the co-ordinate
		 * @param y the y value of the co-ordinate
		 */
		Point(double x, double y) {
			this.x = x;
			this.y = y;
			this.movable = true;
		}

		/**
		 * Constructor that takes the co-ordinate and whether it is movable or not of this point.
		 *
		 * @param x    the x value of the co-ordinate
		 * @param y    the y value of the co-ordinate
		 * @param move the point's initial movable state
		 */
		Point(double x, double y, boolean move) {
			this.x = x;
			this.y = y;
			this.movable = move;
		}

		/**
		 * Constructor that takes a previous point and whether or not this point is movable.
		 *
		 * @param p    the Point to copy the co-ordinate from
		 * @param move the point's initial movable state
		 */
		Point(Point p, boolean move) {
			this.x = p.x;
			this.y = p.y;
			this.movable = move;
		}

		/**
		 * This function overrides the function in the superclass and is called whenever we want to compare if another
		 * Object is equal to this one. If that Object is an instance of the Point class, It compares the x/y co-ordinate
		 * value of both points and if they are the same, then this function returns true. Otherwise it just returns the
		 * result of the superclass's equals function. Usually, this should only be used when comparing Points anyway.
		 *
		 * @param p the Object to compare this Point to
		 * @return true if the object is the same as this one, false otherwise
		 */
		@Override
		public boolean equals(Object p) {
			if(p instanceof Point) {
				Point temp = (Point) p;
				return this.x == temp.x && this.y == temp.y;
			}
			return super.equals(p);
		}

		/**
		 * This function overrides the function in the superclass and is called whenever we want to output the values
		 * of this Point.
		 *
		 * @return a formatted String with this Point's values
		 */
		@Override
		public String toString() {
			return x + ", " + y + ", " + movable;
		}

		/**
		 * This function is used whenever we want to output the x/y co-ordinate value of this pint only.
		 *
		 * @return a formatted String with this Point's co-ordinate value.
		 */
		String values() {
			return x + ", " + y;
		}
	}

	/**
	 * A path segment can either be drawn or clicked and we need to keep track of that for the different keyboard shortcuts
	 * It also needs to store the points of a path segment and if the path segment is clicked, then it needs to store those waypoints as well
	 */
	class PathSegment {
		boolean isDrawn;
		BetterArrayList<Point> pathSegPoints, clickPoints, leftPSPoints, rightPSPoints;

		/**
		 * Constructor for a PathSegment
		 *
		 * @param isDrawn true if the path segment is drawn, otherwise false
		 */
		PathSegment(boolean isDrawn) {
			this.isDrawn = isDrawn;
			this.pathSegPoints = new BetterArrayList<>();
			this.clickPoints = new BetterArrayList<>(0);
			this.leftPSPoints = new BetterArrayList<>();
			this.rightPSPoints = new BetterArrayList<>();
		}
	}

	/**
	 * Handles the actions that are generated by whenever you click on the menuItems or use the mnemonics
	 * Based on the action command (the name of the menuItem that triggered the event) it runs the respective function.
	 */
	class MenuListener implements ActionListener {
		/**
		 * This function overrides the function in the interface it implements and is called whenever a JMenuItem that
		 * has this class as it's listener is selected, either through a click or trough the mnemonic. We figure out
		 * JMenuItem was selected based off the name of the action command (name of JMenuItem) and then call the
		 * corresponding function.
		 *
		 * @param e the ActionEvent that is created whenever a JMenuItem is selected
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getActionCommand().equals("Undo (Ctrl + Z)")) undo();//CTRL + Z
			else if(e.getActionCommand().equals("Redo (Ctrl + Y)")) redo();//CTRL + Y
			else if(e.getActionCommand().equals("Copy Points (Ctrl + C)")) copy();//CTRL + C
			else if(e.getActionCommand().equals("New Path (Ctrl + N)") && !currentPath.isEmpty()) newPath();//CTRL + N
			else if(e.getActionCommand().equals("Toggle Draw Mode (Ctrl + D)")) toggleDrawMode();//CTRL + D
			else if(e.getActionCommand().equals("Open (Ctrl + O)")) open();//CTRL + O
			else if(e.getActionCommand().equals("View Instructions (Ctrl + I)")) instructions();//CTRL + I
			else if(e.getActionCommand().equals("Save (Ctrl + S)")) save();//CTRL + S
			else if(e.getActionCommand().equals("Clear All (Ctrl + A)")) clear();//CTRL + A
		}
	}

	/**
	 * Same thing as the MenuListener but instead it listens for key presses and specific keyboard shortcuts
	 * to call the respective function.
	 */
	class KeyboardListener extends KeyAdapter {
		/**
		 * This function overrides the function in the superclass and is called whenever a key is pressed while the
		 * JFrame has focus. We figure out what keys were pressed, and it it was a specified keyboard shortcut, we
		 * call the corresponding function.
		 *
		 * @param e the KeyEvent that is created whenever a key is pressed
		 */
		@Override
		public void keyPressed(KeyEvent e) {
			System.out.println(e.getExtendedKeyCode());
			if(e.isControlDown())
				if(e.getExtendedKeyCode() == 90)//Ctrl + Z
					undo();
				else if(e.getExtendedKeyCode() == 89)//CTRL + Y
					redo();
				else if(e.getExtendedKeyCode() == 67)//CTRL + C
					copy();
				else if(e.getExtendedKeyCode() == 78 && !currentPath.isEmpty())//CTRL + N
					newPath();
				else if(e.getExtendedKeyCode() == 68)//CTRL + D
					toggleDrawMode();
				else if(e.getExtendedKeyCode() == 79)//CTRL + O
					open();
				else if(e.getExtendedKeyCode() == 73)//CTRL + I
					instructions();
				else if(e.getExtendedKeyCode() == 83)//CTRL + S
					save();
				else if(e.getExtendedKeyCode() == 65)//CTRL + A
					clear();

			//Shift is set to true for as long as a shift key is held down.
			//Used for the shift-clicking path functionality
			if(e.getExtendedKeyCode() == 16) shift = true;
		}

		/**
		 * This function is called whenever a key is released and the JFrame has focus.
		 *
		 * @param e the KeyEvent generated whenever a key is released
		 */
		@Override
		public void keyReleased(KeyEvent e) {
			//Shift is set to false when the shift key that was held down is released.
			//Used for the shift-clicking path functionality.
			if(e.getExtendedKeyCode() == 16) shift = false;
		}
	}

	/**
	 * Handles the closing of the main JFrame. All it does is it gives you the option to save your points when you try
	 * to close the window, in case you forgot to save when you were using the program.
	 */
	class WindowListener extends WindowAdapter {
		/**
		 * This function overrides the function in the superclass and is called whenever the X button is clicked on the JFrame.
		 * If there is at least one Point in the current user session, it prompts the user if they want to save their point(s)
		 * /path(s). If the user says no, the program exits, if the user cancels the program continues, and if the user says yes
		 * then the save() function is called. If the current user session has no visible points on the field, the program exits.
		 *
		 * @param e the WindowEvent (unused) which is generated, in this case, when the X button is clicked.
		 */
		@Override
		public void windowClosing(WindowEvent e) {
			if(!paths.isEmpty() || (!currentPath.isEmpty() && !currentPath.get(0).pathSegPoints.isEmpty())) {
				int response = JOptionPane.showConfirmDialog(g, "Do you want to save your points?", "Point Saver",
						JOptionPane.YES_NO_CANCEL_OPTION);
				if(response == JOptionPane.YES_OPTION)
					save();
				System.exit(0);
			} else System.exit(0);
		}
	}

	/**
	 * Handles the motion and position of the mouse and runs different functions accordingly.
	 */
	class MouseListener extends MouseAdapter {
		//A custom cursor for letting the user know that their cursor is over an invalid position.
		Cursor cursor;

		{
			//Initialize the cursor. Checks if the included image is in the correct file location, and if it is it creates
			//the cursor at the center of the actual image excluding the background.
			cursor = Toolkit.getDefaultToolkit().createCustomCursor(Toolkit.getDefaultToolkit().getImage(getClass().
					getResource("/GUIStuff/cursor.png")), new java.awt.Point(7, 7), "invalid");
		}

		/**
		 * This function overrides the function in the superclass and is called whenever Java detects the mouse has been dragged. A drag
		 * consists of a mouse button being held down and the mouse being moved simultaneously. Draw mode essentially allows you to specify
		 * a free-flowing path for the robot to follow. Instead of clicking multiple waypoints, it automatically takes your cursor location as
		 * a waypoint, adds that to the list of waypoints and then updates the GUI. If you've clicked on a valid point to move, instead of
		 * drawing a path it will move that point instead, to whatever valid location your cursor is at and then update the path and GUI
		 * accordingly.
		 *
		 * If the user is not trying to shift-click a path, then if the user's cursor is within the JFrame, then if the user is trying to move
		 * a clicked point (moveFlag[1] > -1), move that point based on the cursor position, otherwise call updateWaypoints(true). If the user
		 * drags their cursor out of the JFrame prompt the user to move their cursor back into the window.
		 *
		 * @param e the (unused) MouseEvent generated when the mouse is dragged (button is held down and the mouse is moved)
		 **/
		@Override
		public void mouseDragged(MouseEvent e) {
			if(!shift) {
				System.out.println(moveFlag[0]);
				int mf1 = (int) moveFlag[1], mf2 = (int) moveFlag[2];
				if(mf1 > -1) {
					double[] point;
					if((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
						if(moveFlag[0].equals("current")) {
							Point prevPoint = new Point(currentPath.get(mf1).clickPoints.get(mf2), true);
							currentPath.get(mf1).clickPoints.get(mf2).x = point[0];
							currentPath.get(mf1).clickPoints.get(mf2).y = point[1];
							if(!genPath(currentPath.get(mf1), false)) {
								currentPath.get(mf1).clickPoints.get(mf2).x = prevPoint.x;
								currentPath.get(mf1).clickPoints.get(mf2).y = prevPoint.y;
							}
						} else {
							Point prevPoint = new Point(paths.get(moveFlag[0].toString()).get(mf1).clickPoints.get(mf2), true);
							paths.get(moveFlag[0].toString()).get(mf1).clickPoints.get(mf2).x = point[0];
							paths.get(moveFlag[0].toString()).get(mf1).clickPoints.get(mf2).y = point[1];
							if(!genPath(paths.get(moveFlag[0].toString()).get(mf1), false)) {
								paths.get(moveFlag[0].toString()).get(mf1).clickPoints.get(mf2).x = prevPoint.x;
								paths.get(moveFlag[0].toString()).get(mf1).clickPoints.get(mf2).y = prevPoint.y;
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

		/**
		 * This functions overrides the function in the superclass and is called whenever the mouse is moved. First, we check if the
		 * mouse cursor is in the JFrame/window and if it is we constrain it to the field border and keep the values in pixels, and we
		 * take another snapshot of the mouse position and then check if that is within the window. If it is, we shift it to the actual
		 * position of the cursor relative to the field and keep it as pixels. This is the actual, unconstrained position of the cursor.
		 * We then feed those cursor values into the findPoint() function and if it doesn't find a point, within a 7x7 range of the
		 * cursor with the cursor in the center, in the current path and the cursor is not in an invalid field element, if paths is
		 * empty it sets the cursor to the default cursor. Otherwise it searches through paths for the exact same thing as it does with
		 * currentPath. If it still can't find a point, then it sets the cursor to the default arrow cursor. If the function finds a
		 * movable point, it sets the cursor to a hand cursor. If the function finds a point that cannot be moved or the cursor position
		 * is invalid, it changes the cursor to the custom invalid cursor.
		 *
		 * @param e the (unused) MouseEvent that is generated whenever the mouse is moved.
		 */
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

		/**
		 * This function does the actual searching for a point through the specified path. The aim of this function is to modify the cursor
		 * based upon what its close to (in terms of a clicked point) or in (in terms of an invalid field area). First, it searches through
		 * all the clicked points in non-drawn path segments in a path, and if the cursor is within a 7x7 area, with the clicked point in the
		 * middle, of a movable clicked point, it changes the cursor to a hand and returns true, otherwise if the point is not movable, it
		 * changes the cursor to the custom invalid one and returns true. If the cursor is not near a clicked point in that path, it then
		 * checks if the cursor is inside an invalid field area, other than the field border. If it is, then it sets the cursor the the
		 * invalid one and returns true. Regarding the field border, it checks if the path is empty and if it is then it continues to the next
		 * field element, otherwise it checks if the actual position of the cursor is outside the field border. If it is then it sets the
		 * cursor to the invalid one and returns true. The reason why it checks if the path is empty is to make it easier for the user to start
		 * a path along the field border. Because the first Point is automatically constrained to inside the field, you don't need to display
		 * an unwieldy cursor that does not tell you, precisely, where the point will be created if you click.
		 *
		 * @param path the path to search through
		 * @param x the x value of the constrained cursor position
		 * @param y the y value of the constrained cursor position
		 * @param actPos the actual unconstrained cursor position
		 * @return false if the cursor is not near a clicked point in that path or in any invalid area, true otherwise
		 */
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

		/**
		 * This function overrides the function in the superclass and is called whenever a mouse button is clicked.
		 * First we check if shift is held down or not. If it is we multi-thread the search for the point that was clicked.
		 * For each path in paths, a new Thread is created to search through that path for the closest point in a 7x7 area around
		 * the point located at the center. If a point is found, an AtomicBoolean is set to true by the first thread that finds a match.
		 * All subsequent thread's cannot change the value of this boolean so they all finish execution and die. This ensures that the
		 * path is only switched once to the correct path and that's it, even if there are paths that overlap. When a path is swapped,
		 * the currentPath gets stored in place of the path in paths and that path is swapped to the currentPath, so that you can modify
		 * it as you normally would any currentPath and the redoBuffer is cleared to prevent issues form arising.
		 * <p>
		 * If shift is not held, then we just call updateWaypoints(false).
		 *
		 * @param e the MouseEvent (unused) generated when the mouse is clicked in the component
		 */
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
												JOptionPane.showMessageDialog(g, "Successfully switched paths!",
														"Path Switcher", JOptionPane.INFORMATION_MESSAGE);
												System.out.println("Path switch successful");
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

		/**
		 * This function overrides the function in the superclass and is called whenever a mouse button is pressed down. It attempts
		 * to find a clicked point for moving to modify path segments. First it checks if the cursor is inside the window. If it isn't
		 * then it prompts the user to move it back in. If it is, then it resets the moveFlag array to the default states and searches
		 * through the current path to find a clicked point. If it can't find it in the current path it looks through all the stored
		 * paths in paths (the LinkedHashMap).
		 *
		 * @param e the (unused) MouseEvent that is generated whenever a mouse button is pressed.
		 */
		@Override
		public void mousePressed(MouseEvent e) {
			int[] point;
			if((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
				java.awt.Point p = new java.awt.Point(point[0], point[1]);
				moveFlag = new Object[]{-1, -1, -1};
				//Check the current path to see if the clicked point is a part of it and if it isn't then check all the other paths
				//Only check the other paths if you can't find it in the current one to save time and resources instead of needlessly
				//searching through extra paths
				if(!findClickedPoint("current", currentPath, p)) {
					System.out.println(moveFlag[0]);
					for(Map.Entry<String, BetterArrayList<PathSegment>> en : paths.entrySet())
						if(findClickedPoint(en.getKey(), en.getValue(), p))
							break;
				}
			} else
				JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
						JOptionPane.ERROR_MESSAGE);
		}

		/**
		 * This function actually does the searching for the clicked point. It is similar to the previous "find" function but instead of
		 * changing the cursor it sets the moveFlag array accordingly, and if its an unmovable point, it prompts the user to let them
		 * know that that point is unmovable.
		 *
		 * @param name the name of the path, "current" for the current path, otherwise its the key in the LinkedHashMap
		 * @param path the actual path to search through
		 * @param p the cursor point
		 * @return false if it can't find a corresponding point, true otherwise
		 */
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
										moveFlag = new Object[]{name, i, j};
										return true;
									} else {
										JOptionPane.showMessageDialog(g, "You cannot move this point!", "Point Mover",
												JOptionPane.ERROR_MESSAGE);
										moveFlag = new Object[]{-2, -2, -2};
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

		/**
		 * This function simulates a clicked point. It checks if the current path is empty and adds a non-drawn PathSegment if it is.
		 * Then it adds the new point to the end of the current PathSegment. Finally it generates a new path accordingly.
		 *
		 * @param x the x position of the point in feet
		 * @param y the y position of the point in feet
		 */
		private void simClick(double x, double y) {
			if(currentPath.isEmpty())
				currentPath.add(new PathSegment(false));
			currentPath.getLast().clickPoints.add(new Point(x, y));
			genPath(currentPath.getLast(), true);
		}

		/**
		 * This function handles a whole bunch of edge cases and the smoothing when going from clicked to drawn or drawn to clicked. It simulates
		 * a "drawClick" which is the click when you go from drawn to clicked mode.
		 *
		 * @param x the x position of the point in feet
		 * @param y the y position of the point in feet
		 */
		private void simDrawClick(double x, double y) {
			if(!currentPath.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty())
				currentPath.removeLast();
			currentPath.add(new PathSegment(false));
			if(currentPath.size() > 1 && !currentPath.get2ndLast().pathSegPoints.isEmpty()) {
				int numToRemove = (int) constrainTo(currentPath.get2ndLast().pathSegPoints.size(), 0.0, 10.0);
				if(!currentPath.get2ndLast().isDrawn)
					if(!currentPath.getLast().isDrawn && currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty()) {
						System.out.println("normal click sorta");
						//Removes the unnecessary empty path segment and adds a clicked point
						//to the previous because we already know the previous one is not drawn
						currentPath.removeLast();
						simClick(x, y);
					} else {
						System.out.println("blah");
						currentPath.get2ndLast().clickPoints.add(new Point(x, y));
						pathGen = new MPGen2D(convertPointArray(currentPath.get2ndLast().clickPoints), 5.0, 0.02,
								robotTrkWidth);
						pathGen.calculate();
						if(pathGen.smoothPath != null) {
							//Generates the left and right paths for the center path.
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
						currentPath.removeLast();//This probably is the problem perhaps xd
					}
				}
			} else if(currentPath.size() == 1 && currentPath.getLast().clickPoints.isEmpty() && currentPath.getLast().pathSegPoints.isEmpty() &&
					!currentPath.getLast().isDrawn)
				simClick(x, y);
		}

		/**
		 * This function handles all the logic for switching between modes, adding Points to compensate for mode switches, adding PathSegments to
		 * compensate for mode switching, and updating the path correctly with the new point, if the new point is valid.
		 *
		 * @param drawMode true if this function was called from the mouseDragged() function, false if it was from MouseClicked()
		 */
		private void updateWaypoints(boolean drawMode) {
			//Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
			double[] point;
			if((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
				Point prev = currentPath.isEmpty() ? null : currentPath.getLast().pathSegPoints.isEmpty() ? null : currentPath.getLast().isDrawn ?
						currentPath.getLast().pathSegPoints.getLast() : currentPath.getLast().clickPoints.getLast();
				if(checkCircleArea(point[0], point[1]) && validatePoint(point[0], point[1], prev)) {
					if(drawMode) {
						if(previousDraw) {//Handles staying at draw mode
							System.out.println("spicy");
							if(currentPath.isEmpty() || !currentPath.getLast().isDrawn)
								currentPath.add(new PathSegment(true));
							if((pm == PrevMode.CLICKDRAW || pm == PrevMode.UNDO || pm == PrevMode.REDO) && currentPath.size() > 1 &&
									!currentPath.get2ndLast().isDrawn && (currentPath.getLast().pathSegPoints.isEmpty() || !currentPath.
									getLast().pathSegPoints.get(0).equals(currentPath.get2ndLast().pathSegPoints.getLast())))
								currentPath.getLast().pathSegPoints.add(currentPath.get2ndLast().pathSegPoints.getLast());
							if(pm == PrevMode.CLICKDRAW && !currentPath.isEmpty() && !currentPath.getLast().isDrawn)
								currentPath.get2ndLast().clickPoints.getLast().movable = false;
							if(!currentPath.isEmpty() && currentPath.getLast().isDrawn) {
								currentPath.getLast().pathSegPoints.add(new Point(point[0], point[1]));
								BetterArrayList<Point> temp = pathGen.smoother(currentPath.getLast().pathSegPoints, 0.3, 0.8, 0.000001);
								BetterArrayList<BetterArrayList<Point>> lr = leftRight(currentPath.getLast().pathSegPoints, robotTrkWidth);
								if(checkCircleArea(temp) && validatePathSegment(lr.get(0)) && validatePathSegment(lr.get(1))) {
									currentPath.getLast().pathSegPoints = temp;
									currentPath.getLast().leftPSPoints = lr.get(0);
									currentPath.getLast().rightPSPoints = lr.get(1);
								} else {
									currentPath.getLast().pathSegPoints.removeLast();
									showFieldError();
								}
							}
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
					} else if(previousDraw) {//Handles going from draw to click mode
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