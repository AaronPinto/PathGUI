package util;

import java.io.Serial;
import java.io.Serializable;

/**
 * A path stores the points of a path, the waypoints used to create that path, and the left and right path points as well.
 */
public final class Path implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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

    public Path(Path other) {
        this.pathPoints = new BetterArrayList<>(other.pathPoints);
        this.clickPoints = new BetterArrayList<>(other.clickPoints);
        this.leftPoints = new BetterArrayList<>(other.leftPoints);
        this.rightPoints = new BetterArrayList<>(other.rightPoints);
    }

    public boolean isNotEmpty() {
        return !this.pathPoints.isEmpty() || !this.clickPoints.isEmpty();
    }

    public void clear() {
        this.pathPoints.clear();
        this.clickPoints.clear();
        this.leftPoints.clear();
        this.rightPoints.clear();
    }

    @Override
    public String toString() {
        return "Path{" + "pathPoints=" + pathPoints + ", clickPoints=" + clickPoints + '}';
    }
}
