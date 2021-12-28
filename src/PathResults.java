import util.BetterArrayList;

import java.util.Map;

import static java.util.Map.entry;

public final class PathResults {
    final BetterArrayList<Double> rad; // angle in radians
    final BetterArrayList<Double> deg; // angle in degrees
    final BetterArrayList<Double> time, x, y, vel, acc, jerk, roc, distance, omega;
    private final Map<Integer, BetterArrayList<Double>> indexMap;

    PathResults() {
        this.time = new BetterArrayList<>();
        this.x = new BetterArrayList<>();
        this.y = new BetterArrayList<>();
        this.rad = new BetterArrayList<>();
        this.deg = new BetterArrayList<>();
        this.vel = new BetterArrayList<>();
        this.acc = new BetterArrayList<>();
        this.jerk = new BetterArrayList<>();
        this.roc = new BetterArrayList<>();
        this.distance = new BetterArrayList<>();
        this.omega = new BetterArrayList<>();

        this.indexMap = Map.ofEntries(entry(0, this.time), entry(1, this.x), entry(2, this.y), entry(3, this.rad), entry(4, this.vel),
                entry(5, this.acc), entry(6, this.jerk), entry(7, this.roc), entry(8, this.omega));
    }

    public BetterArrayList<Double> getVel() {
        return vel;
    }

    public BetterArrayList<Double> getRoc() {
        return roc;
    }

    public BetterArrayList<Double> getDistance() {
        return this.distance;
    }

    public BetterArrayList<Double> getDeg() {
        return this.deg;
    }

    public BetterArrayList<Double> getRad() {
        return this.rad;
    }

    BetterArrayList<Double> get(int index) {
        return this.indexMap.get(index);
    }

    void clear() {
        for (var list : this.indexMap.values()) {
            list.clear();
        }
    }

    int size() {
        return this.indexMap.size();
    }
}
