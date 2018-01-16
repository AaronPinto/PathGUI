package GUIStuff;

import java.util.LinkedList;
import java.util.List;

/**
 * This Class provides many useful algorithms for Robot Path Planning. It uses optimization techniques and knowledge
 * of Robot Motion in order to calculate smooth path trajectories, if given only discrete waypoints. The Benefit of
 * these optimization algorithms are very efficient path planning that can be used to navigate in real-time.
 * <p>
 * This Class uses a method of Gradient Descent, and other optimization techniques to produce smooth Velocity profiles
 * for both left and right wheels of a differential drive robot.
 * <p>
 * This Class does not attempt to calculate quintic or cubic splines for best fitting a curve. It is for this reason, the
 * algorithm can be ran on embedded devices with very quick computation times.
 * <p>
 * The output of this function are independent velocity profiles for the left and right wheels of a differential
 * drive chassis. The velocity profiles start and end with 0 velocity and maintain smooth transitions throughout the path.
 *
 * @author https://github.com/KHEngineering/SmoothPathPlanner
 * Modified by: Aaron Pinto
 */
public class MPGen2D {
	double[][] smoothPath;
	private double[][] origPath;
	private double timeStep, totalTime, robotTrackWidth, pathAlpha, pathBeta, pathTolerance;

	/**
	 * Constructor to calculate a new 2D Motion Profile
	 *
	 * @param waypoint   double[][]
	 * @param time       double
	 * @param tStep      double
	 * @param trackWidth double
	 */
	MPGen2D(double[][] waypoint, double time, double tStep, double trackWidth) {
		if(waypoint.length == 1) {
			smoothPath = waypoint;
			return;
		} else if(waypoint.length > 0)
			origPath = doubleArrayDeepCopy(waypoint);
		else return;

		pathAlpha = 0.3;
		pathBeta = 0.7;
		pathTolerance = 0.0000001;

		robotTrackWidth = trackWidth;
		timeStep = tStep;
		totalTime = time;
	}

	/**
	 * Performs a deep copy of a 2 Dimensional Array looping thorough each element in the 2D array
	 */
	private static double[][] doubleArrayDeepCopy(double[][] arr) {
		double[][] temp = new double[arr.length][arr[0].length];

		for(int i = 0; i < arr.length; i++) {
			temp[i] = new double[arr[i].length];
			System.arraycopy(arr[i], 0, temp[i], 0, arr[i].length);
		}
		return temp;
	}

	private static double[][] nodeOnlyWayPoints(double[][] path) {
		List<double[]> li = new LinkedList<>();
		li.add(path[0]);//The starting waypoint

		//find intermediate nodes and calculate direction
		for(int i = 1; i < path.length - 1; i++) {
			/*
			  Calculates the angle in RADIANS of each successive pair of waypoints.
			  Vector 1 is the angle in RADIANS that the waypoint at position i-1
			  makes with the waypoint at position i. The angle is relative to the
			  x-axis of the waypoint at i-1. Vector 2 is the angle in RADIANS that
			  the waypoint at position i makes with the waypoint at position i+1.
			  The angle is relative to a horizontal line at the y value of the waypoint at i.
			 */
			double vector1 = Math.atan2((path[i][1] - path[i - 1][1]), path[i][0] - path[i - 1][0]);
			double vector2 = Math.atan2((path[i + 1][1] - path[i][1]), path[i + 1][0] - path[i][0]);

			//determine if both vectors have a change in direction greater than 0.1 degrees
			if(Math.abs(vector2 - vector1) > 0.0017453292519943296)
				li.add(path[i]);
		}
		li.add(path[path.length - 1]);//The last waypoint

		/*
		  re-write nodes into new 2D Array, first and last points in the waypoint
		  sequence are always included in the new array even if there is no
		  direction change between the first to next point and the last to
		  previous point
		 */
		double[][] temp = new double[li.size()][2];

		for(int i = 0; i < li.size(); i++) {
			temp[i][0] = li.get(i)[0];//X-coordinates
			temp[i][1] = li.get(i)[1];//Y-coordinates
		}
		return temp;
	}

