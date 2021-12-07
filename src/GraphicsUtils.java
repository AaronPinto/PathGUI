import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

public final class GraphicsUtils {
    /**
     * Just so you don't get confused, this function draws the numbers and ticks along the y-axis and the horizontal light gray lines.
     * <a href="http://www.purplemath.com/modules/distform.htm">Distance Formula</a>
     *
     * @param g2    The Graphics2D object for this window
     * @param yaxis The line that represents the y-axis
     * @param max   The width of the field in feet
     */
    static void drawYTickRange(PathGUITool p, Graphics2D g2, Line2D yaxis, double max) {
        double upperY_tic = Math.ceil(max);

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
            FontMetrics fm = p.getFontMetrics(p.getFont());
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
                g2.draw(new Line2D.Double(30, newY, p.getWidth(), newY));

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
     * @param g2     The Graphics2D object for this window
     * @param x_axis The line that represents the x-axis
     * @param max    The width of the field in feet
     */
    static void drawXTickRange(PathGUITool p, Graphics2D g2, Line2D x_axis, double yTickYMax, double yTickYMin, double max, int height) {
        double upperX_tic = Math.ceil(max);

        // The starting and ending x and y values for the x-axis
        double x0 = x_axis.getX1();
        double y0 = x_axis.getY1();
        double xf = x_axis.getX2();
        double yf = x_axis.getY2();

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
            FontMetrics fm = p.getFontMetrics(p.getFont());
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

    /**
     * This is the function that plots everything in a path, so clickPoints (if applicable), pathSegPoints, leftPoints, and rightPoints. It
     * draws a circle of radius 2 pixels around each non-clicked point and a circle of radius 3 around each clicked point. This is why a
     * path may appear to be crossing a field element, when in reality it is not because the edge of the robot ends at the value of the
     * point. This function also draws a line between each point to show that they are all connected in the same path.
     *
     * @param g2   the 2D graphics object used to draw everything
     * @param path the path to draw/plot
     */
    static void plotPath(Graphics2D g2, Path path, double xScale, double yScale, int height) {
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
    static void plot(Graphics2D g2, Path currentPath, LinkedHashMap<String, Path> paths, double xScale, double yScale, int height) {
        Color tempC = g2.getColor();

        // Plot the current path then loop through paths and plot each
        plotPath(g2, currentPath, xScale, yScale, height);
        paths.forEach((key, value) -> plotPath(g2, value, xScale, yScale, height));
        g2.setColor(tempC);
    }
}
