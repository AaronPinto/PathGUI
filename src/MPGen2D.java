import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/QuinticPolynomialsPlanner/quinticPolynomialsPlanner.py
 * Modified by: Aaron Pinto
 */
public class MPGen2D {
    private static final double MIN_T = 0.1, MAX_T = 10.0, dt = 0.01; // Seconds
    private static final double max_accel = 8.0; // ft/s^2
    private static final double max_jerk = 30.0; // ft/s^3

    ArrayList<ArrayList<Double>> results = new ArrayList<>(
            Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
                    new ArrayList<>()));

    MPGen2D(Waypoint[] waypoints) {
        int timeSpliceIndex = 0;

        for (int i = 0; i < waypoints.length - 1; i++) {
            List<ArrayList<Double>> temp = quinticPolynomialsPlanner(waypoints[i].x, waypoints[i].y, waypoints[i].yaw, waypoints[i].v,
                    waypoints[i].a, waypoints[i + 1].x, waypoints[i + 1].y, waypoints[i + 1].yaw, waypoints[i + 1].v, waypoints[i + 1].a);

            // TODO: improve this, it shouldn't just be the removal of a point
            if (i >= 1) {
                for (ArrayList<Double> doubles : temp) {
                    doubles.remove(0);
                }
            }

            for (int j = 0; j < results.size(); j++) {
                results.get(j).addAll(temp.get(j));
            }

            // We don't want each segment between 2 points to start with t = 0.0, make the time continuous
            if (results.get(0).size() > timeSpliceIndex && timeSpliceIndex != 0) {
                for (int j = timeSpliceIndex; j < results.get(0).size(); j++) {
                    results.get(0).set(j, results.get(0).get(j) + results.get(0).get(timeSpliceIndex - 1));
                }
            }

            timeSpliceIndex += temp.get(0).size();
        }

        // System.out.printf("%-8s %-17s %-18s %-18s %-18s %-18s %-18s", "Time", "x", "y", "yaw", "vel", "accel", "jerk");
        // System.out.println();
        //
        // for (int i = 0; i < results.get(0).size(); i++) {
        //     System.out.printf("%f %.15f %.16f %.16f %.16f %.16f %.16f\n", results.get(0).get(i), results.get(1).get(i),
        //             results.get(2).get(i), Math.toDegrees(results.get(3).get(i)), results.get(4).get(i), results.get(5).get(i),
        //             results.get(6).get(i));
        // }
    }

    public static void main(String[] args) {
        Waypoint[] waypoints = new Waypoint[]{new Waypoint(0.0, 0.0, 0.0, 0.0, 0.0), new Waypoint(1.5, 0.0, 0.0, 4.73, 7.98),
                new Waypoint(28.5, -15.0, 0.0, 4.73, -7.98), new Waypoint(30.0, -15.0, 0.0, 0.0, 0.0)};

        new MPGen2D(waypoints);
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
    private static List<ArrayList<Double>> quinticPolynomialsPlanner(double s_x, double s_y, double s_yaw, double s_v, double s_a,
            double g_x, double g_y, double g_yaw, double g_v, double g_a) {
        double s_vx = s_v * Math.cos(s_yaw), s_vy = s_v * Math.sin(s_yaw), g_vx = g_v * Math.cos(g_yaw), g_vy = g_v * Math.sin(g_yaw);
        double s_ax = s_a * Math.cos(s_yaw), s_ay = s_a * Math.sin(s_yaw), g_ax = g_a * Math.cos(g_yaw), g_ay = g_a * Math.sin(g_yaw);
        ArrayList<Double> time = new ArrayList<>(), rx = new ArrayList<>(), ry = new ArrayList<>(), r_yaw = new ArrayList<>(), rv =
                new ArrayList<>(), ra = new ArrayList<>(), rj = new ArrayList<>();

        double T;
        for (T = MIN_T; T < MAX_T; T = new BigDecimal(T + MIN_T).setScale(3, RoundingMode.HALF_UP).doubleValue()) {
            QuinticPolynomial xqp = new QuinticPolynomial(s_x, s_vx, s_ax, g_x, g_vx, g_ax, T);
            QuinticPolynomial yqp = new QuinticPolynomial(s_y, s_vy, s_ay, g_y, g_vy, g_ay, T);

            time.clear();
            rx.clear();
            ry.clear();
            r_yaw.clear();
            rv.clear();
            ra.clear();
            rj.clear();

            double t;
            for (t = 0.0; t < T + dt; t = new BigDecimal(t + dt).setScale(3, RoundingMode.HALF_UP).doubleValue()) {
                time.add(t);
                rx.add(xqp.calcPoint(t));
                ry.add(yqp.calcPoint(t));

                double vx = xqp.calcFirstDeriv(t), vy = yqp.calcFirstDeriv(t);
                double v = Math.hypot(vx, vy);
                double yaw = Math.atan2(vy, vx);
                rv.add(v);
                r_yaw.add(yaw);

                ra.add(derivativeMag(rv, xqp.calcSecondDeriv(t), yqp.calcSecondDeriv(t)));
                rj.add(derivativeMag(ra, xqp.calcThirdDeriv(t), yqp.calcThirdDeriv(t)));
            }

            ArrayList<Double> absRa = new ArrayList<>(ra.size()), absRj = new ArrayList<>(rj.size());
            for (int i = 0; i < ra.size(); i++) {
                absRa.add(i, Math.abs(ra.get(i)));
                absRj.add(i, Math.abs(rj.get(i)));
            }

            double max_acc = Collections.max(absRa);
            if (max_acc <= max_accel) {
                if (Collections.max(absRj) > max_jerk) {
                    System.out.println("max jerk violated!");
                } else {
                    System.out.println("found valid path");
                    xqp.printCoeffs();
                    yqp.printCoeffs();
                    System.out.printf("max accel=%f, T=%f!\n", max_acc, T);
                    break;
                }
            }
        }

        return Arrays.asList(time, rx, ry, r_yaw, rv, ra, rj);
    }

    /*
    Calculate the magnitude of the corresponding derivative, with the proper sign
    + = increasing, - = decreasing
     */
    private static double derivativeMag(ArrayList<Double> parent, double dx, double dy) {
        double mag = Math.hypot(dx, dy);
        // If (last - 2nd last) < 0, invert the sign
        if (parent.size() >= 2 && parent.get(parent.size() - 1) - parent.get(parent.size() - 2) < 0.0) {
            mag *= -1;
        }

        return mag;
    }
}
