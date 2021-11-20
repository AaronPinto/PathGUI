/**
 * A path segment is clicked, and we need to keep track of that for the different keyboard shortcuts. It also needs to store the points of a
 * path segment and those waypoints as well.
 */
public class Path {
    BetterArrayList<Waypoint> pathSegPoints, clickPoints, leftPSPoints, rightPSPoints;

    /**
     * Constructor for a Path
     */
    Path() {
        this.pathSegPoints = new BetterArrayList<>();
        this.clickPoints = new BetterArrayList<>(0); // The 0 is intentional
        this.leftPSPoints = new BetterArrayList<>();
        this.rightPSPoints = new BetterArrayList<>();
    }

    boolean isEmpty() {
        return this.pathSegPoints.isEmpty() && this.clickPoints.isEmpty();
    }

    void clear() {
        this.pathSegPoints.clear();
        this.clickPoints.clear();
        this.leftPSPoints.clear();
        this.rightPSPoints.clear();
    }
}
