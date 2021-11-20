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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse button is clicked, it
 * prompts the user for a desired heading, velocity and acceleration for that point and then adds that point to a series of points and
 * generates a path accordingly. The path generation is based off quintic spline interpolation.
 *
 * @author Aaron Pinto
 * @see BetterArrayList
 * @see FieldGenerator
 * @see Polygon2D
 * @see MPGen2D
 * @see Path
 */
public class PathGUITool extends JPanel implements ClipboardOwner {
    // An object of this class, used for the Ctrl + C code
    private static PathGUITool fig;

    // The JFrame for this GUI. It actually displays the window
    private final JFrame g = new JFrame("Path GUI Tool");
    // LinkedHashMap to store the name and path for all previous paths
    private final LinkedHashMap<String, Path> paths = new LinkedHashMap<>();
    // Field generator object that contains all the necessary shapes for the field drawing
    private final FieldGenerator fg = new FieldGenerator();
    // Path for storing the Ctrl + Z'd points
    private final Path redoBuffer = new Path();

    /**
     * doubles for storing important values, xScale and yScale are the values for pixels per foot for each axis, respectively, yTickYMax and
     * Min are the max and min values of the y-axis in pixels, rectWidth and Height are the width and height of the field border in pixels,
     * robotTrkWidth is the track width of the robot in feet, and ppiX and Y are the pixels per inch for each axis respectively.
     */
    private double xScale, yScale, rectWidth, rectHeight, ppiX, ppiY;
    // Height is an integer which stores the height of this panel in pixels
    private int height;
    // A BetterArrayList of PathSegments which stores the values of the current path
    private Path currentPath = new Path();
    // boolean for storing if shift is pressed
    private boolean shift = false;
    // An object array to store the index values necessary to locate a movable point in paths
    // moveFlag[0] is the path name, moveFlag[1] is the Point index
    private Object[] moveFlag = new Object[]{-1, -1};

    /**
     * Constructor.
     */
    private PathGUITool() {
        // Create the menuBar, all the menuItems and specify the mnemonics and ActionListeners for each item.
        JMenuBar menuBar = new JMenuBar();
        MenuListener ml = new MenuListener();
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(menu);

        // I cannot use menu item accelerators for the shortcuts because it makes it extremely laggy and unusable, so I specify the
        // keyboard shortcuts in the name of the menu item.
        JMenuItem menuItem = new JMenuItem("New Path (Ctrl + N)", KeyEvent.VK_N);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        menuItem = new JMenuItem("Open (Ctrl + O)", KeyEvent.VK_O);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save (Ctrl + S)", KeyEvent.VK_S);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        // Build second menu in the menu bar.
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(menu);

        menuItem = new JMenuItem("Undo (Ctrl + Z)", KeyEvent.VK_U);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        menuItem = new JMenuItem("Redo (Ctrl + Y)", KeyEvent.VK_R);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        menuItem = new JMenuItem("Copy Points (Ctrl + C)", KeyEvent.VK_C);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        menuItem = new JMenuItem("Clear All (Ctrl + A)", KeyEvent.VK_A);
        menuItem.addActionListener(ml);
        menu.add(menuItem);

        // Set the properties of this JFrame
        g.add(this);
        g.setJMenuBar(menuBar);
        g.setSize(1280, 753);
        g.setMinimumSize(new Dimension(1280, 753));
        g.setLocationRelativeTo(null);
        g.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        g.setVisible(true);

        // Add listeners to this JFrame. Listeners, well, listen for their respective events to be triggered and call the corresponding
        // function. Ex: if a mouse button is pressed the MouseListener calls mousePressed(MouseEvent e)
        g.addWindowListener(new WindowListener());
        g.addMouseListener(new MouseListener());
        g.addMouseMotionListener(new MouseListener());
        g.addKeyListener(new KeyboardListener());
    }

    /**
     * Main function
     */
    public static void main(String[] args) {
        fig = new PathGUITool();
    }

    /**
     * A simple function that constrains a value from the specified min bound to the specified max bound
     *
     * @param value        The value to constrain
     * @param minConstrain The minimum bound (in the same units as the value)
     * @param maxConstrain The maximum bound (in the same units as the value)
     *
     * @return the constrained value
     */
    private static double constrainTo(double value, double minConstrain, double maxConstrain) {
        return Math.max(minConstrain, Math.min(maxConstrain, value));
    }

    private static BetterArrayList<Waypoint> convert2DArray(MPGen2D pathGen) {
        BetterArrayList<Waypoint> temp = new BetterArrayList<>();
        ArrayList<ArrayList<Double>> results = pathGen.results;

        for (int i = 0; i < results.get(0).size(); i++) {
            temp.add(new Waypoint(results.get(1).get(i), results.get(2).get(i), results.get(3).get(i), results.get(4).get(i),
                    results.get(5).get(i)));
        }

        return temp;
    }

    /**
     * A function that takes a BetterArrayList as an argument, and performs a deep copy of tall the values A deep copy means that an object
     * is copied along with all the object to which it refers. In this case, all the values of the objects that the BetterArrayList refer to
     * are copied into a new double array, which is then returned.
     *
     * @param p the BetterArrayList to convert to a 2d double array
     *
     * @return a 2d double array containing all the points from the BetterArrayList
     */
    private static Waypoint[] convertPointArray(BetterArrayList<Waypoint> p) {
        return p.stream().map(Waypoint::new).toArray(Waypoint[]::new);
    }