	private int[] injectionCounter2Steps(double numNodeOnlyPoints, double maxTimeToComplete, double tStep) {
		int[] ret;
		int first = 0;
		int second = 0;
		int third = 0;
		double oldPointsTotal = 0;
		double totalPoints = maxTimeToComplete / tStep;

		if(totalPoints < 100) {
			double pointsFirst;
			double pointsTotal;

			for(int i = 4; i <= 6; i++)
				for(int j = 1; j <= 8; j++) {
					pointsFirst = i * (numNodeOnlyPoints - 1) + numNodeOnlyPoints;
					pointsTotal = (j * (pointsFirst - 1) + pointsFirst);
					//					System.out.println(pointsFirst + " " + pointsTotal + " " + i + " " + j);
					if(pointsTotal <= totalPoints && pointsTotal > oldPointsTotal) {
						first = i;
						second = j;
						//						System.out.println(first + " " + second);
						oldPointsTotal = pointsTotal;
					}
				}
			ret = new int[]{first, second, third};
		} else {
			double pointsFirst;
			double pointsSecond;
			double pointsTotal;

			for(int i = 1; i <= 5; i++)
				for(int j = 1; j <= 8; j++)
					for(int k = 1; k < 8; k++) {
						pointsFirst = i * (numNodeOnlyPoints - 1) + numNodeOnlyPoints;
						pointsSecond = (j * (pointsFirst - 1) + pointsFirst);
						pointsTotal = (k * (pointsSecond - 1) + pointsSecond);
//						System.out.println(pointsFirst + " " + pointsSecond + " " + pointsTotal + " " + i + " " + j + " " + k);

						if(pointsTotal <= totalPoints) {
//							System.out.println(i + " " + j + " " + k);

							first = i;
							second = j;
							third = k;
						}
					}
			ret = new int[]{first, second, third};
		}
		return ret;
	}

	/**
	 * Method interpolates the path by linear injection. The result providing more waypoints along the path.
	 */
	private double[][] inject(double[][] orig, int numToInject) {
		//create extended 2 Dimensional array to hold additional points
		double morePoints[][] = new double[orig.length + ((numToInject) * (orig.length - 1))][2];
		//		System.out.println(orig.length + " " + numToInject + " " + (orig.length + ((numToInject) * (orig.length - 1))));

		int index = 0;

		//loop through original array
		for(int i = 0; i < orig.length - 1; i++) {
			//copy first
			morePoints[index][0] = orig[i][0];
			morePoints[index][1] = orig[i][1];
			//			System.out.println(morePoints[index][0] + " " + morePoints[index][1] + " " + index + " in");
			index++;
			for(int j = 1; j < numToInject + 1; j++) {
				//calculate intermediate x points between j and j + 1 original points
				morePoints[index][0] = j * ((orig[i + 1][0] - orig[i][0]) / (numToInject + 1)) + orig[i][0];
				//calculate intermediate y points  between j and j + 1 original points
				morePoints[index][1] = j * ((orig[i + 1][1] - orig[i][1]) / (numToInject + 1)) + orig[i][1];
				//				System.out.println(morePoints[index][0] + " " + morePoints[index][1] + " " + index + " inner");
				index++;
			}
		}
		//copy last
		morePoints[index][0] = orig[orig.length - 1][0];
		morePoints[index][1] = orig[orig.length - 1][1];

		//		for(int i = 1; i < morePoints.length; i++)
		//			System.out.println(morePoints[i - 1][0] + " " + morePoints[i - 1][1] + " " + (morePoints[i][0] - morePoints[i - 1][0]));

		return morePoints;
	}

	/**
	 * Optimization algorithm, which optimizes the data points in path to create a smooth trajectory.
	 * This optimization uses gradient descent. While unlikely, it is possible for this algorithm to never
	 * converge. If this happens, try increasing the tolerance level.
	 */
	private double[][] smoother(double[][] path, double weight_data, double weight_smooth, double tolerance) {
		double[][] newPath = doubleArrayDeepCopy(path);
		double change = tolerance;

		while(change >= tolerance) {
			change = 0.0;
			for(int i = 1; i < path.length - 1; i++)
				for(int j = 0; j < path[i].length; j++) {
					double aux = newPath[i][j];
					newPath[i][j] += weight_data * (path[i][j] - newPath[i][j]) + weight_smooth *
							(newPath[i - 1][j] + newPath[i + 1][j] - (2.0 * newPath[i][j]));
					change += Math.abs(aux - newPath[i][j]);
				}
		}
		return newPath;
	}

	public void calculate() {
		if(origPath != null) {
			//first find only direction changing nodes
			double[][] nodeOnlyPath = nodeOnlyWayPoints(origPath);

			//Figure out how many nodes to inject
			int[] inject = injectionCounter2Steps(nodeOnlyPath.length, totalTime, timeStep);

			//Iteratively inject and smooth the path
			for(int i = 0; i < inject.length; i++) {
				if(i == 0) {
					smoothPath = inject(nodeOnlyPath, inject[0]);
					smoothPath = smoother(smoothPath, pathAlpha, pathBeta, pathTolerance);
				} else {
					smoothPath = inject(smoothPath, inject[i]);
					smoothPath = smoother(smoothPath, 0.1, 0.3, 0.0000001);
				}
			}

//			Arrays.stream(smoothPath).map(aSmoothPath -> "{" + aSmoothPath[0] + ", " + aSmoothPath[1] + "},").forEach(System.out::println);
		}
	}
}	