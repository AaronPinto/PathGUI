package GUIStuff;

import java.util.LinkedList;
import java.util.List;

/**
 * This Class provides many useful algorithms for Robot Path Planning. It uses optimization techniques and knowledge
 * of Robot Motion in order to calculate smooth path trajectories, if given only discrete waypoints. The Benefit of 
 * these optimization algorithms are very efficient path planning that can be used to navigate in real-time.
 * 
 * This Class uses a method of Gradient Descent, and other optimization techniques to produce smooth Velocity profiles
 * for both left and right wheels of a differential drive robot.
 * 
 * This Class does not attempt to calculate quintic or cubic splines for best fitting a curve. It is for this reason, the 
 * algorithm can be ran on embedded devices with very quick computation times.
 * 
 * The output of this function are independent velocity profiles for the left and right wheels of a differential 
 * drive chassis. The velocity profiles start and end with 0 velocity and maintain smooth transitions throughout the path. 
 * 
 * @author https://github.com/KHEngineering/SmoothPathPlanner 
 * Modified by: Aaron Pinto 
 */
public class MPGen2D {
	public double[][] origPath;
	public double[][] nodeOnlyPath;
	public double[][] smoothPath;
	public double[][] leftPath;
	public double[][] rightPath;
	public double[][] origCenterVelocity;
	public double[][] origRightVelocity;
	public double[][] origLeftVelocity;
	public double[][] smoothCenterVelocity;
	public double[][] smoothRightVelocity;
	public double[][] smoothLeftVelocity;
	public double[][] heading;
	public double[] smoothLeftPosition;
	public double[] smoothCenterPosition;
	public double[] smoothRightPosition;
	public double[] smoothLeftAccel;
	public double[] smoothRightAccel;
	public double[] avgDeltaDistance;

	double timeStep, totalTime, totalDistance, robotTrackWidth, pathAlpha, pathBeta, pathTolerance,
	velocityAlpha, velocityBeta, velocityTolerance;
	public double pathDist, numFinalPoints;

