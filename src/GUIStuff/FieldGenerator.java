package GUIStuff;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FieldGenerator {
	void plotFieldElements(Graphics2D g2, int h, double xScale, double yScale) {
		HashMap<String, double[][]> elements = new HashMap<>();
		elements.put("blueAirship", new double[][]{
				{15.44166, 11.79},//3.42 ft
				{15.44166, 15.21},//5.9241666666666666666666666666666
				{12.47926, 16.90583},
				{9.517493, 15.21},//9.5174933333333333333333333333333
				{7.788327, 16.185},
				{9.517493, 15.21},
				{9.517493, 11.79},//2.9625 , 1.695833333
				{7.788327, 10.815},
				{9.517493, 11.79},
				{12.47926, 10.09417},//1.729166, 0.975
				{15.44166, 11.79},
		});
		elements.put("redAirship", new double[][]{
				{38.89167, 11.79},//3.42 ft
				{38.89167, 15.21},//5.9241666666666666666666666666666
				{41.85407, 16.90583},
				{44.81584, 15.21},//9.5174933333333333333333333333333
				{46.545, 16.185},
				{44.81584, 15.21},
				{44.81584, 11.79},//2.9625 , 1.695833333
				{46.545, 10.815},
				{44.81584, 11.79},
				{41.85407, 10.09417},//1.7291666, 0.975
				{38.89167, 11.79},
		});
		elements.put("blueAutoLine", new double[][]{
				{7.77500, 0.0},
				{7.77500, 27.0},
				{7.94166, 27.0},
				{7.94166, 0.0},
		});
		elements.put("redAutoLine", new double[][]{
				{46.55833, 0.0},
				{46.55833, 27.0},
				{46.39166, 27.0},
				{46.39166, 0.0},
		});
		elements.put("blueKeyLine", new double[][]{
				{0.0, 17.7316},//9.0375 from top 9.4425 from left outer line
				{9.4425, 27.0},//-0.23083 feet inner line -0.24083 feet
				{9.2016, 27.0},
				{0.0, 17.9625},
		});
		elements.put("redKeyLine", new double[][]{
				{54.33333, 17.7316},//9.0375 from top 9.4425 from left outer line
				{44.89083, 27.0},//-0.23083 feet inner line -0.24083 feet
				{45.13173, 27.0},
				{54.33333, 17.9625},
		});
		elements.put("blueLoadingZoneLine", new double[][]{
				{54.33333, 7.05083},
				{40.3025, 0.0},//-0.18666666666666666666666666666667 for y -0.37083333333333333333333333333333 for x
				{40.67333, 0.0},
				{54.33333, 6.864163},
		});
		elements.put("redLoadingZoneLine", new double[][]{
				{0.0, 7.05083},
				{14.03083, 0.0},//-0.18666666666666666666666666666667 for y -0.37083333333333333333333333333333 for x
				{13.659996, 0.0},
				{0.0, 6.864163},
		});
		elements.put("blueBoilerLine", new double[][]{
				{3.0814052429175126996746554085053, 27.0},
				{0.0, 24.051223364526279556078066141698},//-0.0094280904158206 feet for x +0.0553900311929462 feet for y
				{0.0, 27.0},
		});
		elements.put("redBoilerLine", new double[][]{
				{51.251928090415787300325344591495, 27.0},
				{54.33333333, 24.051223364526279556078066141698},//-0.0094280904158206 feet for x +0.0553900311929462 feet for y
				{54.33333333, 27.0},
		});
		elements.put("redLoadingLine", new double[][]{
				{0.0, 3.14583},//3.14583
				{6.16166, 0.0},
				{0.0, 0.0},//6.16166
		});
		elements.put("blueLoadingLine", new double[][]{
				{54.33333, 3.14583},//3.14583
				{48.171673, 0.0},
				{54.33333, 0.0},//6.16166
		});

		for(Map.Entry<String, double[][]> entry : elements.entrySet()) {
			if(entry.getKey().contains("Line")) {
				double[] xPoints = new double[entry.getValue().length], yPoints = new double[entry.getValue().length];
				IntStream.range(0, entry.getValue().length).forEach(i -> {
					xPoints[i] = 30 + xScale * entry.getValue()[i][0];
					yPoints[i] = h - 30 - yScale * entry.getValue()[i][1];
				});
				Polygon2D p = new Polygon2D(xPoints, yPoints, xPoints.length);
				if(entry.getKey().contains("blue"))
					g2.setPaint(Color.blue);
				else
					g2.setPaint(Color.red);
				g2.fill(p);
				continue;
			}

			for(int i = 0; i < entry.getValue().length - 1; i++)
				for(int j = 0; j < entry.getValue()[0].length - 1; j++) {
					double x1 = 30 + xScale * entry.getValue()[i][j];
					double x2 = 30 + xScale * entry.getValue()[i + 1][j];
					double y1 = h - 30 - yScale * entry.getValue()[i][j + 1];
					double y2 = h - 30 - yScale * entry.getValue()[i + 1][j + 1];

					g2.setPaint(Color.black);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
				}
		}
	}
}