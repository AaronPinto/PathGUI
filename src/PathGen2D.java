import util.BetterArrayList;
import util.Waypoint;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;

/**
 * @author https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/QuinticPolynomialsPlanner/quintic_polynomials_planner.py
 * Modified by: Aaron Pinto
 */
public final class PathGen2D {
    private static final double MIN_T = 0.1, MAX_T = 10.0, POLY_D_T = 0.01, POINT_D_T = 0.01; // Seconds
    private static final double max_accel = 8.0; // ft/s^2
    public final PathResults results;

    public PathGen2D(Waypoint[] waypoints) {
        this.results = new PathResults();
        int timeSpliceIndex = 0;

        for (int i = 0; i < waypoints.length - 1; i++) {
            PathResults temp = quinticPolyPlanner(waypoints[i].getX(), waypoints[i].getY(), waypoints[i].getRad(), waypoints[i].getV(),
                    waypoints[i].getA(), waypoints[i + 1].getX(), waypoints[i + 1].getY(), waypoints[i + 1].getRad(),
                    waypoints[i + 1].getV(), waypoints[i + 1].getA());

            // TODO: improve this, it shouldn't just be the removal of a point every waypoint
            if (i >= 1) {
                // noinspection ListRemoveInLoop
                for (int j = 0; j < temp.size(); j++) {
                    temp.get(j).remove(0);
                }
            }

            for (int j = 0; j < temp.size(); j++) {
                results.get(j).addAll(temp.get(j));
            }

            // We don't want each segment between 2 points to start with t = 0.0, make the time continuous
            if (results.time.size() > timeSpliceIndex && timeSpliceIndex != 0) {
                for (int j = timeSpliceIndex; j < results.time.size(); j++) {
                    results.time.set(j, results.time.get(j) + results.time.get(timeSpliceIndex - 1));
                }
            }

            timeSpliceIndex += temp.get(0).size();
        }

        for (Double r : results.getRad()) {
            results.deg.add(Math.toDegrees(r));
        }

        if (waypoints.length > 1) {
            // Fix heading on last point
            results.deg.setLast(waypoints[waypoints.length - 1].getDeg());
            results.rad.setLast(waypoints[waypoints.length - 1].getRad());

            results.distance.add(0.0);
            for (int i = 1; i < results.x.size(); i++) {
                double xDelta = results.x.get(i) - results.x.get(i - 1);
                double yDelta = results.y.get(i) - results.y.get(i - 1);
                results.distance.add(Math.hypot(xDelta, yDelta) + results.distance.get(i - 1));
            }
        }

        // printResults();
    }

    public static void main(String[] args) {
        Waypoint[] waypoints1 = new Waypoint[]{new Waypoint(0.0, 0.0, 0.0, 0.0, 0.0), new Waypoint(1.5, 0.0, 0.0, 5.7, 12.0),
                new Waypoint(28.5, -15.0, 0.0, 5.7, -12.0), new Waypoint(30.0, -15.0, 0.0, 0.0, 0.0)};

        Waypoint[] waypoints2 = new Waypoint[]{new Waypoint(0.0, 23.14, 0.0, 0.0, 12.0),
                new Waypoint(19.31, 16.55, Math.toRadians(-90.0), 12.0, 12.0),
                new Waypoint(19.31, 11.03, Math.toRadians(-90.0), 9.0, -12.0), new Waypoint(24.13, 6.35, Math.toRadians(20.0), 0.0, -12.0)};

        Waypoint[] waypoints3 = new Waypoint[]{new Waypoint(0.0, 0.0, 0.0, 0.0, 12.0), new Waypoint(4.0, 4.0, 0.0, 0.0, -12.0)};

        Waypoint[] waypoints4 = new Waypoint[]{new Waypoint(1.0, 24.0, 0.0, 0.0, 12.0), new Waypoint(2.0, 24.0, 0.0, 4.4, 12.0),
                new Waypoint(7.0, 19.0, Math.toRadians(-90.0), 4.4, -12.0), new Waypoint(7.0, 18.0, Math.toRadians(-90.0), 0.0, -12.0)};

        Waypoint[] waypoints5 = new Waypoint[]{new Waypoint(0.0, 0.0, 0.0, 0.0, 8.0), new Waypoint(1.0, 0.0, 0.0, 4.0, 8.0)};

        PathGen2D test = new PathGen2D(waypoints5);
        var l_r = test.leftRight(1.744792);
        test.printResults();

        // for (int i = 0; i < l_r.get(0).size(); i++) {
        // System.out.printf("%16.12f %16.12f %.12f\n", l_r.get(0).get(i).getV(), l_r.get(1).get(i).getV(), test.results.roc.get(i));
        // }
    }

