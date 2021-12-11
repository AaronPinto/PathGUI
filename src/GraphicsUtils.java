import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.text.DecimalFormat;
import java.util.LinkedHashMap;

public final class GraphicsUtils {
    /**
     * This function draws the numbers and ticks along the y-axis and the horizontal light gray lines.
     *
     * @param g2     The Graphics2D object for this window
     * @param y_axis The line that represents the y-axis
     * @param max    The width of the field in feet
     */
    static void drawYTickRange(PathGUITool p, Graphics2D g2, Line2D y_axis, double xTickXMin, double xTickXMax, double max) {
        double upperY_tick = Math.ceil(max);

        // The starting and ending x and y values for the y-axis
        final double x0 = y_axis.getX1();
        double y0 = y_axis.getY1();
        final double xf = y_axis.getX2();
        final double yf = y_axis.getY2();

        // Calculate step-size between ticks and length of Y axis using distance formula
        // Total distance of the axis / number of ticks = distance per tick
        double distance = Math.hypot(xf - x0, yf - y0) / upperY_tick;

        // Iterates through each number from 0 to the max length of the y-axis in feet and draws the horizontal grid lines for each
        // iteration except the last one
        for (int i = 0; i <= upperY_tick; i++) {
            // calculate width of number for proper drawing
            String number = new DecimalFormat("#.#").format(upperY_tick - i);
            FontMetrics fm = p.getFontMetrics(p.getFont());
            int width = fm.stringWidth(number);

            // Draws a tick line and the corresponding number at that value in black
            g2.draw(new Line2D.Double(x0, y0, x0 - 10, y0));
            g2.drawString(number, (float) x0 - 15 - width, (float) y0 + 5);

            // add grid lines to chart
            if (i != upperY_tick) {
                Stroke tempS = g2.getStroke();
                Color tempC = g2.getColor();

                // Sets the color and stroke to light gray dashes and draws them all the way across to the right field edge
                g2.setColor(Color.lightGray);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
                g2.draw(new Line2D.Double(xTickXMin, y0, xTickXMax, y0));

                g2.setColor(tempC);
                g2.setStroke(tempS);
            }
            // Increment the position of the tick
            y0 += distance;
        }
    }

    /**
     * This function draws the numbers and ticks along the x-axis and the vertical light gray lines.
     *
     * @param g2     The Graphics2D object for this window
     * @param x_axis The line that represents the x-axis
     * @param max    The width of the field in feet
     */
    static void drawXTickRange(PathGUITool p, Graphics2D g2, Line2D x_axis, double yTickYMin, double yTickYMax, double max) {
        double upperX_tick = Math.ceil(max);

        // The starting and ending x and y values for the x-axis
        double x0 = x_axis.getX1();
        final double y0 = x_axis.getY1();
        final double xf = x_axis.getX2();
        final double yf = x_axis.getY2();

        // calculate step-size between ticks and length of Y axis using distance formula
        // Total distance of the axis / number of ticks = distance per tick
        double distance = Math.hypot(xf - x0, yf - y0) / upperX_tick;

        // Iterates through each number from 0 to the max length of the x-axis in feet and draws the horizontal grid lines for each
        // iteration except the last one
        for (int i = 0; i <= upperX_tick; i++) {
            // calculate width of number for proper drawing
            String number = new DecimalFormat("#.#").format(i);
            FontMetrics fm = p.getFontMetrics(p.getFont());
            int width = fm.stringWidth(number);

            // Draws a tick line and the corresponding number at that value in black
            g2.draw(new Line2D.Double(x0, yf, x0, yf + 10));
            g2.drawString(number, (float) (x0 - width / 2.0), (float) yf + 25);

            // add grid lines to chart
            if (i != 0) {
                Stroke tempS = g2.getStroke();
                Color tempC = g2.getColor();

                // Sets the color and stroke to light gray dashes and draws them all the way down to the x-axis
                g2.setColor(Color.lightGray);
                g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{5f}, 0f));
                g2.draw(new Line2D.Double(x0, yTickYMin, x0, yTickYMax));

                g2.setColor(tempC);
                g2.setStroke(tempS);
            }
            // Increment the position of the tick
            x0 += distance;
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
    private static void plotPath(Graphics2D g2, Path path, double xOff, double xScale, double yOff, double yScale) {
        // Draw clicked points
        if (path.clickPoints.size() > 1) {
            for (int j = 0; j < path.clickPoints.size() - 1; j++) {
                double x1 = xOff + xScale * path.clickPoints.get(j).getX(), y1 = yOff - yScale * path.clickPoints.get(j).getY();
                double x2 = xOff + xScale * path.clickPoints.get(j + 1).getX(), y2 = yOff - yScale * path.clickPoints.get(j + 1).getY();

                g2.setPaint(Color.magenta);
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
                g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
                g2.fill(new Ellipse2D.Double(x2 - 3, y2 - 3, 6, 6));
            }
        } else if (path.clickPoints.size() == 1) {
            double x1 = xOff + xScale * path.clickPoints.get(0).getX(), y1 = yOff - yScale * path.clickPoints.get(0).getY();

            g2.setPaint(Color.magenta);
            g2.fill(new Ellipse2D.Double(x1 - 3, y1 - 3, 6, 6));
        }

        // Draw all the pathPoints, leftPoints and rightPoints
        if (path.pathPoints.size() > 0) {
            for (int j = 0; j < path.pathPoints.size() - 1; j++) {
                double x1 = xOff + xScale * path.pathPoints.get(j).getX(), y1 = yOff - yScale * path.pathPoints.get(j).getY();
                double x2 = xOff + xScale * path.pathPoints.get(j + 1).getX(), y2 = yOff - yScale * path.pathPoints.get(j + 1).getY();

                g2.setPaint(Color.green);
                g2.draw(new Line2D.Double(x1, y1, x2, y2));
                g2.fill(new Ellipse2D.Double(x1 - 2, y1 - 2, 4, 4));
                g2.fill(new Ellipse2D.Double(x2 - 2, y2 - 2, 4, 4));

                double lx1 = xOff + xScale * path.leftPoints.get(j).getX(), ly1 = yOff - yScale * path.leftPoints.get(j).getY();
                double lx2 = xOff + xScale * path.leftPoints.get(j + 1).getX(), ly2 = yOff - yScale * path.leftPoints.get(j + 1).getY();
                double rx1 = xOff + xScale * path.rightPoints.get(j).getX(), ry1 = yOff - yScale * path.rightPoints.get(j).getY();
                double rx2 = xOff + xScale * path.rightPoints.get(j + 1).getX(), ry2 = yOff - yScale * path.rightPoints.get(j + 1).getY();

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
    }

    /**
     * This function plots all the paths in the current user session. It stores the last color of the Graphics object and then resets the
     * Graphics object to that color after.
     *
     * @param g2 the 2D graphics object used to draw everything
     */
    static void plot(Graphics2D g2, Path currPath, LinkedHashMap<String, Path> paths, double xOff, double xScl, double yOff, double yScl) {
        Color tempC = g2.getColor();

        // Plot the current path then loop through paths and plot each
        plotPath(g2, currPath, xOff, xScl, yOff, yScl);
        paths.forEach((key, value) -> plotPath(g2, value, xOff, xScl, yOff, yScl));
        g2.setColor(tempC);
    }
}
