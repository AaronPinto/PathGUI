package util;

/**
 * A path stores the points of a path, the waypoints used to create that path, and the left and right path points as well.
 */
public final class Path {
    public BetterArrayList<Waypoint> pathPoints, clickPoints, leftPoints, rightPoints;

    /**
     * Constructor for a Path
     */
    public Path() {
        this.pathPoints = new BetterArrayList<>();
        this.clickPoints = new BetterArrayList<>();
        this.leftPoints = new BetterArrayList<>();
        this.rightPoints = new BetterArrayList<>();
    }

    public boolean isEmpty() {
        return this.pathPoints.isEmpty() && this.clickPoints.isEmpty();
    }

    public void clear() {
        this.pathPoints.clear();
        this.clickPoints.clear();
        this.leftPoints.clear();
        this.rightPoints.clear();
    }
}