    /**
     * @param s_x   start x
     * @param s_y   start y
     * @param s_yaw start yaw
     * @param s_v   start velocity
     * @param s_a   start acceleration
     * @param g_x   goal x
     * @param g_y   goal y
     * @param g_yaw goal yaw
     * @param g_v   goal velocity
     * @param g_a   goal acceleration
     *
     * @return a list of points of the quintic spline between the start and goal points. if the spline could not be found, the list will be
     * empty
     */
    private static PathResults quinticPolyPlanner(double s_x, double s_y, double s_yaw, double s_v, double s_a, double g_x, double g_y,
            double g_yaw, double g_v, double g_a) {
        // Setup x and y components of magnitudes
        double s_vx = s_v * Math.cos(s_yaw), s_vy = s_v * Math.sin(s_yaw), g_vx = g_v * Math.cos(g_yaw), g_vy = g_v * Math.sin(g_yaw);
        double s_ax = s_a * Math.cos(s_yaw), s_ay = s_a * Math.sin(s_yaw), g_ax = g_a * Math.cos(g_yaw), g_ay = g_a * Math.sin(g_yaw);

        PathResults results = new PathResults();

        // TODO: check that the velocity doesn't saturate going around a corner, left and right wheel speeds
        for (double T = MIN_T; T <= MAX_T; T = new BigDecimal(T + POLY_D_T).setScale(4, RoundingMode.HALF_UP).doubleValue()) {
            QuinticPolynomial xqp = new QuinticPolynomial(s_x, s_vx, s_ax, g_x, g_vx, g_ax, T);
            QuinticPolynomial yqp = new QuinticPolynomial(s_y, s_vy, s_ay, g_y, g_vy, g_ay, T);

            results.clear();

            for (double t = 0.0; t < T + POLY_D_T; t = new BigDecimal(t + POINT_D_T).setScale(3, RoundingMode.HALF_UP).doubleValue()) {
                results.time.add(t);
                results.x.add(xqp.calcPoint(t));
                results.y.add(yqp.calcPoint(t));

                double vx = xqp.calcFirstDeriv(t), vy = yqp.calcFirstDeriv(t);
                double v = Math.hypot(vx, vy);
                double yaw = Math.atan2(vy, vx);
                results.vel.add(v);
                results.rad.add(yaw);

                results.acc.add(alignedDerivative(yaw, xqp.calcSecondDeriv(t), yqp.calcSecondDeriv(t)));
                results.jerk.add(alignedDerivative(yaw, xqp.calcThirdDeriv(t), yqp.calcThirdDeriv(t)));

                double roc = calcParametricRadOfCurve(xqp, yqp, t);
                results.roc.add(roc);

                results.omega.add(v / roc);
            }

            ArrayList<Double> absRa = new ArrayList<>(results.acc.size());
            for (int i = 0; i < results.acc.size(); i++) {
                absRa.add(i, Math.abs(results.acc.get(i)));
            }

            double max_acc = Collections.max(absRa);
            if (max_acc <= max_accel) {
                System.out.printf("found valid path: max accel=%f, T=%f!\n", max_acc, T);
                // xqp.printCoeffs();
                // yqp.printCoeffs();
                break;
            } /*else {
                System.out.printf("Time=%f\n", T);
                xqp.printCoeffs();
            }*/
        }

        return results;
    }

    /**
     * Calculate the magnitude of the corresponding derivative in the same direction as velocity, with the proper sign + = increasing, - =
     * decreasing
     */
    private static double alignedDerivative(double alignYaw, double dx, double dy) {
        double cosDelta = Math.cos(Math.atan2(dy, dx) - alignYaw);

        return Math.hypot(dx, dy) * cosDelta;
    }

