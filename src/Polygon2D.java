import java.awt.*;
import java.awt.geom.*;
import java.io.Serializable;
import java.util.stream.IntStream;

/**
 * This class is a Polygon with double coordinates.
 */
public class Polygon2D implements Shape, Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	String name = "";
	/**
	 * The total number of points.  The value of <code>npoints</code>
	 * represents the number of valid points in this <code>Polygon</code>.
	 */
	private int npoints;
	/**
	 * The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the
	 * number of points in this <code>Polygon2D</code>.
	 */
	private double[] xpoints;
	/**
	 * The array of <i>x</i> coordinates. The value of {@link #npoints npoints} is equal to the
	 * number of points in this <code>Polygon2D</code>.
	 */
	private double[] ypoints;
	/**
	 * Bounds of the Polygon2D.
	 *
	 * @see #getBounds()
	 */
	private Rectangle2D bounds;
	private Path2D path;
	private Path2D closedPath;

	/**
	 * Creates an empty Polygon2D.
	 */
	private Polygon2D() {
		xpoints = new double[4];
		ypoints = new double[4];
	}

	/**
	 * Constructs and initializes a <code>Polygon2D</code> from the specified
	 * parameters.
	 *
	 * @param xpoints an array of <i>x</i> coordinates
	 * @param ypoints an array of <i>y</i> coordinates
	 * @param npoints the total number of points in the <code>Polygon2D</code>
	 * @throws NegativeArraySizeException if the value of
	 *                                    <code>npoints</code> is negative.
	 * @throws IndexOutOfBoundsException  if <code>npoints</code> is
	 *                                    greater than the length of <code>xpoints</code>
	 *                                    or the length of <code>ypoints</code>.
	 * @throws NullPointerException       if <code>xpoints</code> or
	 *                                    <code>ypoints</code> is <code>null</code>.
	 */
	Polygon2D(double[] xpoints, double[] ypoints, int npoints, String name) {
		assert npoints <= xpoints.length && npoints <= ypoints.length : "npoints > xpoints.length || npoints > ypoints.length";
		this.name = name;
		this.npoints = npoints;
		this.xpoints = new double[npoints];
		this.ypoints = new double[npoints];
		System.arraycopy(xpoints, 0, this.xpoints, 0, npoints);
		System.arraycopy(ypoints, 0, this.ypoints, 0, npoints);
		calculatePath();
	}

	protected Object clone() {
		Polygon2D pol = new Polygon2D();
		IntStream.range(0, npoints).forEach(i -> pol.addPoint(xpoints[i], ypoints[i]));
		return pol;
	}

	/**
	 * This function calculates the best path between each point in this <code>Polygon2D</code>
	 */
	private void calculatePath() {
		path = new Path2D.Double();
		path.moveTo(xpoints[0], ypoints[0]);
		IntStream.range(1, npoints).forEach(i -> path.lineTo(xpoints[i], ypoints[i]));
		bounds = path.getBounds2D();
		path.closePath();
		closedPath = null;
	}

	/**
	 * This function updates the path based on the new point that was added to this <code>Polygon2D</code>
	 *
	 * @param x the x-value of the point that was added
	 * @param y the y-value of the point that was added
	 */
	private void updatePath(double x, double y) {
		closedPath = null;
		if(path == null) {
			path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
			path.moveTo(x, y);
			bounds = new Rectangle2D.Double(x, y, 0, 0);
		} else {
			path.lineTo(x, y);
			double xmax = bounds.getMaxX();
			double ymax = bounds.getMaxY();
			double xmin = bounds.getMinX();
			double ymin = bounds.getMinY();
			if(x < xmin) xmin = x;
			else if(x > xmax) xmax = x;
			if(y < ymin) ymin = y;
			else if(y > ymax) ymax = y;
			bounds = new Rectangle2D.Double(xmin, ymin, xmax - xmin, ymax - ymin);
		}
	}

	/**
	 * Appends the specified coordinates to this <code>Polygon2D</code>.
	 *
	 * @param x the specified x coordinate
	 * @param y the specified y coordinate
	 */
	private void addPoint(double x, double y) {
		if(npoints == xpoints.length) {
			double[] tmp;

			tmp = new double[npoints * 2];
			System.arraycopy(xpoints, 0, tmp, 0, npoints);
			xpoints = tmp;

			tmp = new double[npoints * 2];
			System.arraycopy(ypoints, 0, tmp, 0, npoints);
			ypoints = tmp;
		}
		xpoints[npoints] = x;
		ypoints[npoints] = y;
		npoints++;
		updatePath(x, y);
	}

	/**
	 * Returns the high precision bounding box of the {@link Shape}.
	 *
	 * @return a {@link Rectangle2D} that precisely
	 * bounds the <code>Shape</code>.
	 */
	public Rectangle2D getBounds2D() {
		return bounds;
	}

	public Rectangle getBounds() {
		return bounds == null ? null : bounds.getBounds();
	}

	/**
	 * Determines if the specified coordinates are inside this
	 * <code>Polygon</code>.  For the definition of
	 * <i>insideness</i>, see the class comments of {@link Shape}.
	 *
	 * @param x the specified x coordinate
	 * @param y the specified y coordinate
	 * @return <code>true</code> if the <code>Polygon</code> contains the
	 * specified coordinates; <code>false</code> otherwise.
	 */
	public boolean contains(double x, double y) {
		if(npoints <= 2 || !bounds.contains(x, y)) return false;
		updateComputingPath();
		return closedPath.contains(x, y);
	}

	/**
	 * Updates the closed path so that functions that rely on a closed path can work properly
	 */
	private void updateComputingPath() {
		if(npoints >= 1 && closedPath == null) {
			closedPath = (Path2D) path.clone();
			closedPath.closePath();
		}
	}

	/**
	 * Tests if a specified {@link Point2D} is inside the boundary of this
	 * <code>Polygon</code>.
	 *
	 * @param p a specified <code>Point2D</code>
	 * @return <code>true</code> if this <code>Polygon</code> contains the
	 * specified <code>Point2D</code>; <code>false</code>
	 * otherwise.
	 * @see #contains(double, double)
	 */
	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}

	/**
	 * Tests if the interior of this <code>Polygon</code> intersects the
	 * interior of a specified set of rectangular coordinates.
	 *
	 * @param x the x coordinate of the specified rectangular
	 *          shape's top-left corner
	 * @param y the y coordinate of the specified rectangular
	 *          shape's top-left corner
	 * @param w the width of the specified rectangular shape
	 * @param h the height of the specified rectangular shape
	 * @return <code>true</code> if the interior of this
	 * <code>Polygon</code> and the interior of the
	 * specified set of rectangular
	 * coordinates intersect each other;
	 * <code>false</code> otherwise.
	 */
	public boolean intersects(double x, double y, double w, double h) {
		if(npoints <= 0 || !bounds.intersects(x, y, w, h)) return false;
		updateComputingPath();
		return closedPath.intersects(x, y, w, h);
	}

	/**
	 * Checks if the last element in the array is the same as the first element.
	 * Used for creating all point-to-point lines for a polygon.
	 *
	 * @param d the array to check
	 * @return the inputted array if its closed, otherwise a new array which is closed
	 */
	private double[] checkIfClosed(double[] d) {
		if(d[0] == d[d.length - 1]) return d;
		else {
			double[] temp = new double[d.length + 1];
			System.arraycopy(d, 0, temp, 0, d.length);
			temp[temp.length - 1] = temp[0];
			return temp;
		}
	}


	/**
	 * Checks if the line made by the 2 input points intersect with any of the lines made by the points of this polygon
	 *
	 * @param x the x-coordinate of the current point
	 * @param y the y-coordinate of the current point
	 * @param p the next point, to create a line to
	 * @return <code>true</code> if the lines intersect <code>false</code> otherwise.
	 */
	boolean intersects(double x, double y, PathGUITool.Waypoint p) {
		if(p != null) {
			double[] xs = checkIfClosed(xpoints), ys = checkIfClosed(ypoints);
			Line2D line = new Line2D.Double(x, y, p.x, p.y);
			return IntStream.range(0, xs.length - 2).mapToObj(i -> new Line2D.Double(xs[i], ys[i], xs[i + 1], ys[i + 1])).
					anyMatch(line::intersectsLine);
		}
		return false;
	}

	/**
	 * Checks if a point is on the border or outside this polygon
	 *
	 * @param x the x-coordinate of the current point
	 * @param y the y-coordinate of the current point
	 * @return <code>true</code> if the lines intersect <code>false</code> otherwise.
	 */
	boolean out(double x, double y) {
		return x <= this.bounds.getX() || x >= this.bounds.getMaxX() || y <= this.bounds.getY() || y >= this.bounds.getMaxY();
	}

	/**
	 * Tests if the interior of this <code>Polygon</code> intersects the
	 * interior of a specified <code>Rectangle2D</code>.
	 *
	 * @param r a specified <code>Rectangle2D</code>
	 * @return <code>true</code> if this <code>Polygon</code> and the
	 * interior of the specified <code>Rectangle2D</code>
	 * intersect each other; <code>false</code>
	 * otherwise.
	 */
	public boolean intersects(Rectangle2D r) {
		return intersects(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Tests if the interior of this <code>Polygon</code> entirely
	 * contains the specified set of rectangular coordinates.
	 *
	 * @param x the x coordinate of the top-left corner of the
	 *          specified set of rectangular coordinates
	 * @param y the y coordinate of the top-left corner of the
	 *          specified set of rectangular coordinates
	 * @param w the width of the set of rectangular coordinates
	 * @param h the height of the set of rectangular coordinates
	 * @return <code>true</code> if this <code>Polygon</code> entirely
	 * contains the specified set of rectangular
	 * coordinates; <code>false</code> otherwise.
	 */
	public boolean contains(double x, double y, double w, double h) {
		if(npoints <= 0 || !bounds.intersects(x, y, w, h)) return false;
		updateComputingPath();
		return closedPath.contains(x, y, w, h);
	}

	/**
	 * Tests if the interior of this <code>Polygon</code> entirely
	 * contains the specified <code>Rectangle2D</code>.
	 *
	 * @param r the specified <code>Rectangle2D</code>
	 * @return <code>true</code> if this <code>Polygon</code> entirely
	 * contains the specified <code>Rectangle2D</code>;
	 * <code>false</code> otherwise.
	 * @see #contains(double, double, double, double)
	 */
	public boolean contains(Rectangle2D r) {
		return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
	}

	/**
	 * Returns an iterator object that iterates along the boundary of this
	 * <code>Polygon</code> and provides access to the geometry
	 * of the outline of this <code>Polygon</code>.  An optional
	 * {@link AffineTransform} can be specified so that the coordinates
	 * returned in the iteration are transformed accordingly.
	 *
	 * @param at an optional <code>AffineTransform</code> to be applied to the
	 *           coordinates as they are returned in the iteration, or
	 *           <code>null</code> if untransformed coordinates are desired
	 * @return a {@link PathIterator} object that provides access to the
	 * geometry of this <code>Polygon</code>.
	 */
	public PathIterator getPathIterator(AffineTransform at) {
		updateComputingPath();
		return closedPath == null ? null : closedPath.getPathIterator(at);
	}

	/**
	 * Returns an iterator object that iterates along the boundary of
	 * the <code>Polygon2D</code> and provides access to the geometry of the
	 * outline of the <code>Shape</code>.  Only SEG_MOVETO, SEG_LINETO, and
	 * SEG_CLOSE point types are returned by the iterator.
	 * Since polygons are already flat, the <code>flatness</code> parameter
	 * is ignored.
	 *
	 * @param at       an optional <code>AffineTransform</code> to be applied to the
	 *                 coordinates as they are returned in the iteration, or
	 *                 <code>null</code> if untransformed coordinates are desired
	 * @param flatness the maximum amount that the control points
	 *                 for a given curve can vary from co-linear before a subdivided
	 *                 curve is replaced by a straight line connecting the
	 *                 endpoints.  Since polygons are already flat the
	 *                 <code>flatness</code> parameter is ignored.
	 * @return a <code>PathIterator</code> object that provides access to the
	 * <code>Shape</code> object's geometry.
	 */
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		return getPathIterator(at);
	}
}