    /**
     * This function saves the points in each path to a file in proper Java 2D array syntax. It checks if any paths are not empty and if so,
     * it saves those points to a file, otherwise it lets the user know that they cannot save nothing.
     */
    private void save() {
        if (!paths.isEmpty() || !currentPath.isEmpty()) {
            try {
                Calendar c = Calendar.getInstance();
                JFileChooser jfc = new JFileChooser();
                jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "TXT"));
                jfc.setSelectedFile(new File(String.format("%tB%te%tY-%tl%tM%tS%tp.txt", c, c, c, c, c, c, c)));

                if (jfc.showSaveDialog(g) == JFileChooser.APPROVE_OPTION) {
                    if (!jfc.getSelectedFile().getAbsolutePath().endsWith(".txt") &&
                            !jfc.getSelectedFile().getAbsolutePath().endsWith(".TXT")) {
                        jfc.setSelectedFile(new File(jfc.getSelectedFile().getAbsolutePath() + ".txt"));
                    }

                    FileWriter fw = new FileWriter(jfc.getSelectedFile().getAbsolutePath());
                    fw.write(output2DArray());
                    fw.flush();
                    fw.close();

                    JOptionPane.showConfirmDialog(g, "Points Saved Successfully!", "Points Saver", JOptionPane.DEFAULT_OPTION);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            JOptionPane.showMessageDialog(g, "You cannot save nothing!", "Points Saver", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This function removes the most recent update to the current path and adds it to a buffer. It checks if the current path is not empty
     * then removes any empty path segments from (ex.) incomplete mode switching, then checks if the new most recent path segment has any
     * path points. If this is the first time undo has been called since the last path update, or if the buffer is empty, it adds a new path
     * segment to the buffer. Then it checks if the (new) most recent path segment is drawn and if it is it removes the last point in the
     * center path from the current path and adds them to the buffer. If the most recent path is not drawn, it removes the last clicked
     * point and then regenerates the path. If the new path is valid it stays like that, otherwise if the new path is invalid, it does not
     * let you undo anymore until you fix that issue. Finally, it clears any newly created empty paths, repaints the GUI and sets the
     * previous mode to Undo.
     */
    private void undo() {
        if (!currentPath.isEmpty()) {
            redoBuffer.clickPoints.add(currentPath.clickPoints.removeLast());
            genPath(currentPath);
            outputRedoBuffer();
            fig.repaint();
        } else {
            JOptionPane.showConfirmDialog(g, "No More Undos!", "Undo Status", JOptionPane.DEFAULT_OPTION);
        }
    }

    /**
     * This function is the inverse of undo. It removes the most recent update to the buffer and adds it to the current path. It checks if
     * the redo buffer is not empty then removes any empty path segments, then checks if the current path's current path segment isn't of
     * the same type (drawn or not) as the one in the buffer and if the buffer isn't empty, and based off that it may add a new path segment
     * to the current path. Then it checks if the (new) most recent path segment is drawn and if it is it removes the last point from the
     * buffer and, adds it to the current path and then generates the left and right paths. If the most recent path is not drawn, it removes
     * the last clicked point from the buffer and then regenerates the path. Finally, it clears any newly empty path segments in the buffer,
     * repaints the GUI and sets the previous mode to Redo.
     */
    private void redo() {
        if (!redoBuffer.isEmpty()) {
            outputRedoBuffer();
            currentPath.clickPoints.add(redoBuffer.clickPoints.removeLast());
            outputRedoBuffer();
            genPath(currentPath);
            fig.repaint();
        } else {
            JOptionPane.showConfirmDialog(g, "No More Redos!", "Redo Status", JOptionPane.DEFAULT_OPTION);
        }
    }

    /**
     * This function copies all the points in all the paths to your clipboard in proper Java 2D array syntax. You can paste these points
     * wherever you please, except for in this program.
     */
    private void copy() {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(output2DArray()), fig);

        JOptionPane.showConfirmDialog(g, "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
    }

    /**
     * This function clears all the paths and essentially resets the program, if the user says yes.
     */
    private void clear() {
        if (!currentPath.isEmpty() || !paths.isEmpty()) {
            if (JOptionPane.showConfirmDialog(g, "Are you sure you want to clear everything?", "Window Clearer",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentPath.clear();
                paths.clear();
                redoBuffer.clear();
                shift = false;
                fig.repaint();
            }
        } else {
            JOptionPane.showMessageDialog(g, "You cannot clear nothing!", "Window Clearer", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This function imports a path into the program and displays it onto the field. It will prompt the user if the file they are trying to
     * import has an invalid path, and it will import any valid path previous to the invalid path in the file.
     */
    private void open() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new FileNameExtensionFilter("Text or Java Files", "txt", "TXT", "java"));

        if (jfc.showOpenDialog(g) == JFileChooser.APPROVE_OPTION) {
            String fAP = jfc.getSelectedFile().getAbsolutePath(); // File absolute path

            if (fAP.endsWith(".txt") || fAP.endsWith(".TXT") || fAP.endsWith(".java")) {
                try {
                    Scanner fileReader = new Scanner(jfc.getSelectedFile());
                    addToPathsAndClear(); // If there are already points in the current path, add it to paths and clear the current path

                    while (fileReader.hasNextLine()) {
                        String s = fileReader.nextLine();
                        // Opening syntax line
                        if (s.contains("Object[][]") && s.contains("= new Object[][]{")) {
                            if (currentPath.isEmpty()) {
                                System.out.println("Should only run once");
                            } else {
                                // Every new 2D array, check the previous one to see if it was valid
                                System.out.println("Should run every new 2D array");
                                addToPathsAndClear();
                            }

                            continue;
                        }
                        // Get rid of syntax and just keep values.
                        s = s.replaceAll("\\{", " ").replaceAll("},", " ").replaceAll("}", " ").replaceAll(";", " ");
                        s = s.trim();

                        String[] o = s.split(", ");
                        // This line must contain point values
                        if (o.length > 1) {
                            currentPath.clickPoints.add(
                                    new Waypoint(Double.parseDouble(o[0]), Double.parseDouble(o[1]), Double.parseDouble(o[2]),
                                            Double.parseDouble(o[3]), Double.parseDouble(o[4])));
                        }
                    }

                    fileReader.close();
                    System.out.println("File imported successfully!");

                    JOptionPane.showMessageDialog(g, "File imported successfully!", "File Importer", JOptionPane.INFORMATION_MESSAGE);
                } catch (FileNotFoundException ex) {
                    System.err.println("The file has magically disappeared");
                } catch (Exception ex) {
                    System.err.println("Please format the data correctly!");

                    JOptionPane.showMessageDialog(g, "You cannot import this path as it is!", "File Importer", JOptionPane.ERROR_MESSAGE);
                    currentPath.clear();
                }
            }
            // Add last imported path to paths and clear the current path.
            addToPathsAndClear();
        }
    }

    private void outputRedoBuffer() {
        redoBuffer.clickPoints.forEach(System.out::println);
    }

    /**
     * This function adds the current path to the LinkedHashMap paths and then clears the currentPath, if the currentPath isn't empty.
     */
    private void addToPathsAndClear() {
        if (!currentPath.isEmpty()) {
            paths.put(String.format("path%d", paths.size() + 1), currentPath);
            currentPath = new Path();
            fig.repaint();
        }
    }

    /**
     * The central method which paints the panel and shows the figure and is called every time an event occurs to update the GUI. In this
     * method, we cast the Graphics object copy to a Graphics2D object, which is better suited for our purposes of drawing 2D shapes and
     * objects. Then, we store the width and height of this JPanel in double variables so that they may be used later without having to call
     * the functions multiple times. Next, we draw the axes, grid lines, field elements, field border, and then all of the path data. We
     * also figure out the ratio for feet to pixels in both the x and y directions, which is used for drawing a path in the correct spot on
     * the field, and also the ratio for pixels per inch in the x and y directions, which is used to provide a buffer area for paths so that
     * you don't have to make it perfectly perpendicular to the border.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        // Probably don't need anti-aliasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double width = getWidth();
        height = getHeight();

        // Draw X and Y lines axis.
        Line2D.Double yaxis = new Line2D.Double(30, 10, 30, height - 30);
        Line2D.Double xaxis = new Line2D.Double(30, height - 30, width - 12, height - 30);
        g2.draw(yaxis);
        g2.draw(xaxis);

        // Draw ticks (the light gray grid lines and the numbers)
        double yMax = 27.0, xMax = 54.0;
        drawYTickRange(g2, yaxis, yMax);
        drawXTickRange(g2, xaxis, yaxis.getY1(), yaxis.getY2(), xMax);

        // Draw the field and everything on it
        rectWidth = (xaxis.getX2() - xaxis.getX1());
        rectHeight = (yaxis.getY2() - yaxis.getY1());
        xScale = rectWidth / xMax;
        yScale = rectHeight / yMax;
        ppiX = 1.0 / 12.0 * xScale;
        ppiY = 1.0 / 12.0 * yScale;
        fg.plotField(g2, height, xScale, yScale);
        g2.setColor(Color.black);

        Rectangle rect = new Rectangle((int) yaxis.getX1(), (int) yaxis.getY1(), (int) rectWidth, (int) rectHeight);
        g2.draw(rect);

        // Hides excess grid lines on the rightmost side of the field
        g2.setColor(super.getBackground());
        g2.fillRect(rect.x + rect.width + 1, rect.y, (int) width - rect.width - 30, rect.height + 1);

        // Plot data
        plot(g2);
    }

    /**
     * This is the function that plots everything in a path, so clickPoints (if applicable), pathSegPoints, leftPoints, and rightPoints. It
     * draws a circle of radius 2 pixels around each non-clicked point and a circle of radius 3 around each clicked point. This is why a
     * path may appear to be crossing a field element, when in reality it is not because the edge of the robot ends at the value of the
     * point. This function also draws a line between each point to show that they are all connected in the same path.
     *
     * @param g2   the 2D graphics object used to draw everything
     * @param path the path to draw/plot
     */
    private void plotPath(Graphics2D g2, Path path) {
        System.out.println("Path point list sizes: " + path.pathSegPoints.size() + " " + path.clickPoints.size());
        // Draw clicked points
        if (path.clickPoints.size() > 1) {
            for (int j = 0; j < path.clickPoints.size() - 1; j++) {
                double x1 = 30 + xScale * path.clickPoints.get(j).x, y1 = height - 30 - yScale * path.clickPoints.get(j).y;
                double x2 = 30 + xScale * path.clickPoints.get(j + 1).x, y2 = height - 30 - yScale * path.clickPoints.get(j + 1).y;

                g2.setPaint(Color.magenta);
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
                g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                g2.fill(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
            }
        } else if (path.clickPoints.size() == 1) {
            double x1 = 30 + xScale * path.clickPoints.get(0).x, y1 = height - 30 - yScale * path.clickPoints.get(0).y;

            g2.setPaint(Color.magenta);
            g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
        }

        // Draw all the pathSegPoints, leftPoints and rightPoints
        if (path.pathSegPoints.size() > 1) {
            for (int j = 0; j < path.pathSegPoints.size() - 1; j++) {
                double x1 = 30 + xScale * path.pathSegPoints.get(j).x, y1 = height - 30 - yScale * path.pathSegPoints.get(j).y;
                double x2 = 30 + xScale * path.pathSegPoints.get(j + 1).x, y2 = height - 30 - yScale * path.pathSegPoints.get(j + 1).y;

                g2.setPaint(Color.green);
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
                g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
                g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));

                if (!path.leftPSPoints.isEmpty() && j < path.leftPSPoints.size() - 1) {
                    double lx1 = 30 + xScale * path.leftPSPoints.get(j).x, ly1 = height - 30 - yScale * path.leftPSPoints.get(j).y;
                    double lx2 = 30 + xScale * path.leftPSPoints.get(j + 1).x, ly2 = height - 30 - yScale * path.leftPSPoints.get(j + 1).y;
                    double rx1 = 30 + xScale * path.rightPSPoints.get(j).x, ry1 = height - 30 - yScale * path.rightPSPoints.get(j).y;
                    double rx2 = 30 + xScale * path.rightPSPoints.get(j + 1).x, ry2 =
                            height - 30 - yScale * path.rightPSPoints.get(j + 1).y;

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
        } else if (path.pathSegPoints.size() == 1) {
            double x1 = 30 + xScale * path.pathSegPoints.get(0).x, y1 = height - 30 - yScale * path.pathSegPoints.get(0).y;

            g2.setPaint(Color.green);
            g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
        }
    }

    /**
     * This function plots all the paths in the current user session. It stores the last color of the Graphics object and then resets the
     * Graphics object to that color after.
     *
     * @param g2 the 2D graphics object used to draw everything
     */
    private void plot(Graphics2D g2) {
        Color tempC = g2.getColor();

        // Plot the current path then loop through paths and plot each
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
        double upperY_tic = Math.ceil(Max);

        // The starting and ending x and y values for the y-axis
        double x0 = yaxis.getX1();
        double y0 = yaxis.getY1();
        double xf = yaxis.getX2();
        double yf = yaxis.getY2();

        // Calculate step-size between ticks and length of Y axis using distance formula
        // Total distance of the axis / number of ticks = distance per tick
        double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperY_tic;

        double upper = upperY_tic;
        // Iterates through each number from 0 to the max length of the y-axis in feet and draws the horizontal grid
        // lines for each iteration except the last one
        for (int i = 0; i <= upperY_tic; i++) {
            double newY = y0;
            // calculate width of number for proper drawing
            String number = new DecimalFormat("#.#").format(upper);
            FontMetrics fm = getFontMetrics(getFont());
            int width = fm.stringWidth(number);

            // Draws a tick line and the corresponding number at that value in black
            g2.draw(new Line2D.Double(x0, newY, x0 - 10, newY));
            g2.drawString(number, (float) x0 - 15 - width, (float) newY + 5);

            // add grid lines to chart
            if (i != upperY_tic) {
                Stroke tempS = g2.getStroke();
                Color tempC = g2.getColor();

                // Sets the color and stroke to light gray dashes and draws them all the way across the JPanel
                g2.setColor(Color.lightGray);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
                g2.draw(new Line2D.Double(30, newY, getWidth(), newY));

                g2.setColor(tempC);
                g2.setStroke(tempS);
            }
            // Increment the number to draw and the position of the tick
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
        double upperX_tic = Math.ceil(Max);

        // The starting and ending x and y values for the x-axis
        double x0 = xaxis.getX1();
        double y0 = xaxis.getY1();
        double xf = xaxis.getX2();
        double yf = xaxis.getY2();

        // calculate step-size between ticks and length of Y axis using distance formula
        // Total distance of the axis / number of ticks = distance per tick
        double distance = Math.sqrt(Math.pow((xf - x0), 2) + Math.pow((yf - y0), 2)) / upperX_tic;

        double lower = 0;
        // Iterates through each number from 0 to the max length of the x-axis in feet and draws the horizontal grid
        // lines for each iteration except the last one
        for (int i = 0; i <= upperX_tic; i++) {
            double newX = x0;
            // calculate width of number for proper drawing
            String number = new DecimalFormat("#.#").format(lower);
            FontMetrics fm = getFontMetrics(getFont());
            int width = fm.stringWidth(number);

            // Draws a tick line and the corresponding number at that value in black
            g2.draw(new Line2D.Double(newX, yf, newX, yf + 10));
            g2.drawString(number, (float) (newX - (width / 2.0)), (float) yf + 25);

            // add grid lines to chart
            if (i != 0) {
                Stroke tempS = g2.getStroke();
                Color tempC = g2.getColor();

                // Sets the color and stroke to light gray dashes and draws them all the way down to the x-axis
                g2.setColor(Color.lightGray);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
                g2.draw(new Line2D.Double(newX, yTickYMax, newX, height - (height - yTickYMin)));

                g2.setColor(tempC);
                g2.setStroke(tempS);
            }
            // Increment the number to draw and the position of the tick
            lower += 1;
            x0 = newX + distance;
        }
    }

    @Override
    public void lostOwnership(Clipboard clip, Transferable transferable) {
        // We must keep the object we placed on the system clipboard until this method is called.
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

        System.out.println("2D Array output: " + output);
        return output.toString();
    }

    /**
     * This function does the actual formatting and appending for all the values in a path to a StringBuilder
     *
     * @param output the StringBuilder to append the values of the path to.
     * @param value  the path to parse, format and get the values from.
     */
    private void outputPath(StringBuilder output, Path value) {
        output.append(value.clickPoints.stream().map(clickPoint -> "{" + clickPoint + "},\n").collect(Collectors.joining()));
        output.append("};\n");
    }

    /**
     * This function generates the left and right paths from a center path and returns those paths. All math is in radians because Java's
     * math library uses radians for its trigonometric calculations. First, it checks if the original path has more than one point because
     * you can't calculate a heading from one point only. Then, it calculates the heading between 2 consecutive points in the original path
     * and stores those values in an array. Next, it sets the last point to the same heading as the previous point so that the ending of the
     * path is more accurate. Finally, it calculates the new point values (in feet) and returns those paths in the BetterArrayList of
     * BetterArrayLists.
     *
     * @param points        the original center path to generate the left and right values from (point values are in feet)
     * @param robotTrkWidth the robot track width (used as the actual width of the robot for simplicity)
     *
     * @return A BetterArrayList of the left and right paths (Stored in BetterArrayLists also) for the robot
     */
    private BetterArrayList<BetterArrayList<Waypoint>> leftRight(BetterArrayList<Waypoint> points, double robotTrkWidth) {
        BetterArrayList<BetterArrayList<Waypoint>> temp = new BetterArrayList<>();
        temp.add(new BetterArrayList<>(points.size())); // Left
        temp.add(new BetterArrayList<>(points.size())); // Right
        double[] heading = new double[points.size()];

        // System.out.println(points.size());

        if (points.size() > 1) {
            // Heading calculation
            for (int i = 0; i < points.size() - 1; i++) {
                double x1 = points.get(i).x, x2 = points.get(i + 1).x, y1 = points.get(i).y, y2 = points.get(i + 1).y;
                heading[i] = Math.atan2(y2 - y1, x2 - x1);
            }

            // Makes the last heading value = to the 2nd last for a smoother path.
            heading[heading.length - 1] = heading[heading.length - 2];

            // Point value calculation, temp.get(0) and temp.get(1) are the left and right paths respectively
            // Pi / 2 rads = 90 degrees
            // leftX = trackWidth / 2 * cos(calculatedAngleAtThatIndex + Pi / 2) + centerPathXValueAtThatIndex
            // leftY = trackWidth / 2 * sin(calculatedAngleAtThatIndex + Pi / 2) + centerPathYValueAtThatIndex
            // rightX = trackWidth / 2 * cos(calculatedAngleAtThatIndex - Pi / 2) + centerPathXValueAtThatIndex
            // rightY = trackWidth / 2 * sin(calculatedAngleAtThatIndex - Pi / 2) + centerPathYValueAtThatIndex
            for (int i = 0; i < heading.length; i++) {
                temp.get(0).add(i, new Waypoint(robotTrkWidth / 2 * Math.cos(heading[i] + Math.PI / 2) + points.get(i).x,
                        robotTrkWidth / 2 * Math.sin(heading[i] + Math.PI / 2) + points.get(i).y, points.get(i)));
                temp.get(1).add(i, new Waypoint(robotTrkWidth / 2 * Math.cos(heading[i] - Math.PI / 2) + points.get(i).x,
                        robotTrkWidth / 2 * Math.sin(heading[i] - Math.PI / 2) + points.get(i).y, points.get(i)));
            }
        } else if (points.size() == 1) {
            temp.get(0).add(0, new Waypoint(points.get(0).x, points.get(0).y, points.get(0)));
        }

        return temp;
    }

    /**
     * This function generates pathSegPoints from the clicked points and is only called with a clicked Path. It generates the center path
     * first and then if that wasn't null, it will generate the left and right paths, check if those are valid, assign those to the given
     * pathSegment's left and right path BetterArrayLists respectively, and return true, else it will remove the last point if remove is
     * true, show the fieldError and return false.
     *
     * @param ps the pathSegment to get the clickPoints from and generate a paths from them
     */
    private void genPath(Path ps) {
        // The path generator object that also stores the points of the generator path
        MPGen2D pathGen = new MPGen2D(convertPointArray(ps.clickPoints));
        BetterArrayList<Waypoint> temp = convert2DArray(pathGen);
        BetterArrayList<BetterArrayList<Waypoint>> lAndR = leftRight(temp, 24.0889 / 12.0);

        ps.pathSegPoints = temp;
        ps.leftPSPoints = lAndR.get(0);
        ps.rightPSPoints = lAndR.get(1);
    }

    /**
     * This function gets the cursor position, constrains it to one inch inside the field and converts it to feet. The once in buffer is
     * used to make generating a path that starts at the field border, easier to create.
     *
     * @param p the point where the cursor is on the JFrame
     *
     * @return an array with the x and y value of that point in feet
     */
    private double[] getCursorFeet(java.awt.Point p) {
        if (p != null) {
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
     *
     * @return an array with the x and y value of that point in pixels
     */
    private int[] getCursorPixels(java.awt.Point p) {
        if (p != null) {
            int[] temp = new int[2];

            temp[0] = (int) constrainTo(p.getX() - 30, ppiX, rectWidth - ppiX);
            temp[1] = (int) constrainTo(((height - 30 + g.getJMenuBar().getHeight()) - p.getY()), ppiY, rectHeight - ppiY);

            return temp;
        }

        return null;
    }

    /**
     * Get the waypoint's kinematic values
     *
     * @return the yaw, speed and acceleration values for the waypoint
     */
    private double[] getWaypointKinematicValues() {
        String s;

        if ((s = JOptionPane.showInputDialog("Enter a yaw (deg), speed, and accel value:")) != null) {
            double[] temp = Arrays.stream(s.split(", ")).mapToDouble(Double::parseDouble).toArray();

            temp[0] = Math.toRadians(temp[0]);

            return temp;
        }

        return null;
    }

    /**
     * Handles the actions that are generated by whenever you click on the menuItems or use the mnemonics Based on the action command (the
     * name of the menuItem that triggered the event) it runs the respective function.
     */
    class MenuListener implements ActionListener {
        /**
         * This function overrides the function in the interface it implements and is called whenever a JMenuItem that has this class as
         * it's listener is selected, either through a click or through the mnemonic. We figure out which JMenuItem was selected based off
         * the name of the action command (name of JMenuItem) and then call the corresponding function.
         *
         * @param e the ActionEvent that is created whenever a JMenuItem is selected
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Undo (Ctrl + Z)")) {
                undo();
            } else if (e.getActionCommand().equals("Redo (Ctrl + Y)")) {
                redo();
            } else if (e.getActionCommand().equals("Copy Points (Ctrl + C)")) {
                copy();
            } else if (e.getActionCommand().equals("New Path (Ctrl + N)") && !currentPath.isEmpty()) {
                addToPathsAndClear();
            } else if (e.getActionCommand().equals("Open (Ctrl + O)")) {
                open();
            } else if (e.getActionCommand().equals("Save (Ctrl + S)")) {
                save();
            } else if (e.getActionCommand().equals("Clear All (Ctrl + A)")) {
                clear();
            }
        }
    }

    /**
     * Same thing as the MenuListener but instead it listens for key presses and specific keyboard shortcuts to call the respective
     * function.
     */
    class KeyboardListener extends KeyAdapter {
        /**
         * This function overrides the function in the superclass and is called whenever a key is pressed while the JFrame has focus. We
         * figure out what keys were pressed, and if it was a specified keyboard shortcut, we call the corresponding function.
         *
         * @param e the KeyEvent that is created whenever a key is pressed
         */
        @Override
        public void keyPressed(KeyEvent e) {
            System.out.println("Key pressed: " + e.getExtendedKeyCode());

            if (e.isControlDown()) {
                if (e.getExtendedKeyCode() == 90) { // CTRL + Z
                    undo();
                } else if (e.getExtendedKeyCode() == 89) { // CTRL + Y
                    redo();
                } else if (e.getExtendedKeyCode() == 67) { // CTRL + C
                    copy();
                } else if (e.getExtendedKeyCode() == 78 && !currentPath.isEmpty()) { // CTRL + N
                    addToPathsAndClear();
                } else if (e.getExtendedKeyCode() == 79) { // CTRL + O
                    open();
                } else if (e.getExtendedKeyCode() == 83) { // CTRL + S
                    save();
                } else if (e.getExtendedKeyCode() == 65) { // CTRL + A
                    clear();
                }
            }

            // Shift is set to true for as long as a shift key is held down. Used for the shift-clicking path functionality
            if (e.getExtendedKeyCode() == 16) {
                shift = true;
            }
        }

        /**
         * This function is called whenever a key is released and the JFrame has focus.
         *
         * @param e the KeyEvent generated whenever a key is released
         */
        @Override
        public void keyReleased(KeyEvent e) {
            // Shift is set to false when the shift key that was held down is released. Used for the shift-clicking path functionality.
            if (e.getExtendedKeyCode() == 16) {
                shift = false;
            }
        }
    }

    /**
     * Handles the closing of the main JFrame. All it does is it gives you the option to save your points when you try to close the window,
     * in case you forgot to save when you were using the program.
     */
    class WindowListener extends WindowAdapter {
        /**
         * This function overrides the function in the superclass and is called whenever the X button is clicked on the JFrame. If there is
         * at least one Point in the current user session, it prompts the user if they want to save their point(s)/path(s). If the user says
         * no, the program exits, if the user cancels the program continues, and if the user says yes then the save() function is called. If
         * the current user session has no visible points on the field, the program exits.
         *
         * @param e the WindowEvent (unused) which is generated, in this case, when the X button is clicked.
         */
        @Override
        public void windowClosing(WindowEvent e) {
            if (!paths.isEmpty() || !currentPath.isEmpty()) {
                int response = JOptionPane.showConfirmDialog(g, "Do you want to save your points?", "Point Saver",
                        JOptionPane.YES_NO_CANCEL_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    save();
                    System.exit(0);
                } else if (response == JOptionPane.NO_OPTION) {
                    System.exit(0);
                }
            } else {
                System.exit(0);
            }
        }
    }

    /**
     * Handles the motion and position of the mouse and runs different functions accordingly.
     */
    class MouseListener extends MouseAdapter {
        // A custom cursor for letting the user know that their cursor is over an invalid position.
        final Cursor invalid_cursor;

        {
            // Initialize the cursor. Checks if the included image is in the correct file location, and if it is it creates the cursor at
            // the center of the actual image excluding the background.
            invalid_cursor = Toolkit.getDefaultToolkit()
                    .createCustomCursor(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/cursor.png")),
                            new java.awt.Point(7, 7), "invalid");
        }

        /**
         * This function overrides the function in the superclass and is called whenever Java detects the mouse has been dragged. A drag
         * consists of a mouse button being held down and the mouse being moved simultaneously. If you've clicked on a valid point to move,
         * it will move that point instead, to whatever valid location your cursor is at and then update the path and GUI accordingly.
         * <p>
         * If the user is not trying to shift-click a path, then if the user's cursor is within the JFrame, then if the user is trying to
         * move a clicked point (moveFlag[1] > -1), move that point based on the cursor position, otherwise call updateWaypoints(true). If
         * the user drags their cursor out of the JFrame prompt the user to move their cursor back into the window.
         *
         * @param e the (unused) MouseEvent generated when the mouse is dragged (button is held down and the mouse is moved)
         **/
        @Override
        public void mouseDragged(MouseEvent e) {
            if (!shift) {
                System.out.println("Mouse dragged moveFlag[0]: " + moveFlag[0]);
                int mf1 = (int) moveFlag[1];

                if (mf1 > -1) {
                    double[] point;

                    if ((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
                        if (moveFlag[0].equals("current")) {
                            currentPath.clickPoints.get(mf1).x = point[0];
                            currentPath.clickPoints.get(mf1).y = point[1];
                        } else {
                            paths.get(moveFlag[0].toString()).clickPoints.get(mf1).x = point[0];
                            paths.get(moveFlag[0].toString()).clickPoints.get(mf1).y = point[1];
                        }

                        fig.repaint();
                    } else {
                        JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        /**
         * This functions overrides the function in the superclass and is called whenever the mouse is moved. First, we check if the mouse
         * cursor is in the JFrame/window and if it is we constrain it to the field border and keep the values in pixels, and we take
         * another snapshot of the mouse position and then check if that is within the window. If it is, we shift it to the actual position
         * of the cursor relative to the field and keep it as pixels. This is the actual, unconstrained position of the cursor. We then feed
         * those cursor values into the findPoint() function and if it doesn't find a point, within a 7x7 range of the cursor with the
         * cursor in the center, in the current path and the cursor is not in an invalid field element, if paths is empty it sets the cursor
         * to the default cursor. Otherwise, it searches through paths for the exact same thing as it does with currentPath. If it still
         * can't find a point, then it sets the cursor to the default arrow cursor. If the function finds a movable point, it sets the
         * cursor to a hand cursor. If the function finds a point that cannot be moved or the cursor position is invalid, it changes the
         * cursor to the custom invalid cursor.
         *
         * @param e the (unused) MouseEvent that is generated whenever the mouse is moved.
         */
        @Override
        public void mouseMoved(MouseEvent e) {
            int[] point;

            if ((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
                java.awt.Point p = g.getRootPane().getMousePosition();

                if (p != null) {
                    p.x = p.x - 30;
                    p.y = (height - 30 + g.getJMenuBar().getHeight()) - p.y;

                    if (!findPoint(currentPath, point[0], point[1])) {
                        if (paths.isEmpty()) {
                            g.setCursor(Cursor.getDefaultCursor());
                        }

                        for (Map.Entry<String, Path> en : paths.entrySet()) {
                            if (findPoint(en.getValue(), point[0], point[1])) {
                                return;
                            }
                        }

                        g.setCursor(Cursor.getDefaultCursor());
                    }
                }
            }
        }

        /**
         * This function does the actual searching for a point through the specified path. The aim of this function is to modify the cursor
         * based upon what its close to (in terms of a clicked point) or in (in terms of an invalid field area). First, it searches through
         * all the clicked points in non-drawn path segments in a path, and if the cursor is within a 7x7 area, with the clicked point in
         * the middle, of a movable clicked point, it changes the cursor to a hand and returns true, otherwise if the point is not movable,
         * it changes the cursor to the custom invalid one and returns true. If the cursor is not near a clicked point in that path, it then
         * checks if the cursor is inside an invalid field area, other than the field border. If it is, then it sets the cursor to the
         * invalid one and returns true. Regarding the field border, it checks if the path is empty and if it is then it continues to the
         * next field element, otherwise it checks if the actual position of the cursor is outside the field border. If it is then it sets
         * the cursor to the invalid one and returns true. The reason why it checks if the path is empty is to make it easier for the user
         * to start a path along the field border. Because the first Point is automatically constrained to inside the field, you don't need
         * to display an unwieldy cursor that does not tell you, precisely, where the point will be created if you click.
         *
         * @param path the path to search through
         * @param x    the x value of the constrained cursor position
         * @param y    the y value of the constrained cursor position
         *
         * @return false if the cursor is not near a clicked point in that path or in any invalid area, true otherwise
         */
        private boolean findPoint(Path path, int x, int y) {
            int tx, ty;

            for (Waypoint po : path.clickPoints) {
                for (int k = -3; k < 4; k++) {
                    tx = (int) (po.x * xScale) + k;

                    for (int l = -3; l < 4; l++) {
                        ty = (int) (po.y * yScale) + l;

                        if (x == tx && y == ty) {
                            g.setCursor(invalid_cursor);
                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * This function overrides the function in the superclass and is called whenever a mouse button is clicked. First we check if shift
         * is held down or not. If it is we multi-thread the search for the point that was clicked. For each path in paths, a new Thread is
         * created to search through that path for the closest point in a 7x7 area around the point located at the center. If a point is
         * found, an AtomicBoolean is set to true by the first thread that finds a match. All subsequent thread's cannot change the value of
         * this boolean, so they all finish execution and die. This ensures that the path is only switched once to the correct path and
         * that's it, even if there are paths that overlap. When a path is swapped, the currentPath gets stored in place of the path in
         * paths and that path is swapped to the currentPath, so that you can modify it as you normally would any currentPath and the
         * redoBuffer is cleared to prevent issues form arising.
         * <p>
         * If shift is not held, then we just call updateWaypoints(false).
         *
         * @param e the MouseEvent (unused) generated when the mouse is clicked in the component
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (shift) {
                AtomicBoolean b = new AtomicBoolean(false);
                int[] point;

                if ((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
                    java.awt.Point p = new java.awt.Point(point[0], point[1]), temp = new java.awt.Point();
                    System.out.println("Mouse clicked: " + p.x + " " + p.y);

                    // TODO: Instead of creating a new thread every time, re-use existing threads (perhaps in a pool)
                    paths.forEach((key, value) -> new Thread(() -> {
                        for (Waypoint po : value.pathSegPoints) {
                            for (int k = -3; k < 4; k++) {
                                temp.x = (int) (po.x * xScale) + k;

                                for (int l = -3; l < 4; l++) {
                                    temp.y = (int) (po.y * yScale) + l;

                                    if (p.equals(temp)) {
                                        if (b.compareAndSet(false, true)) {
                                            JOptionPane.showMessageDialog(g, "Successfully switched paths!", "Path Switcher",
                                                    JOptionPane.INFORMATION_MESSAGE);
                                            System.out.println("Path switch successful");

                                            Path swap = currentPath;
                                            currentPath = paths.get(key);
                                            paths.put(key, swap);

                                            redoBuffer.clear();
                                            shift = false;
                                        }

                                        return;
                                    }
                                }
                            }
                        }
                    }).start());
                }
            } else {
                updateWaypoints();
            }
        }

        /**
         * This function overrides the function in the superclass and is called whenever a mouse button is pressed down. It attempts to find
         * a clicked point for moving to modify path segments. First it checks if the cursor is inside the window. If it isn't then it
         * prompts the user to move it back in. If it is, then it resets the moveFlag array to the default states and searches through the
         * current path to find a clicked point. If it can't find it in the current path it looks through all the stored paths in paths (the
         * LinkedHashMap).
         *
         * @param e the (unused) MouseEvent that is generated whenever a mouse button is pressed.
         */
        @Override
        public void mousePressed(MouseEvent e) {
            int[] point;

            if ((point = getCursorPixels(g.getRootPane().getMousePosition())) != null) {
                java.awt.Point p = new java.awt.Point(point[0], point[1]);
                moveFlag = new Object[]{"current", -1};

                // Check the current path to see if the clicked point is a part of it and if it isn't then check all the other paths
                // Only check the other paths if you can't find it in the current one to save time and resources instead of needlessly
                // searching through extra paths
                if (!findClickedPoint(currentPath, p)) {
                    System.out.println("Mouse pressed moveFlag[0]: " + moveFlag[0]);

                    for (Map.Entry<String, Path> en : paths.entrySet()) {
                        if (findClickedPoint(en.getValue(), p)) {
                            break;
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        /**
         * This function actually does the searching for the clicked point. It is similar to the previous "find" function but instead of
         * changing the cursor it sets the moveFlag array accordingly, and if it's an unmovable point, it prompts the user to let them know
         * that that point is unmovable.
         *
         * @param path the actual path to search through
         * @param p    the cursor point
         *
         * @return false if it can't find a corresponding point, true otherwise
         */
        private boolean findClickedPoint(Path path, java.awt.Point p) {
            java.awt.Point temp = new java.awt.Point();

            for (Waypoint po : path.clickPoints) {
                for (int k = -3; k < 4; k++) {
                    temp.x = (int) (po.x * xScale) + k;

                    for (int l = -3; l < 4; l++) {
                        temp.y = (int) (po.y * yScale) + l;

                        if (p.equals(temp)) {
                            JOptionPane.showMessageDialog(g, "You cannot move this point!", "Point Mover", JOptionPane.ERROR_MESSAGE);

                            moveFlag = new Object[]{-2, -2};

                            return true;
                        }
                    }
                }
            }

            return false;
        }

        /**
         * This function handles all logic for updating the path correctly with the new point, if the new point is valid.
         */
        private void updateWaypoints() {
            // Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
            double[] point;

            if ((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
                // Add the new point to the end of the current Path and then generate a new path accordingly.
                double[] values = getWaypointKinematicValues();

                if (values != null && values.length == 3) {
                    currentPath.clickPoints.add(new Waypoint(point[0], point[1], values[0], values[1], values[2]));
                    genPath(currentPath);

                    // Every time a new point is added, clear the redo buffer
                    redoBuffer.clear();
                    System.out.println("Waypoint updated: " + point[0] + " " + point[1]);
                }

                fig.repaint();
            } else {
                JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
