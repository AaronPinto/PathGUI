import org.apache.commons.math3.linear.*;

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
	private static final double MIN_T = 1.0, MAX_T = 10.0, dt = 0.01, //Seconds
			max_accel = 8.0, //ft/s^2
			max_jerk = 15.0; //ft/s^3

	ArrayList<ArrayList<Double>> results = new ArrayList<>(Arrays.asList(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
			new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));

	MPGen2D(PathGUITool.Waypoint[] waypoints) {
		int timeSpliceIndex = 0;
		for(int i = 0; i < waypoints.length - 1; i++) {
			List<ArrayList<Double>> temp = quinticPolynomialsPlanner(waypoints[i].x, waypoints[i].y, waypoints[i].yaw, waypoints[i].v, waypoints[i].a,
					waypoints[i + 1].x, waypoints[i + 1].y, waypoints[i + 1].yaw, waypoints[i + 1].v, waypoints[i + 1].a);

			//TODO: improve this, it shouldn't just be the removal of a point
			if(i >= 1) for(ArrayList<Double> doubles : temp) doubles.remove(0);

			for(int j = 0; j < results.size(); j++) results.get(j).addAll(temp.get(j));

			//We don't want each path segment to start with t = 0.0, make the time continuous
			if(results.get(0).size() > timeSpliceIndex && timeSpliceIndex != 0)
				for(int j = timeSpliceIndex; j < results.get(0).size(); j++)
					results.get(0).set(j, results.get(0).get(j) + results.get(0).get(timeSpliceIndex - 1));

			timeSpliceIndex += temp.get(0).size();
		}

		for(int i = 0; i < results.get(0).size(); i++)
			System.out.printf("%f %.15f %.16f %.16f %.16f %.16f %.16f\n", results.get(0).get(i), results.get(1).get(i), results.get(2).get(i),
					results.get(3).get(i), results.get(4).get(i), results.get(5).get(i), results.get(6).get(i));
	}

	public static void main(String[] args) {
//		new MPGen2D(new PathGUITool.Waypoint[]{
//				new PathGUITool.Waypoint(0.0, 23.030627871362938, Math.toRadians(0.0), 1.0, 0.0),
//				new PathGUITool.Waypoint(9.397058823529411, 23.65084226646248, Math.toRadians(4.0), 8.0, 0.0),
//				new PathGUITool.Waypoint(20.470588235294116, 9.344563552833078, Math.toRadians(-90.0), 8.0, 0.0)
//		});

		new MPGen2D(new PathGUITool.Waypoint[]{
				new PathGUITool.Waypoint(0.8626198083067093, 22.358490566037734, 0.0, 0.01, 0.1),
				new PathGUITool.Waypoint(9.287539936102236, 23.80188679245283, 0.06981317007977318, 5.0, 0.0),
				new PathGUITool.Waypoint(19.926517571884983, 19.952830188679243, -1.5707963267948966, 2.0, 0.0),
				new PathGUITool.Waypoint(19.49520766773163, 7.160377358490566, -1.5707963267948966, 5.0, 0.0),
				new PathGUITool.Waypoint(25.159744408945688, 4.783018867924528, 0.5235987755982988, 0.0, 0.0)
		});
	}

	static class QuinticPolynomial {
		private double a0, a1, a2, a3, a4, a5;
		RealMatrix A;
		RealVector b;
		RealVector x;

		QuinticPolynomial(double xs, double vxs, double axs, double xe, double vxe, double axe, double T) {
			this.a0 = xs;
			this.a1 = vxs;
			this.a2 = axs / 2.0;

			double T2 = T * T, T3 = T * T * T, T4 = T * T * T * T, T5 = T * T * T * T * T;

			//Ax = b, solve for x
			this.A = new Array2DRowRealMatrix(new double[][]{
					{T3, T4, T5},
					{3 * T2, 4 * T3, 5 * T4},
					{6 * T, 12 * T2, 20 * T3}
			});
			this.b = new ArrayRealVector(new double[]{
					xe - this.a0 - this.a1 * T - this.a2 * T2,
					vxe - this.a1 - 2 * this.a2 * T,
					axe - 2 * this.a2}
			);
			this.x = new LUDecomposition(this.A).getSolver().solve(b);

			this.a3 = this.x.getEntry(0);
			this.a4 = this.x.getEntry(1);
			this.a5 = this.x.getEntry(2);
		}

		double calcPoint(double t) {
			return this.a0 + this.a1 * t + this.a2 * (t * t) + this.a3 * (t * t * t) + this.a4 * (t * t * t * t) + this.a5 * (t * t * t * t * t);
		}

		double calcFirstDeriv(double t) {
			return this.a1 + 2 * this.a2 * t + 3 * this.a3 * (t * t) + 4 * this.a4 * (t * t * t) + 5 * this.a5 * (t * t * t * t);
		}

		double calcSecondDeriv(double t) {
			return 2 * this.a2 + 6 * this.a3 * t + 12 * this.a4 * (t * t) + 20 * this.a5 * (t * t * t);
		}

		double calcThirdDeriv(double t) {
			return 6 * this.a3 + 24 * this.a4 * t + 60 * this.a5 * (t * t);
		}
	}

	private static List<ArrayList<Double>> quinticPolynomialsPlanner(double sx, double sy, double syaw, double sv, double sa, double gx, double gy, double gyaw, double gv,
	                                                                 double ga) {
		double vxs = sv * Math.cos(syaw), vys = sv * Math.sin(syaw), vxg = gv * Math.cos(gyaw), vyg = gv * Math.sin(gyaw);
		double axs = sa * Math.cos(syaw), ays = sa * Math.sin(syaw), axg = ga * Math.cos(gyaw), ayg = ga * Math.sin(gyaw);
		ArrayList<Double> time = new ArrayList<>(), rx = new ArrayList<>(), ry = new ArrayList<>(), ryaw = new ArrayList<>(),
				rv = new ArrayList<>(), ra = new ArrayList<>(), rj = new ArrayList<>();

		for(double T = MIN_T; T <= MAX_T; T += MIN_T) {
			QuinticPolynomial xqp = new QuinticPolynomial(sx, vxs, axs, gx, vxg, axg, T);
			QuinticPolynomial yqp = new QuinticPolynomial(sy, vys, ays, gy, vyg, ayg, T);

			time.clear();
			rx.clear();
			ry.clear();
			ryaw.clear();
			rv.clear();
			ra.clear();
			rj.clear();

			double t;
			for(t = 0.0; t < T + dt; t = new BigDecimal(t + dt).setScale(3, RoundingMode.HALF_UP).doubleValue()) {
				time.add(t);
				rx.add(xqp.calcPoint(t));
				ry.add(yqp.calcPoint(t));

				double vx = xqp.calcFirstDeriv(t), vy = yqp.calcFirstDeriv(t);
				double v = Math.hypot(vx, vy);
				double yaw = Math.atan2(vy, vx);
				rv.add(v);
				ryaw.add(yaw);

				double ax = xqp.calcSecondDeriv(t), ay = yqp.calcSecondDeriv(t);
				double a = Math.hypot(ax, ay);
				if(rv.size() >= 2 && rv.get(rv.size() - 1) - rv.get(rv.size() - 2) < 0.0)
					a *= -1;
				ra.add(a);

				double jx = xqp.calcThirdDeriv(t), jy = yqp.calcThirdDeriv(t);
				double j = Math.hypot(jx, jy);
				if(ra.size() >= 2 && ra.get(ra.size() - 1) - ra.get(ra.size() - 2) < 0.0)
					j *= -1;
				rj.add(j);
			}

			ArrayList<Double> absRa = new ArrayList<>(ra.size()), absRj = new ArrayList<>(rj.size());
			for(int i = 0; i < ra.size(); i++) {
				absRa.add(i, Math.abs(ra.get(i)));
				absRj.add(i, Math.abs(rj.get(i)));
			}

			//Test without jerk and see becuz like its robotics lmao who even uses jerk wtf?
			if(Collections.max(absRa) <= max_accel && Collections.max(absRj) <= max_jerk) {
				System.out.println("found valid path");
				break;
			}
		}

		return Arrays.asList(time, rx, ry, ryaw, rv, ra, rj);
	}
}