    /**
     * Positive = turning left, negative = turning right, INFINITY = straight
     */
    private static double calcParametricRadOfCurve(QuinticPolynomial xqp, QuinticPolynomial yqp, double t) {
        double x_prime = xqp.calcFirstDeriv(t), y_prime = yqp.calcFirstDeriv(t);
        double x_double_prime = xqp.calcSecondDeriv(t), y_double_prime = yqp.calcSecondDeriv(t);

        double denominator = x_prime * y_double_prime - y_prime * x_double_prime;

        if (Utils.absLessThanEps(denominator)) {
            return Double.POSITIVE_INFINITY;
        }

        return Math.pow(x_prime * x_prime + y_prime * y_prime, 1.5) / denominator;
    }

    private void printResults() {
        System.out.printf("%-9s %-17s %-17s %-16s %-17s %-18s %-19s %-16s %-16s\n", "Time", "x", "y", "angle", "vel", "accel", "jerk",
                "roc", "omega");

        for (int i = 0; i < results.time.size(); i++) {
            System.out.printf("%f %15.12f %17.12f %17.12f %16.12f %17.12f %18.12f %19.12f %16.12f\n", results.time.get(i), results.x.get(i),
                    results.y.get(i), results.getDeg().get(i), results.vel.get(i), results.acc.get(i), results.jerk.get(i),
                    results.roc.get(i), results.omega.get(i));
        }
    }

    /**
     * Generate the left and right paths and velocities and return them
     *
     * @param robotTrkWidth the robot track width
     *
     * @return A BetterArrayList of the left and right paths (Stored in BetterArrayLists also) for the robot
     */
    public BetterArrayList<BetterArrayList<Waypoint>> leftRight(double robotTrkWidth) {
        BetterArrayList<BetterArrayList<Waypoint>> res = new BetterArrayList<>();
        res.add(new BetterArrayList<>(results.time.size())); // Left
        res.add(new BetterArrayList<>(results.time.size())); // Right

        if (results.time.size() > 1) {
            // Point value calculation, res.get(0) and res.get(1) are the left and right paths respectively
            // Pi / 2 rad = 90 degrees
            // leftX = trackWidth / 2 * cos(calculatedAngleAtThatIndex + Pi / 2) + centerPathXValueAtThatIndex
            // leftY = trackWidth / 2 * sin(calculatedAngleAtThatIndex + Pi / 2) + centerPathYValueAtThatIndex
            // rightX = trackWidth / 2 * cos(calculatedAngleAtThatIndex - Pi / 2) + centerPathXValueAtThatIndex
            // rightY = trackWidth / 2 * sin(calculatedAngleAtThatIndex - Pi / 2) + centerPathYValueAtThatIndex
            for (int i = 0; i < results.time.size(); i++) {
                Waypoint point = new Waypoint(results.x.get(i), results.y.get(i), results.rad.get(i), results.vel.get(i),
                        results.acc.get(i));
                Waypoint leftPoint = new Waypoint(robotTrkWidth / 2 * Math.cos(point.getRad() + Math.PI / 2) + point.getX(),
                        robotTrkWidth / 2 * Math.sin(point.getRad() + Math.PI / 2) + point.getY(), point);
                Waypoint rightPoint = new Waypoint(robotTrkWidth / 2 * Math.cos(point.getRad() - Math.PI / 2) + point.getX(),
                        robotTrkWidth / 2 * Math.sin(point.getRad() - Math.PI / 2) + point.getY(), point);

                // Calculate left and right velocities
                leftPoint.setV(point.getV() - results.omega.get(i) * robotTrkWidth / 2.0);
                rightPoint.setV(point.getV() + results.omega.get(i) * robotTrkWidth / 2.0);

                res.get(0).add(i, leftPoint);
                res.get(1).add(i, rightPoint);
            }
        } else if (results.time.size() == 1) {
            res.get(0).add(0, new Waypoint(results.x.get(0), results.y.get(0), results.rad.get(0), results.vel.get(0), results.acc.get(0)));
        }

        return res;
    }
}
