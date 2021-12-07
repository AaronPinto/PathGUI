import java.util.ArrayList;
import java.util.Arrays;

public class ReedsShepp {
    private static final PathSegmentResults PSR_FALSE = new PathSegmentResults(false, 0.0, 0.0, 0.0);

    private static class PathSegmentResults {
        boolean valid;
        double t, u, v;

        PathSegmentResults(boolean valid, double t, double u, double v) {
            this.valid = valid;
            this.t = t;
            this.u = u;
            this.v = v;
        }
    }

    private static class Path {
        double[] lengths;
        String[] ctypes;
        ArrayList<Double> x;
        ArrayList<Double> y;
        ArrayList<Double> yaw;
        ArrayList<Integer> directions;

        double L;

        Path() {
            this.lengths = new double[3];
            this.ctypes = new String[3];
            this.x = new ArrayList<>();
            this.y = new ArrayList<>();
            this.yaw = new ArrayList<>();
            this.directions = new ArrayList<>();
            this.L = 0.0;
        }
    }

    double mod2pi(double x) {
        // Be consistent with fmod in C++ here
        double v = x % Math.copySign(2.0 * Math.PI, x); // May not be the same as numpy

        if (v < -Math.PI) {
            v += 2.0 * Math.PI;
        } else if (v > Math.PI) {
            v -= 2.0 * Math.PI;
        }

        return v;
    }

    PathSegmentResults straight_left_straight(double x, double y, double phi) {
        phi = mod2pi(phi);

        if (y > 0.0 && (0.0 < phi && phi < Math.PI * 0.99)) {
            double xd = -y / Math.tan(phi) + x;
            return new PathSegmentResults(true, xd - Math.tan(phi / 2.0), phi, Math.hypot(x - xd, y) - Math.tan(phi / 2.0));
        } else if (y < 0.0 && 0.0 < phi && phi < Math.PI * 0.99) {
            double xd = -y / Math.tan(phi) + x;
            return new PathSegmentResults(true, xd - Math.tan(phi / 2.0), phi, -Math.hypot(x - xd, y) - Math.tan(phi / 2.0));
        }

        return PSR_FALSE;
    }

    ArrayList<Path> set_path(ArrayList<Path> paths, double[] lengths, String[] ctypes, double step_size) {
        Path path = new Path();
        path.ctypes = ctypes;
        path.lengths = lengths;
        path.L = Arrays.stream(lengths).map(Math::abs).sum();

        // Check if same path exists
        for (Path i_path : paths) {
            boolean type_is_same = Arrays.equals(i_path.ctypes, path.ctypes);
            boolean length_is_close = Arrays.stream(i_path.lengths).map(Math::abs).sum() - path.L <= step_size;

            if (type_is_same && length_is_close) {
                return paths; // Same path found, so do not insert path
            }
        }

        // Check if path is long enough
        if (path.L <= step_size) {
            return paths; // Too short, so do not insert path
        }

        paths.add(path);
        return paths;
    }