	/**
	 * Constructor to calculate a new 2D Motion Profile
	 * @param waypoint double[][]
	 * @param time double
	 * @param tStep double
	 * @param trackWidth double
	 */
	public MPGen2D(double[][] waypoint, double time, double tStep, double trackWidth) {
		if(waypoint.length > 0)
			origPath = doubleArrayDeepCopy(waypoint);
		else return;

		pathAlpha = 0.3;
		pathBeta = 0.7;
		pathTolerance = 0.0000001;

		velocityAlpha = 0.1;
		velocityBeta = 0.3;
		velocityTolerance = 0.0000001;

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
			for(int j = 0; j < arr[i].length; j++)
				temp[i][j] = arr[i][j];
		}
		return temp;
	}

	private static double[][] nodeOnlyWayPoints(double[][] path) {
		List<double[]> li = new LinkedList<double[]>();
		li.add(path[0]);//The starting waypoint

		//find intermediate nodes and calculate direction
		for(int i = 1; i < path.length - 1; i++) {			
			/**
			 * Calculates the angle in RADIANS of each successive pair of waypoints.
			 * Vector 1 is the angle in RADIANS that the waypoint at position i-1 
			 * makes with the waypoint at position i. The angle is relative to the
			 * x-axis of the waypoint at i-1. Vector 2 is the angle in RADIANS that
			 * the waypoint at position i makes with the waypoint at position i+1.
			 * The angle is relative to the x-axis of the waypoint at i.
			 */
			double vector1 = Math.atan2((path[i][1] - path[i - 1][1]), path[i][0] - path[i - 1][0]);
			double vector2 = Math.atan2((path[i + 1][1] - path[i][1]), path[i + 1][0] - path[i][0]);

			//determine if both vectors have a change in direction greater than 0.1 degrees
			if(Math.abs(vector2-vector1) > 0.0017453292519943296) 
				li.add(path[i]);
		}
		li.add(path[path.length - 1]);//The last waypoint

		/**
		 * re-write nodes into new 2D Array, first and last points in the waypoint
		 * sequence are always included in the new array even if there is no
		 * direction change between the first to next point and the last to
		 * previous point
		 */
		double[][] temp = new double[li.size()][2];

		for(int i = 0; i < li.size(); i++) {
			temp[i][0] = li.get(i)[0];//X-coordinates
			temp[i][1] = li.get(i)[1];//Y-coordinates
		}	
		return temp;
	}

	private int[] injectionCounter2Steps(double numNodeOnlyPoints, double maxTimeToComplete, double tStep) {
		int[] ret = null;
		int first = 0;
		int second = 0;
		int third = 0;
		double oldPointsTotal = 0;
		double totalPoints = maxTimeToComplete / tStep;
		numFinalPoints = 0;

		if(totalPoints < 100) {
			double pointsFirst = 0;
			double pointsTotal = 0;

			for(int i = 4; i <= 6; i++)
				for(int j = 1; j <= 8; j++) {
					pointsFirst = i * (numNodeOnlyPoints - 1) + numNodeOnlyPoints;
					pointsTotal = (j * (pointsFirst - 1) + pointsFirst);
					//					System.out.println(pointsFirst + " " + pointsTotal + " " + i + " " + j);
					if(pointsTotal <= totalPoints && pointsTotal > oldPointsTotal) {
						first = i;
						second = j;
						//						System.out.println(first + " " + second);
						numFinalPoints = pointsTotal;
						oldPointsTotal = pointsTotal;
					}
				}
			ret = new int[] {first, second, third};
		} else {
			double pointsFirst = 0;
			double pointsSecond = 0;
			double pointsTotal = 0;

			for(int i = 1; i <= 5; i++)
				for(int j = 1; j <= 8; j++)
					for(int k = 1; k < 8; k++) {
						pointsFirst = i * (numNodeOnlyPoints - 1) + numNodeOnlyPoints;
						pointsSecond = (j * (pointsFirst - 1) + pointsFirst);
						pointsTotal = (k * (pointsSecond - 1) + pointsSecond);

						if(pointsTotal <= totalPoints) {
							first = i;
							second = j;
							third = k;
							numFinalPoints = pointsTotal;
						}
					}
			ret = new int[] {first, second, third};
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
		for(int i = 0; i < orig.length-1; i++) {
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
		index++;

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

	private void leftRight(double[][] smoothPath, double robotTrackWidth) {
		double[][] leftPath = new double[smoothPath.length][2];
		double[][] rightPath = new double[smoothPath.length][2];
		double[][] gradient = new double[smoothPath.length][2];

		for(int i = 0; i < smoothPath.length - 1; i++) {
			if((smoothPath[i + 1][0] - smoothPath[i][0]) > 0.0)
				gradient[i][1] = Math.atan2(smoothPath[i + 1][1] - smoothPath[i][1], smoothPath[i + 1][0]
						- smoothPath[i][0]);
			else if((smoothPath[i + 1][1] - smoothPath[i][1]) > 0.0)
				gradient[i][1] = Math.PI - Math.atan2(smoothPath[i + 1][1] - smoothPath[i][1], smoothPath[i + 1][0]
						- smoothPath[i][0]);
			else
				gradient[i][1] = Math.abs(Math.atan2(smoothPath[i + 1][1] - smoothPath[i][1], smoothPath[i + 1][0]
						- smoothPath[i][0])) - Math.PI;

			//			System.out.println((smoothPath[i + 1][1] - smoothPath[i][1]) + " " + (smoothPath[i + 1][0] - smoothPath[i][0])
			//								+ " " + Math.atan2(smoothPath[i + 1][1] - smoothPath[i][1], smoothPath[i + 1][0] - smoothPath[i][0])
			//								+ " " + Math.toDegrees(gradient[i][1]));
		} 

		gradient[gradient.length - 1][1] = gradient[gradient.length - 2][1];

		for(int i = 0; i < gradient.length; i++) {
			leftPath[i][0] = robotTrackWidth / 2 * Math.cos(gradient[i][1] + Math.PI / 2) + smoothPath[i][0];
			leftPath[i][1] = robotTrackWidth / 2 * Math.sin(gradient[i][1] + Math.PI / 2) + smoothPath[i][1];
			rightPath[i][0] = robotTrackWidth / 2 * Math.cos(gradient[i][1] - Math.PI / 2) + smoothPath[i][0];
			rightPath[i][1] = robotTrackWidth / 2 * Math.sin(gradient[i][1] - Math.PI / 2) + smoothPath[i][1];

			//convert to degrees 0 to 360 where 0 degrees is +X - axis, accumulated to align with WPI sensor
			gradient[i][1] = -Math.toDegrees(gradient[i][1]);

			if(i > 0) {
				if((gradient[i][1] - gradient[i - 1][1]) > 180)
					gradient[i][1] = -360 + gradient[i][1];

				if((gradient[i][1] - gradient[i - 1][1]) < -180)
					gradient[i][1] = 360 + gradient[i][1];
			}
		}
		this.heading = gradient;
		this.leftPath = leftPath;
		this.rightPath = rightPath;
	}

	/**
	 * Returns velocity as a double array. The first column vector is time, based on the time step, the second vector 
	 * is the velocity magnitude.
	 */
	private double[][] velocity(double[][] smoothPath, double tStep) {
		double[] dxdt = new double[smoothPath.length];
		double[] dydt = new double[smoothPath.length];
		double[][] velocity = new double[smoothPath.length][2];

		dxdt[0] = 0;
		dydt[0] = 0;
		velocity[0][0] = 0;
		velocity[0][1] = 0;
		heading[0][1] = 0;

		for(int i = 1; i < smoothPath.length; i++) {
			dxdt[i] = (smoothPath[i][0] - smoothPath[i - 1][0]) / tStep;
			dydt[i] = (smoothPath[i][1] - smoothPath[i - 1][1]) / tStep;

			//create time vector
			velocity[i][0] = velocity[i - 1][0] + tStep;
			heading[i][0] = heading[i - 1][0] + tStep;

			//calculate velocity
			velocity[i][1] = Math.hypot(dxdt[i], dydt[i]);
			velocity[i][1] *= (smoothPath[i][0] - smoothPath[i - 1][0]) < 0.0 ? -1.0 : 1.0;
		}
		return velocity;
	}

	/**
	 * Optimize velocity by minimizing the error distance at the end of travel
	 * when this function converges, the fixed velocity vector will be smooth, start
	 * and end with 0 velocity, and travel the same final distance as the original
	 * non-smoothed velocity profile
	 * 
	 * This Algorithm may never converge. If this happens, reduce tolerance. 
	 */
	private double[][] velocityFix(double[][] smoothVelocity, double[][] origVelocity, double tolerance) {
		/*
		 * 1. Find Error Between Original Velocity and Smooth Velocity
		 * 2. Keep increasing the velocity between the first and last node of the smooth Velocity by a small amount
		 * 3. Recalculate the difference, stop if threshold is met or repeat step 2 until the final threshold is met.
		 * 3. Return the updated smoothVelocity
		 */
		double[] difference = errorSum(origVelocity, smoothVelocity);
		double[][] fixVel = new double[smoothVelocity.length][2];
		double increase = 0.0;

		for(int i = 0; i < smoothVelocity.length; i++) {
			fixVel[i][0] = smoothVelocity[i][0];
			fixVel[i][1] = smoothVelocity[i][1];
		}
		while(Math.abs(difference[difference.length - 1]) > tolerance) {
			increase = difference[difference.length - 1] / 1 / 50;

			for(int i = 1; i < fixVel.length - 1; i++)
				fixVel[i][1] = fixVel[i][1] - increase;

			difference = errorSum(origVelocity, fixVel);
		}
		return fixVel;
	}

	/**
	 * This method calculates the integral of the smooth velocity term and compares it to the integral of the 
	 * original velocity term. In essence we are comparing the total distance by the original velocity path and 
	 * the smooth velocity path to ensure that as we modify the smooth Velocity it still covers the same distance 
	 * as was intended by the original velocity path.
	 */
	private double[] errorSum(double[][] origVelocity, double[][] smoothVelocity) {
		double[] tempOrigDist = new double[origVelocity.length];
		double[] tempSmoothDist = new double[smoothVelocity.length];
		double[] difference = new double[smoothVelocity.length];
		double tStep = origVelocity[1][0] - origVelocity[0][0];

		tempOrigDist[0] = origVelocity[0][1];
		tempSmoothDist[0] = smoothVelocity[0][1];

		//calculate difference
		for(int i = 1; i < origVelocity.length; i++) {
			tempOrigDist[i] = origVelocity[i][1] * tStep + tempOrigDist[i - 1];
			tempSmoothDist[i] = smoothVelocity[i][1] * tStep + tempSmoothDist[i - 1];
			difference[i] = tempSmoothDist[i] - tempOrigDist[i];
		}
		return difference;
	}

	private double[] getPosition(double[][] velocity) {
		double tempPos[] = new double[velocity.length];
		tempPos[0] = 0;
		for(int i = 1; i < velocity.length; i++)
			tempPos[i] = (velocity[i][1] / 60 * timeStep) + tempPos[i - 1];
		return tempPos;
	}

	private double calcPositionLength() {
		double length = 0;
		for(int i = 1; i < smoothLeftPosition.length; i++) {
			length += ((Math.abs(smoothLeftPosition[i]) - Math.abs(smoothLeftPosition[i - 1])) + 
					(Math.abs(smoothRightPosition[i]) - Math.abs(smoothRightPosition[i - 1]))) / 2;
		}
		length *= (Math.signum(smoothLeftPosition[smoothLeftPosition.length - 1]) == -1.0) ? -1.0 : 1.0;
		return length;
	}

	public double[] calcSumLengthBetweenPoints() {
		double[] sumLength = new double[smoothLeftPosition.length];
		for(int i = 1; i < smoothLeftPosition.length; i++) {
			sumLength[i] = (((smoothLeftPosition[i] - smoothLeftPosition[i - 1]) - 
					(smoothRightPosition[i] - smoothRightPosition[i - 1])) / 2) + sumLength[i - 1];
		}
		return sumLength;
	}

	public static double[][] transposeVector(double[][] arr) {
		double[][] temp = new double[arr[0].length][arr.length];

		for(int i = 0; i < temp.length; i++)
			for(int j = 0; j < temp[i].length; j++) 
				temp[i][j] = arr[j][i];
		return temp;		
	}

	public void setPathAlpha(double alpha) {
		pathAlpha = alpha;
	}

	public void setPathBeta(double beta) {
		pathAlpha = beta;
	}

	public void setPathTolerance(double tolerance) {
		pathAlpha = tolerance;
	}

	public void calculate() {
		if(origPath != null) {
			//first find only direction changing nodes
			nodeOnlyPath = nodeOnlyWayPoints(origPath);

			//				for(int i = 0; i < nodeOnlyPath.length; i++)
			//					System.out.println(nodeOnlyPath[i][0] + " " + nodeOnlyPath[i][1]);

			//Figure out how many nodes to inject
			int[] inject = injectionCounter2Steps(nodeOnlyPath.length, totalTime, timeStep);

			//		for(int i = 0; i < inject.length; i++)
			//			System.out.println(inject[i]);

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

			//calculate left and right path based on center path
			leftRight(smoothPath, robotTrackWidth);

			//		for(int i = 0; i < leftPath.length; i++)
			//			System.out.println("lX: " + leftPath[i][0] + ",lY: " + leftPath[i][1] + ",rX: " + rightPath[i][0]
			//					+ ",rY: " + rightPath[i][1] + ",head: " + heading[i][1]);

			origCenterVelocity = velocity(smoothPath, timeStep);
			origLeftVelocity = velocity(leftPath, timeStep);
			origRightVelocity = velocity(rightPath, timeStep);

			smoothCenterVelocity = doubleArrayDeepCopy(origCenterVelocity);
			smoothLeftVelocity = doubleArrayDeepCopy(origLeftVelocity);
			smoothRightVelocity = doubleArrayDeepCopy(origRightVelocity);

			smoothCenterVelocity[smoothCenterVelocity.length - 1][1] = 0.0;
			smoothLeftVelocity[smoothLeftVelocity.length - 1][1] = 0.0;
			smoothRightVelocity[smoothRightVelocity.length - 1][1] = 0.0;

			smoothCenterVelocity = smoother(smoothCenterVelocity, velocityAlpha, velocityBeta, velocityTolerance);
			smoothLeftVelocity = smoother(smoothLeftVelocity, velocityAlpha, velocityBeta, velocityTolerance);
			smoothRightVelocity = smoother(smoothRightVelocity, velocityAlpha, velocityBeta, velocityTolerance);

			//fix velocity distance error
			smoothCenterVelocity = velocityFix(smoothCenterVelocity, origCenterVelocity, 0.0000001);
			smoothLeftVelocity = velocityFix(smoothLeftVelocity, origLeftVelocity, 0.0000001);
			smoothRightVelocity = velocityFix(smoothRightVelocity, origRightVelocity, 0.0000001);

			smoothCenterVelocity = smoother(smoothCenterVelocity, velocityAlpha, velocityBeta, velocityTolerance);
			smoothLeftVelocity = smoother(smoothLeftVelocity, velocityAlpha, velocityBeta, velocityTolerance);
			smoothRightVelocity = smoother(smoothRightVelocity, velocityAlpha, velocityBeta, velocityTolerance);

			smoothCenterVelocity = velocityFix(smoothCenterVelocity, origCenterVelocity, 0.0000001);
			smoothLeftVelocity = velocityFix(smoothLeftVelocity, origLeftVelocity, 0.0000001);
			smoothRightVelocity = velocityFix(smoothRightVelocity, origRightVelocity, 0.0000001);

			//velocity[i][0] = time, velocity[i][1] = velocity
			for(int i = 0; i < smoothLeftVelocity.length; i++) {
				smoothLeftVelocity[i][1] *= 60;
				smoothCenterVelocity[i][1] *= 60;
				smoothRightVelocity[i][1] *= 60;
			}

			smoothLeftPosition = getPosition(smoothLeftVelocity);
			smoothCenterPosition = getPosition(smoothCenterVelocity);
			smoothRightPosition = getPosition(smoothRightVelocity);

			for(int i = 0; i < smoothLeftVelocity.length; i++) {
				smoothRightVelocity[i][1] *= -1;
				smoothRightPosition[i] *= -1;
			}

			//				for(int i = 0; i < smoothPath.length; i++) {
			//					System.out.println(smoothLeftVelocity[i][1] + " " + smoothRightVelocity[i][1] + " " 
			//							+ smoothLeftPosition[i] + " " + smoothRightPosition[i] + " " + heading[i][1]);
			//				}

			System.out.println(pathDist = calcPositionLength());

			avgDeltaDistance = calcSumLengthBetweenPoints();

			//				for(int i = 0; i < avgDeltaDistance.length; i++)
			//					System.out.println(avgDeltaDistance[i]);
		}
	}

	//	public static void main(String[] args) {
	//		MPGen2D mpGen = new MPGen2D(Waypoints.blueRightPathToPeg, 2.2, 0.02, 3.867227572441874);
	//		mpGen.calculate();
	//	}
}	