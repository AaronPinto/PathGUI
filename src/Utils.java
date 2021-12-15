import util.BetterArrayList;
import util.Path;
import util.Waypoint;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public final class Utils {
    private static final double kEpsilon = 1e-9;

    /**
     * This function formats and appends all the values in all the paths into proper 2D array syntax and then returns that String.
     *
     * @return a String that contains all the values for all the paths
     */
    static String output2DArray(Path currentPath, LinkedHashMap<String, Path> paths) {
        StringBuilder output = new StringBuilder();
        output.append("public static Object[][] currentPath = new Object[][]{\n");

        outputPath(output, currentPath);

        paths.forEach((key, value) -> {
            output.append(String.format("public static Object[][] %s = new Object[][]{\n", key));
            outputPath(output, value);
        });

        System.out.println("2D Array output: " + output);
        return output.toString();
    }

    /**
     * This function does the actual formatting and appending for all the values in a path to a StringBuilder
     *
     * @param output the StringBuilder to append the values of the path to.
     * @param value  the path to parse, format and get the values from.
     */
    static void outputPath(StringBuilder output, Path value) {
        output.append(value.clickPoints.stream().map(clickPoint -> "{" + clickPoint + "},\n").collect(Collectors.joining()));
        output.append("};\n");
    }

    /**
     * A simple function that constrains a value from the specified min bound to the specified max bound
     *
     * @param value        The value to constrain
     * @param minConstrain The minimum bound (in the same units as the value)
     * @param maxConstrain The maximum bound (in the same units as the value)
     *
     * @return the constrained value
     */
    static double constrainTo(double value, double minConstrain, double maxConstrain) {
        return Math.max(minConstrain, Math.min(maxConstrain, value));
    }

    public static BetterArrayList<Waypoint> convertResults(PathResults results) {
        BetterArrayList<Waypoint> temp = new BetterArrayList<>();

        for (int i = 0; i < results.time.size(); i++) {
            temp.add(new Waypoint(results.x.get(i), results.y.get(i), results.getRad().get(i), results.vel.get(i), results.accel.get(i)));
        }

        return temp;
    }

    public static boolean absLessThanEps(double x) {
        return Math.abs(x) < kEpsilon;
    }

    /**
     * A function that takes a BetterArrayList as an argument, and performs a deep copy of tall the values A deep copy means that an object
     * is copied along with all the object to which it refers. In this case, all the values of the objects that the BetterArrayList refer to
     * are copied into a new double array, which is then returned.
     *
     * @param p the BetterArrayList to convert to a 2d double array
     *
     * @return a 2d double array containing all the points from the BetterArrayList
     */
    static Waypoint[] convertPointArray(BetterArrayList<Waypoint> p) {
        return p.stream().map(Waypoint::new).toArray(Waypoint[]::new);
    }

    /**
     * Get the waypoint's kinematic values
     *
     * @return the yaw (rad), speed and acceleration values for the waypoint
     */
    static double[] getWaypointKinematicValues() {
        String s;

        if ((s = JOptionPane.showInputDialog("Enter a yaw (deg), speed, and accel value:")) != null) {
            double[] temp = Arrays.stream(s.split(", ")).mapToDouble(Double::parseDouble).toArray();

            temp[0] = Math.toRadians(temp[0]);

            return temp;
        }

        return null;
    }

    /**
     * Get the waypoint's kinematic values
     *
     * @return the yaw (deg), speed and acceleration values for the waypoint
     */
    static double[] editWaypointKinematicValues(String initialValue) {
        String s;

        if ((s = JOptionPane.showInputDialog(null, "Edit yaw (deg), speed, and accel values:", initialValue)) != null) {
            return Arrays.stream(s.split(", ")).mapToDouble(Double::parseDouble).toArray();
        }

        return null;
    }
}
