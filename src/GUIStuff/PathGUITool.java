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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PathGUITool extends JPanel implements ClipboardOwner {
	private static final long serialVersionUID = 3205256608145459434L;
	private static MPGen2D selectedPath;
	private static PathGUITool fig;
	private JFrame g = new JFrame("Path GUI Tool");
	private double upperXtic, upperYtic, height, xScale, yScale,
			yTickYMax = 0, yTickYMin = 0, rektWidth, rektHeight;
	private BetterArrayList<PathSegment> currentPath = new BetterArrayList<>();
	private LinkedHashMap<String, BetterArrayList<PathSegment>> paths = new LinkedHashMap<>();
	private boolean previousDraw = false, draw = true, shouldSmooth = false, firstUndoRedo = false;
	private PrevMode pm;
	private Object[] moveflag = new Object[]{-1, -1, -1};
	private FieldGenerator fg = new FieldGenerator();
	private Deque<PathSegment> redoBuffer = new ArrayDeque<>();//add() adds last, peek() gets first

	/**
	 * Constructor.
	 */
	private PathGUITool() {
		upperXtic = -Double.MAX_VALUE;
		upperYtic = -Double.MAX_VALUE;

		//Set the properties of this JFrame
		g.add(this);
		g.setSize(1280, 720);//Be nice to people who don't have 1080p displays :P
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

	public static void main(String[] args) {
		fig = new PathGUITool();
	}

	/**
	 * The central method which paints the panel and shows the figure and is called every time the GUI updates.
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

		//draw ticks
		double yMax = 27.0, xMax = 54.0;
		drawYTickRange(g2, yaxis, yMax);
		drawXTickRange(g2, xaxis, xMax);

		//plot field
		plotFieldBorder(g2);

		rektWidth = (xaxis.getX2() - xaxis.getX1());
		rektHeight = (yaxis.getY2() - yaxis.getY1());
		xScale = rektWidth / xMax;
		yScale = rektHeight / yMax;

		fg.plotFieldElements(g2, super.getHeight(), xScale, yScale);
		g2.setColor(Color.black);
		Rectangle rekt = new Rectangle((int) yaxis.getX1(), (int) yaxis.getY1(), (int) rektWidth, (int) rektHeight);
		g2.draw(rekt);
		g2.setColor(super.getBackground());
		g2.fillRect(rekt.x + rekt.width + 1, rekt.y, (int) width - rekt.width - 30, rekt.height + 1);//Hides excess grid lines

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

		plotPath(g2, h, currentPath);
		//loop through paths and plot each
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
		for(PathSegment path : value) {
			if(path.isDrawn) {
				output.append("{").append(true).append(", ").append(true).append("},\n");
				for(int j = 0; j < path.pathSegPoints.size(); j++)
					output.append("{").append(path.pathSegPoints.get(j).x).append(", ").append(path.pathSegPoints.get(j).y).append("},\n");
			} else {
				output.append("{").append(false).append(", ").append(false).append("},\n");
				for(int j = 0; j < path.clickPoints.size(); j++)
					output.append("{").append(path.clickPoints.get(j).x).append(", ").append(path.clickPoints.get(j).y).append(", ").
							append(path.clickPoints.get(j).movable).append("},\n");
			}
		}
		output.append("};\n");
	}

	private void genPath() {
		selectedPath = new MPGen2D(convertPointArray(currentPath.getLast().clickPoints), 5.0, 0.02, 3.867227572441874);
		selectedPath.calculate();
		if(selectedPath.smoothPath != null)
			currentPath.getLast().pathSegPoints = convert2DArray(selectedPath.smoothPath);
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
	}

	class WindowListener extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			if(!paths.isEmpty() || (currentPath.size() != 0 && (currentPath.get(0).pathSegPoints.size() > 0))) {
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
				currentPath.remove(currentPath.size() - 1);
				addPathSegment();
			} else if(currentPath.getLast().clickPoints.isEmpty() && !currentPath.getLast().isDrawn) {
				currentPath.remove(currentPath.size() - 1);
				addPathSegment();
			}
		}

		private void outputRedoBuffer() {
			for(PathSegment ps : redoBuffer) {
				System.out.println("new path segment");
				if(ps.isDrawn)
					ps.pathSegPoints.forEach(System.out::println);
				else
					ps.clickPoints.forEach(System.out::println);
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
							if(currentPath.getLast().isDrawn) {
								redoBuffer.peekLast().pathSegPoints.add(currentPath.getLast().pathSegPoints.
										remove(currentPath.getLast().pathSegPoints.size() - 1));
							} else {
								redoBuffer.peekLast().clickPoints.add(currentPath.getLast().clickPoints.
										remove(currentPath.getLast().clickPoints.size() - 1));
								genPath();
								fig.repaint();
							}
						}
						removeEmptyPaths();

						outputRedoBuffer();

						fig.repaint();
						pm = PrevMode.UNDO;
					} else
						JOptionPane.showConfirmDialog(e.getComponent(), "No More Undos!", "Undo Status", JOptionPane.DEFAULT_OPTION);
				}
				if(e.getExtendedKeyCode() == 89) {//CTRL + Y
					if(!redoBuffer.isEmpty()) {
						/*
						  so u want to add a path segment to the current path if the current path's current path segment
						  isn't of the same type as the one in the buffer and ofc if the buffer isn't empty
						 */
						if(!redoBuffer.peekLast().clickPoints.isEmpty() && !redoBuffer.peekLast().isDrawn && (currentPath.isEmpty() || currentPath.getLast().isDrawn))
							currentPath.add(new PathSegment(false));
						else if(!redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().isDrawn && (currentPath.isEmpty() || !currentPath.getLast().isDrawn))
							currentPath.add(new PathSegment(true));

						if(currentPath.getLast().isDrawn)
							currentPath.getLast().pathSegPoints.add(redoBuffer.peekLast().pathSegPoints.remove(redoBuffer.peekLast().pathSegPoints.size() - 1));
						else {
							currentPath.getLast().clickPoints.add(redoBuffer.peekLast().clickPoints.remove(redoBuffer.peekLast().clickPoints.size() - 1));
							genPath();
						}

						if(redoBuffer.peekLast().pathSegPoints.isEmpty() && redoBuffer.peekLast().clickPoints.isEmpty())
							redoBuffer.removeLast();

						outputRedoBuffer();

						fig.repaint();
						pm = PrevMode.REDO;
					} else
						JOptionPane.showConfirmDialog(e.getComponent(), "No More Redos!", "Redo Status", JOptionPane.DEFAULT_OPTION);
				}
				if(e.getExtendedKeyCode() == 67) {//CTRL + C, Keycode for S = 83
					Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
					c.setContents(new StringSelection(output2DArray()), fig);
					JOptionPane.showConfirmDialog(e.getComponent(), "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
				}
				if(e.getExtendedKeyCode() == 78 && !currentPath.isEmpty()) {//CTRL + N
					paths.put(String.format("path%d", paths.size() + 1), currentPath);
					currentPath = new BetterArrayList<>();
					fig.repaint();
				}
				if(e.getExtendedKeyCode() == 68) {//CTRL + D
					draw = !draw;
					JOptionPane.showMessageDialog(e.getComponent(), String.format("Draw mode set to %b", draw), "Draw Mode Status",
							JOptionPane.INFORMATION_MESSAGE);
				}
			}
		}
	}

	class MouseListener extends MouseAdapter {
		Image curImg;
		Cursor cursor;

		{
			curImg = Toolkit.getDefaultToolkit().getImage(new File("").getAbsolutePath() + "/PathGUI/src/GUIStuff/cursor.png");
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
						currentPath.get(mf1).clickPoints.get(mf2).x = x;
						currentPath.get(mf1).clickPoints.get(mf2).y = y;
						selectedPath = new MPGen2D(convertPointArray(currentPath.get(mf1).clickPoints), 5.0, 0.02, 3.867227572441874);
						selectedPath.calculate();
						if(selectedPath.smoothPath != null)
							currentPath.get(mf1).pathSegPoints = convert2DArray(selectedPath.smoothPath);
					} else {
						paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).x = x;
						paths.get(moveflag[0].toString()).get(mf1).clickPoints.get(mf2).y = y;
						selectedPath = new MPGen2D(convertPointArray(paths.get(moveflag[0].toString()).get(mf1).clickPoints), 5.0, 0.02, 3.867227572441874);
						selectedPath.calculate();
						if(selectedPath.smoothPath != null)
							paths.get(moveflag[0].toString()).get(mf1).pathSegPoints = convert2DArray(selectedPath.smoothPath);
					}
					fig.repaint();
				} else
					JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor", JOptionPane.ERROR_MESSAGE);
			} else if(draw && mf1 > -2)
				updateWaypoints(true, e);
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			java.awt.Point p = g.getRootPane().getMousePosition();
			if(p != null) {
				p.x = (int) constrainTo((p.getX() - 30), rektWidth);
				p.y = (int) constrainTo(((height - 30) - p.getY()), rektHeight);
				if(!findPoint(currentPath, e, p))
					paths.forEach((key, value) -> findPoint(value, e, p));
			}
		}

		private boolean findPoint(BetterArrayList<PathSegment> path, MouseEvent e, java.awt.Point p) {
			java.awt.Point temp = new java.awt.Point();
			for(PathSegment ps : path) {
				if(!ps.isDrawn) {
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
				}
			}
			if(e.getComponent().getCursor() != Cursor.getDefaultCursor())
				e.getComponent().setCursor(Cursor.getDefaultCursor());
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
				//Only check the other paths if you can;t find it in the current one to save time and resources instead of needlessly
				//Searching through extra paths
				if(!findClickedPoint("current", currentPath, e, p))
					paths.forEach((key, value) -> findClickedPoint(key, value, e, p));
				System.out.println(moveflag[0]);
			} else
				JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor", JOptionPane.ERROR_MESSAGE);
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

		private boolean smoothThings(double x, double y) {
			if(currentPath.size() > 1 && shouldSmooth) {
				shouldSmooth = false;
				System.out.println("its ya boi");
				if(currentPath.get(currentPath.size() - 2).clickPoints.size() > 1)
					currentPath.get(currentPath.size() - 2).clickPoints.get(currentPath.get(currentPath.size() - 2).clickPoints.size() - 1).movable = false;
				if(currentPath.getLast().isDrawn) {
					System.out.println("its actually ya boi");
					currentPath.get(currentPath.size() - 2).clickPoints.add(new Point(x, y, false));
					selectedPath = new MPGen2D(convertPointArray(currentPath.get(currentPath.size() - 2).clickPoints), 5.0, 0.02, 3.867227572441874);
					selectedPath.calculate();
					if(selectedPath.smoothPath != null)
						currentPath.get(currentPath.size() - 2).pathSegPoints = convert2DArray(selectedPath.smoothPath);
					return true;
				}
			}
			return false;
		}

		private void simClick(double x, double y) {
			if(currentPath.size() == 0)
				currentPath.add(new PathSegment(false));
			currentPath.getLast().clickPoints.add(new Point(x, y));
			genPath();
		}

		private void simDrawClick(double x, double y) {
			if(currentPath.size() > 0 && currentPath.getLast().pathSegPoints.size() == 0)
				currentPath.remove(currentPath.size() - 1);
			currentPath.add(new PathSegment(false));
			if(!smoothThings(x, y))
				if(currentPath.size() > 1 && currentPath.get(currentPath.size() - 2).pathSegPoints.size() > 0) {
					int numToRemove = (int) constrainTo(currentPath.get(currentPath.size() - 2).pathSegPoints.size(), 10);
					if(!currentPath.get(currentPath.size() - 2).isDrawn) {
						if(currentPath.getLast().clickPoints.size() == 0 && currentPath.getLast().pathSegPoints.size() == 0 &&
								!currentPath.getLast().isDrawn) {
							System.out.println("normal click sorta");
							currentPath.remove(currentPath.size() - 1);
							simClick(x, y);
						} else {
							System.out.println("blah");
							currentPath.get(currentPath.size() - 2).clickPoints.add(new Point(x, y));
							selectedPath = new MPGen2D(convertPointArray(currentPath.get(currentPath.size() - 2).clickPoints), 5.0, 0.02, 3.867227572441874);
							selectedPath.calculate();
							if(selectedPath.smoothPath != null)
								currentPath.getLast().pathSegPoints = convert2DArray(selectedPath.smoothPath);
						}
					} else {
						if(numToRemove > 1 && currentPath.get(currentPath.size() - 2).isDrawn)
							currentPath.getLast().clickPoints.add(new Point(currentPath.get(currentPath.size() - 2).
									pathSegPoints.get(currentPath.get(currentPath.size() - 2).pathSegPoints.size() - numToRemove), false));
						currentPath.getLast().clickPoints.add(new Point(currentPath.get(currentPath.size() - 2).
								pathSegPoints.get(currentPath.get(currentPath.size() - 2).pathSegPoints.size() - 1), false));
						System.out.println("WEWWWWWWWWWWWWWWW");
						if(currentPath.get(currentPath.size() - 2).isDrawn)
							for(int i = 0; i < numToRemove - 1; i++)
								currentPath.get(currentPath.size() - 2).pathSegPoints.remove(currentPath.get(currentPath.size() - 2).pathSegPoints.size() - 1);
						if(numToRemove == 1 && currentPath.get(currentPath.size() - 2).pathSegPoints.size() == 1 &&
								currentPath.get(currentPath.size() - 2).clickPoints.size() == 1 && !currentPath.get(currentPath.size() - 2).isDrawn) {
							currentPath.remove(currentPath.size() - 2);
						}
						currentPath.getLast().clickPoints.add(new Point(x, y));
						genPath();
					}
				} else if(currentPath.size() == 1 && currentPath.getLast().clickPoints.size() == 0 &&
						currentPath.getLast().pathSegPoints.size() == 0 && !currentPath.getLast().isDrawn)
					simClick(x, y);
		}

		private void updateWaypoints(boolean draw, MouseEvent e) {
			//Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
			java.awt.Point p = g.getRootPane().getMousePosition();
			if(p != null) {
				double x = constrainTo((p.getX() - 30), rektWidth) / xScale;
				double y = constrainTo(((height - 30) - p.getY()), rektHeight) / yScale;
				if(draw)
					if(previousDraw) {//Handles staying at draw mode
						System.out.println("spicy");
						if(currentPath.size() == 0)
							currentPath.add(new PathSegment(true));
						if(!currentPath.getLast().isDrawn && currentPath.size() > 1) {
							currentPath.add(new PathSegment(true));
							currentPath.getLast().pathSegPoints.add(currentPath.get(currentPath.size() - 2).
									pathSegPoints.get(currentPath.get(currentPath.size() - 2).pathSegPoints.size() - 1));
						}
						smoothThings(x, y);
						currentPath.getLast().pathSegPoints.add(new Point(x, y));
						fig.repaint();
						pm = PrevMode.DRAW;
					} else {//Handles going from click to draw mode
						System.out.println("plsssssssssssssssssssssssssssssssssssssssssssss");
						if(pm == PrevMode.DRAWCLICK) {
							if(currentPath.size() > 0 && currentPath.getLast().pathSegPoints.size() == 0)
								currentPath.remove(currentPath.size() - 1);
							simClick(x, y);
							currentPath.add(new PathSegment(true));
							if(currentPath.size() > 1)
								shouldSmooth = true;
						} else if(pm == PrevMode.UNDO || pm == PrevMode.REDO) {
							simDrawClick(x, y);
						} else {
							if(currentPath.size() == 0)
								currentPath.add(new PathSegment(false));
							currentPath.getLast().clickPoints.add(new Point(x, y, false));
							System.out.println(currentPath.getLast().clickPoints.size());
							if(currentPath.getLast().clickPoints.size() > 1) {
								currentPath.getLast().clickPoints.get(currentPath.getLast().clickPoints.size() - 2).movable = false;
								genPath();
							}
							currentPath.add(new PathSegment(true));
							if(currentPath.size() > 1)
								shouldSmooth = true;
						}
						fig.repaint();
						pm = PrevMode.CLICKDRAW;
					}
				else {
					if(previousDraw) {//Handles going from draw to click mode
						System.out.println("SAJEGNJKGNJKASN");
						if(pm == PrevMode.CLICKDRAW) {
							if(currentPath.size() > 0 && currentPath.getLast().pathSegPoints.size() == 0)
								currentPath.remove(currentPath.size() - 1);
							simClick(x, y);
						} else
							simDrawClick(x, y);
						fig.repaint();
						pm = PrevMode.DRAWCLICK;
					} else {//Handles staying at click mode
						System.out.println("dank");
						if(pm == PrevMode.UNDO || pm == PrevMode.REDO)
							simDrawClick(x, y);
						else
							simClick(x, y);
						fig.repaint();
						pm = PrevMode.CLICK;
					}
				}
				//Every time a new point is added, clear the redo buffer, give the firstTimer its virginity back and update the previous draw state
				redoBuffer.clear();
				firstUndoRedo = true;
				System.out.println(previousDraw + " " + draw);
				previousDraw = draw;
			} else
				JOptionPane.showMessageDialog(e.getComponent(), "Please move your cursor back into the window!", "Boundary Monitor", JOptionPane.ERROR_MESSAGE);
		}
	}
}