    ArrayList<Path> straight_curve_straight(double x, double y, double phi, ArrayList<Path> paths, double step_size) {
        var psr = straight_left_straight(x, y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"S", "L", "S"}, step_size);
        }

        psr = straight_left_straight(x, -y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"S", "R", "S"}, step_size);
        }

        return paths;
    }

    double[] polar(double x, double y) {
        double r = Math.hypot(x, y);
        double theta = Math.atan2(y, x);

        return new double[]{r, theta};
    }

    PathSegmentResults left_straight_left(double x, double y, double phi) {
        var polar = polar(x - Math.sin(phi), y - 1.0 + Math.cos(phi));
        double u = polar[0], t = polar[1];

        if (t >= 0.0) {
            double v = mod2pi(phi - t);

            if (v >= 0.0) {
                return new PathSegmentResults(true, t, u, v);
            }
        }

        return PSR_FALSE;
    }

    PathSegmentResults left_right_left(double x, double y, double phi) {
        var polar = polar(x - Math.sin(phi), y - 1.0 + Math.cos(phi));
        double u1 = polar[0], t1 = polar[1];

        if (u1 <= 4.0) {
            double u = -2.0 * Math.asin(0.25 * u1);
            double t = mod2pi(t1 + 0.5 * u + Math.PI);
            double v = mod2pi(phi - t + u);

            if (t >= 0.0 && 0.0 >= u) {
                return new PathSegmentResults(true, t, u, v);
            }
        }

        return PSR_FALSE;
    }

    ArrayList<Path> curve_curve_curve(double x, double y, double phi, ArrayList<Path> paths, double step_size) {
        var psr = left_right_left(x, y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"L", "R", "L"}, step_size);
        }

        psr = left_right_left(-x, y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"L", "R", "L"}, step_size);
        }

        psr = left_right_left(x, -y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"R", "L", "R"}, step_size);
        }

        psr = left_right_left(-x, -y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"R", "L", "R"}, step_size);
        }

        // Backwards
        double xb = x * Math.cos(phi) + y * Math.sin(phi);
        double yb = x * Math.sin(phi) - y * Math.cos(phi);

        psr = left_right_left(xb, yb, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.v, psr.u, psr.t}, new String[]{"L", "R", "L"}, step_size);
        }

        psr = left_right_left(-xb, yb, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.v, -psr.u, -psr.t}, new String[]{"L", "R", "L"}, step_size);
        }

        psr = left_right_left(xb, -yb, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.v, psr.u, psr.t}, new String[]{"R", "L", "R"}, step_size);
        }

        psr = left_right_left(-xb, -yb, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.v, -psr.u, -psr.t}, new String[]{"R", "L", "R"}, step_size);
        }

        return paths;
    }

    ArrayList<Path> curve_straight_curve(double x, double y, double phi, ArrayList<Path> paths, double step_size) {
        var psr = left_straight_left(x, y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"L", "S", "L"}, step_size);
        }

        psr = left_straight_left(-x, y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"L", "S", "L"}, step_size);
        }

        psr = left_straight_left(x, -y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"R", "S", "R"}, step_size);
        }

        psr = left_straight_left(-x, -y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"R", "S", "R"}, step_size);
        }

        psr = left_straight_right(x, y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"L", "S", "R"}, step_size);
        }

        psr = left_straight_right(-x, y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"L", "S", "R"}, step_size);
        }

        psr = left_straight_right(x, -y, -phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{psr.t, psr.u, psr.v}, new String[]{"R", "S", "L"}, step_size);
        }

        psr = left_straight_right(-x, -y, phi);
        if (psr.valid) {
            paths = set_path(paths, new double[]{-psr.t, -psr.u, -psr.v}, new String[]{"R", "S", "L"}, step_size);
        }

        return paths;
    }

    PathSegmentResults left_straight_right(double x, double y, double phi) {
        var polar = polar(x + Math.sin(phi), y - 1.0 - Math.cos(phi));
        double u1 = polar[0], t1 = polar[1];
        u1 = Math.pow(u1, 2.0);

        if (u1 >= 4.0) {
            double u = Math.sqrt(u1 - 4.0);
            double theta = Math.atan2(2.0, u);
            double t = mod2pi(t1 + theta);
            double v = mod2pi(t - phi);

            if (t >= 0.0 && v >= 0.0) {
                return new PathSegmentResults(true, t, u, v);
            }
        }

        return PSR_FALSE;
    }

    ArrayList<Path> generate_path(double[] q0, double[] q1, double max_curvature, double step_size) {
        double dx = q1[0] - q0[0];
        double dy = q1[1] - q0[1];
        double dth = q1[2] - q0[2];
        double c = Math.cos(q0[2]);
        double s = Math.sin(q0[2]);

        double x = (c * dx + s * dy) * max_curvature;
        double y = (-s * dx + c * dy) * max_curvature;

        ArrayList<Path> paths = new ArrayList<>();
        paths = straight_curve_straight(x, y, dth, paths, step_size);
        paths = curve_straight_curve(x, y, dth, paths, step_size);
        paths = curve_curve_curve(x, y, dth, paths, step_size);

        return paths;
    }

    ArrayList<Double> calc_interpolate_dists_list(double[] lengths, double step_size) {
        ArrayList<Double> interpolate_dists_list = new ArrayList<>();

        for (double length : lengths) {
            double d_dist = length >= 0.0 ? step_size : -step_size;

        }

        return interpolate_dists_list;
    }
}
