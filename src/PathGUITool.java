import util.FieldGenerator;
import util.Path;
import util.PointMarker;
import util.Waypoint;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The program takes the mouse position on the field drawn in the GUI and then based off of that, when the mouse button is clicked, it
 * prompts the user for a desired heading, velocity and acceleration for that point and then adds that point to a series of points and
 * generates a path accordingly. The path generation is based off quintic spline interpolation.
 *
 * @author Aaron Pinto
 * @see FieldGenerator
 * @see PathGen2D
 */
public final class PathGUITool extends JPanel implements ClipboardOwner {
    // An object of this class, used for the Ctrl + C code
    private static PathGUITool fig;

    private final double borderSize = 30;
    private final FieldGenerator fieldGen = new FieldGenerator();
    // The JFrame for this GUI. It actually displays the window
    private final JFrame g = new JFrame("Path GUI Tool");
    // LinkedHashMap to store the name and path for all previous paths
    private final LinkedHashMap<String, Path> paths = new LinkedHashMap<>();
    // Path for storing the Ctrl + Z'd points
    private final Path redoBuffer = new Path();
    private final Path currentPath = new Path();
    /**
     * doubles for storing important values, xScale and yScale are the values for pixels per foot for each axis, respectively, yTickYMax and
     * Min are the max and min values of the y-axis in pixels, rectWidth and Height are the width and height of the field border in pixels,
     * robotTrkWidth is the track width of the robot in feet, and ppiX and Y are the pixels per inch for each axis respectively.
     */
    private double xScale, yScale, rectWidth, rectHeight, ppiX, ppiY;
    // Height is an integer which stores the height of this panel in pixels
    private int height;
    // A class to store the values necessary to move a clicked point in paths
    private PointMarker moveFlag = PointMarker.DEFAULT;

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
        if (currentPath.isNotEmpty()) {
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
        if (redoBuffer.isNotEmpty()) {
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
     * This function clears all the paths and essentially resets the program, if the user says yes.
     */
    private void clear() {
        if (currentPath.isNotEmpty() || !paths.isEmpty()) {
            if (JOptionPane.showConfirmDialog(g, "Are you sure you want to clear everything?", "Window Clearer",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                currentPath.clear();
                paths.clear();
                redoBuffer.clear();
                moveFlag = PointMarker.DEFAULT;
                fig.repaint();
            }
        } else {
            JOptionPane.showMessageDialog(g, "You cannot clear nothing!", "Window Clearer", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This function copies all the points in all the paths to your clipboard in proper Java 2D array syntax. You can paste these points
     * wherever you please, except for in this program.
     */
    private void copy() {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(new StringSelection(Utils.outputWaypointArray(currentPath, paths)), fig);

        JOptionPane.showConfirmDialog(g, "Waypoints copied to clipboard!", "Points Copier", JOptionPane.DEFAULT_OPTION);
    }

    /**
     * This function saves the points in each path to a file in proper Java 2D array syntax. It checks if any paths are not empty and if so,
     * it saves those points to a file, otherwise it lets the user know that they cannot save nothing.
     */
    private void save() {
        if (currentPath.isNotEmpty() || !paths.isEmpty()) {
            Calendar c = Calendar.getInstance();
            JFileChooser jfc = new JFileChooser();
            jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "TXT"));
            jfc.setSelectedFile(new File(String.format("%tB%te%tY-%tH%tM%tS.txt", c, c, c, c, c, c)));

            if (jfc.showSaveDialog(g) == JFileChooser.APPROVE_OPTION) {
                String fileAbsPath = jfc.getSelectedFile().getAbsolutePath();
                if (!fileAbsPath.endsWith(".txt") && !fileAbsPath.endsWith(".TXT")) {
                    fileAbsPath += ".txt";
                }

                try (var oos = new ObjectOutputStream(new FileOutputStream(fileAbsPath))) {
                    oos.writeObject(currentPath);
                    oos.writeObject(paths);
                    oos.flush();
                    JOptionPane.showConfirmDialog(g, "Points Saved Successfully!", "Points Saver", JOptionPane.DEFAULT_OPTION);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            JOptionPane.showMessageDialog(g, "You cannot save nothing!", "Points Saver", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This function imports a path into the program and displays it onto the field. It will prompt the user if the file they are trying to
     * import has an invalid path, and it will import any valid path previous to the invalid path in the file.
     */
    private void open() {
        JFileChooser jfc = new JFileChooser();
        jfc.setFileFilter(new FileNameExtensionFilter("Text Files", "txt", "TXT"));

        if (jfc.showOpenDialog(g) == JFileChooser.APPROVE_OPTION) {
            String fileAbsPath = jfc.getSelectedFile().getAbsolutePath();

            if (fileAbsPath.endsWith(".txt") || fileAbsPath.endsWith(".TXT")) {
                try (var ois = new ObjectInputStream(new FileInputStream(jfc.getSelectedFile()))) {
                    addToPathsAndClear(currentPath); // Handle if there are already points in the current path

                    addToPathsAndClear((Path) ois.readObject()); // Read the saved current path

                    var tempPaths = (LinkedHashMap<String, Path>) ois.readObject(); // Read the saved previous paths
                    tempPaths.values().forEach(this::addToPathsAndClear);

                    System.out.println("File imported successfully!");
                    JOptionPane.showMessageDialog(g, "File imported successfully!", "File Importer", JOptionPane.INFORMATION_MESSAGE);
                } catch (FileNotFoundException ex) {
                    System.err.println("The file has magically disappeared!");
                } catch (Exception ex) {
                    System.err.println("Please format the data correctly!");
                    JOptionPane.showMessageDialog(g, "You cannot import this path as is!", "File Importer", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void outputRedoBuffer() {
        System.out.println("Redo buffer:");
        redoBuffer.clickPoints.forEach(System.out::println);
    }

    /**
     * This function adds the current path to the LinkedHashMap paths and then clears the currentPath, if the currentPath isn't empty.
     */
    private void addToPathsAndClear(Path path) {
        if (path.isNotEmpty()) {
            paths.put(String.format("path%d", paths.size() + 1), new Path(path));
            path.clear();
            fig.repaint();
        }
    }

    /**
     * The central method which paints the panel and shows the figure and is called every time an event occurs to update the GUI. In this
     * method, we cast the Graphics object copy to a Graphics2D object, which is better suited for our purposes of drawing 2D shapes and
     * objects. Then, we store the width and height of this JPanel in double variables so that they may be used later without having to call
     * the functions multiple times. Next, we draw the axes, grid lines, field elements, field border, and then all the path data. We also
     * figure out the ratio for feet to pixels in both the x and y directions, which is used for drawing a path in the correct spot on the
     * field, and also the ratio for pixels per inch in the x and y directions, which is used to provide a buffer area for paths so that you
     * don't have to make it perfectly perpendicular to the border.
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
        Line2D.Double x_axis = new Line2D.Double(borderSize, height - borderSize, width - 12, height - borderSize);
        Line2D.Double y_axis = new Line2D.Double(borderSize, 10, borderSize, height - borderSize);
        g2.draw(x_axis);
        g2.draw(y_axis);

        // Draw ticks (the light gray grid lines and the numbers)
        double yMax = 27.0, xMax = 54.0;
        GraphicsUtils.drawXTickRange(this, g2, x_axis, y_axis.getY1(), y_axis.getY2(), xMax);
        GraphicsUtils.drawYTickRange(this, g2, y_axis, x_axis.getX1(), x_axis.getX2(), yMax);

        // Draw the field and everything on it
        rectWidth = (x_axis.getX2() - x_axis.getX1());
        rectHeight = (y_axis.getY2() - y_axis.getY1());
        xScale = rectWidth / xMax;
        yScale = rectHeight / yMax;
        ppiX = 1.0 / 12.0 * xScale;
        ppiY = 1.0 / 12.0 * yScale;
        fieldGen.plotField(g2, height, xScale, yScale);

        Rectangle rect = new Rectangle((int) y_axis.getX1(), (int) y_axis.getY1(), (int) rectWidth, (int) rectHeight);
        g2.setColor(Color.black);
        g2.draw(rect);

        // Plot data
        GraphicsUtils.plot(g2, currentPath, paths, borderSize, xScale, height - borderSize, yScale);
    }

    @Override
    public void lostOwnership(Clipboard clip, Transferable transferable) {
        // We must keep the object we placed on the system clipboard until this method is called.
    }

    /**
     * This function generates pathSegPoints from the clicked points and is only called with a clicked Path. It generates the center path
     * first and then if that wasn't null, it will generate the left and right paths, check if those are valid, assign those to the given
     * pathSegment's left and right path BetterArrayLists respectively, and return true, else it will remove the last point if remove is
     * true, show the fieldError and return false.
     *
     * @param path the pathSegment to get the clickPoints from and generate a paths from them
     */
    private void genPath(Path path) {
        // The path generator object that also stores the points of the generator path
        PathGen2D pathGen = new PathGen2D(Utils.convertPointArray(path.clickPoints));
        var lAndR = pathGen.leftRight(1.744792);

        path.pathPoints = Utils.convertResults(pathGen.results);
        path.leftPoints = lAndR.get(0);
        path.rightPoints = lAndR.get(1);
    }

    /**
     * This function gets the cursor position, constrains it to one inch inside the field and converts it to feet. The once in buffer is
     * used to make generating a path that starts at the field border, easier to create.
     *
     * @param p the point where the cursor is on the JFrame
     *
     * @return an array with the x and y value of that point in feet
     */
    private double[] getCursorFeet(Point p) {
        if (p != null) {
            double[] temp = new double[2];

            temp[0] = Utils.constrainTo(p.getX() - borderSize, ppiX, rectWidth - ppiX) / xScale;
            temp[1] = Utils.constrainTo((height - borderSize + g.getJMenuBar().getHeight()) - p.getY(), ppiY, rectHeight - ppiY) / yScale;

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
    private int[] getCursorPixels(Point p) {
        if (p != null) {
            int[] temp = new int[2];

            temp[0] = (int) Utils.constrainTo(p.getX() - borderSize, ppiX, rectWidth - ppiX);
            temp[1] = (int) Utils.constrainTo((height - borderSize + g.getJMenuBar().getHeight()) - p.getY(), ppiY, rectHeight - ppiY);

            return temp;
        }

        return null;
    }

    private void displayBoundaryWarning() {
        JOptionPane.showMessageDialog(g, "Please move your cursor back into the window!", "Boundary Monitor", JOptionPane.ERROR_MESSAGE);
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
            } else if (e.getActionCommand().equals("New Path (Ctrl + N)") && currentPath.isNotEmpty()) {
                addToPathsAndClear(currentPath);
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
            if (e.isControlDown()) {
                if (e.getExtendedKeyCode() == 90) { // CTRL + Z
                    undo();
                } else if (e.getExtendedKeyCode() == 89) { // CTRL + Y
                    redo();
                } else if (e.getExtendedKeyCode() == 67) { // CTRL + C
                    copy();
                } else if (e.getExtendedKeyCode() == 78 && currentPath.isNotEmpty()) { // CTRL + N
                    addToPathsAndClear(currentPath);
                } else if (e.getExtendedKeyCode() == 79) { // CTRL + O
                    open();
                } else if (e.getExtendedKeyCode() == 83) { // CTRL + S
                    save();
                } else if (e.getExtendedKeyCode() == 65) { // CTRL + A
                    clear();
                }
            }

            if (!moveFlag.equals(PointMarker.DEFAULT)) {
                double x_inc = 0.0, y_inc = 0.0;

                if (e.getExtendedKeyCode() == 38) { // Up
                    y_inc = 1.0 / ppiY / 12.0;
                } else if (e.getExtendedKeyCode() == 39) { // Right
                    x_inc = 1.0 / ppiX / 12.0;
                } else if (e.getExtendedKeyCode() == 37) { // Left
                    x_inc = -1.0 / ppiX / 12.0;
                } else if (e.getExtendedKeyCode() == 40) { // Down
                    y_inc = -1.0 / ppiY / 12.0;
                }

                Path temp = moveFlag.getPathName().equals("current") ? currentPath : paths.get(moveFlag.getPathName());

                temp.clickPoints.get(moveFlag.getPointIndex()).incrementPosition(x_inc, y_inc);
                genPath(temp);

                fig.repaint();
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
         * at least one point in the current user session, it prompts the user if they want to save their point(s)/path(s). If the user says
         * no, the program exits, if the user cancels the program continues, and if the user says yes then the save() function is called. If
         * the current user session has no visible points on the field, the program exits.
         *
         * @param e the WindowEvent (unused) which is generated, in this case, when the X button is clicked.
         */
        @Override
        public void windowClosing(WindowEvent e) {
            if (!paths.isEmpty() || currentPath.isNotEmpty()) {
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
            if (!moveFlag.equals(PointMarker.DEFAULT)) {
                double[] point;

                if ((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
                    Path temp = moveFlag.getPathName().equals("current") ? currentPath : paths.get(moveFlag.getPathName());

                    temp.clickPoints.get(moveFlag.getPointIndex()).setPosition(point[0], point[1]);
                    genPath(temp);

                    fig.repaint();
                } else {
                    displayBoundaryWarning();
                }
            }
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
         * @param e the MouseEvent generated when the mouse is clicked in the component
         */
        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                addNewWaypoint();
            } else if (SwingUtilities.isRightMouseButton(e)) { // Edit the potentially clicked waypoint in the current path
                if (!moveFlag.equals(PointMarker.DEFAULT)) {
                    System.out.println("Mouse clicked moveFlag: " + moveFlag);

                    Path temp = moveFlag.getPathName().equals("current") ? currentPath : paths.get(moveFlag.getPathName());

                    // Modify the clicked point and then generate the new path accordingly.
                    String valuesToEdit = temp.clickPoints.get(moveFlag.getPointIndex()).getDegVelAcc();
                    double[] values = Utils.editWaypointKinematicValues(valuesToEdit);

                    if (values != null && values.length == 3) {
                        temp.clickPoints.get(moveFlag.getPointIndex()).setDegVelAcc(values[0], values[1], values[2]);
                        genPath(temp);
                    }

                    fig.repaint();
                }
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
                Point p = new Point(point[0], point[1]);
                moveFlag = PointMarker.DEFAULT;

                // Check the current path to see if the clicked point is a part of it, and if it isn't, then check all the other paths.
                if (!findClickedPoint("current", currentPath, p)) {
                    for (Map.Entry<String, Path> en : paths.entrySet()) {
                        if (findClickedPoint(en.getKey(), en.getValue(), p)) {
                            break;
                        }
                    }
                }

                System.out.println("Mouse pressed moveFlag: " + moveFlag);
            } else {
                displayBoundaryWarning();
            }
        }

        /**
         * This function actually does the searching for the clicked point. If it finds the point, it sets the moveFlag array accordingly
         *
         * @param pathName the name of the path to search through
         * @param path     the actual path to search through
         * @param p        the cursor point
         *
         * @return false if it can't find a corresponding point, true otherwise
         */
        private boolean findClickedPoint(String pathName, Path path, Point p) {
            Point temp = new Point();

            for (int i = 0; i < path.clickPoints.size(); i++) {
                Waypoint po = path.clickPoints.get(i);

                for (int k = -3; k < 4; k++) {
                    temp.x = (int) (po.getX() * xScale) + k;

                    for (int l = -3; l < 4; l++) {
                        temp.y = (int) (po.getY() * yScale) + l;

                        if (p.equals(temp)) {
                            moveFlag = new PointMarker(pathName, i);
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
        private void addNewWaypoint() {
            // Get the mouse position (x, y) on the window, constrain it to the field borders and convert it to feet.
            double[] point;

            if ((point = getCursorFeet(g.getRootPane().getMousePosition())) != null) {
                // Add the new point to the end of the current Path and then generate a new path accordingly.
                double[] values = Utils.getWaypointKinematicValues();

                if (values != null && values.length == 3) {
                    currentPath.clickPoints.add(new Waypoint(point[0], point[1], values[0], values[1], values[2]));
                    genPath(currentPath);

                    // Every time a new point is added, clear the redo buffer
                    redoBuffer.clear();
                }

                fig.repaint();
            } else {
                displayBoundaryWarning();
            }
        }
    }
}
