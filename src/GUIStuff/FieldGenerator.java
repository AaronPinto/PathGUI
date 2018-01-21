package GUIStuff;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class FieldGenerator {
	ArrayList<Polygon2D> invalidAreas = new ArrayList<>();
	private LinkedHashMap<String, double[][]> elements = new LinkedHashMap<>();

	private double[][] fieldBorder = new double[][]{
			{0.0, 0.0},
			{54.0, 0},
			{54.0, 27.0},
			{0, 27.0},
			{0.0, 0.0},
	};

	private double[][] leftAutoLine = new double[][]{
			{0.0 + incToFt(120), 0.0},
			{0.0 + incToFt(120), 27.0},
			{0.0 + incToFt(120) + incToFt(2.0), 27.0},
			{0.0 + incToFt(120) + incToFt(2.0), 0.0},
	};
	private double[][] redExZone = new double[][]{
			{0.0, 27.0 / 2 + incToFt(12.0)},
			{incToFt(36.0), 27.0 / 2 + incToFt(12.0)},
			{incToFt(36.0), 27.0 / 2 + incToFt(60.0)},
			{0.0, 27.0 / 2 + incToFt(60.0)},
			{0.0, 27.0 / 2 + incToFt(58.0)},
			{incToFt(36.0) - incToFt(2.0), 27.0 / 2 + incToFt(58.0)},
			{incToFt(36.0) - incToFt(2.0), 27.0 / 2 + incToFt(14.0)},
			{0.0, 27.0 / 2 + incToFt(14.0)},
	};
	private double[][] redPcZone = new double[][]{
			{incToFt(140.0), 27.0 / 2 - incToFt(45.0 / 2)},
			{incToFt(98.0), 27.0 / 2 - incToFt(45.0 / 2)},
			{incToFt(98.0), 27.0 / 2 + incToFt(45.0 / 2)},
			{incToFt(140.0), 27.0 / 2 + incToFt(45.0 / 2)},
			{incToFt(140.0), 27.0 / 2 + incToFt(41.0 / 2)},
			{incToFt(100.0), 27.0 / 2 + incToFt(41.0 / 2)},
			{incToFt(100.0), 27.0 / 2 - incToFt(41.0 / 2)},
			{incToFt(140.0), 27.0 / 2 - incToFt(41.0 / 2)},
	};
	private double[][] leftSwitchFrame = new double[][]{
			{incToFt(140.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2},
			{incToFt(140.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
			{incToFt(196.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
			{incToFt(196.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2},
			{incToFt(140.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2},
	};
	private double[][] leftSwitchTop = new double[][]{
			{incToFt(144.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(38.25)},
			{incToFt(144.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(2.25)},
			{incToFt(192.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(2.25)},
			{incToFt(192.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(38.25)},
	};
	private double[][] leftSwitchBot = new double[][]{
			{incToFt(144.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(38.25)},
			{incToFt(144.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(2.25)},
			{incToFt(192.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(2.25)},
			{incToFt(192.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(38.25)},
	};
	private double[][] redPlatZoneLineBot = new double[][]{
			{incToFt(196.0), incToFt(95.25)},
			{27.0, incToFt(95.25)},
			{27.0, incToFt(97.25)},
			{incToFt(196.0), incToFt(97.25)},
	};
	private double[][] redPlat = new double[][]{
			{incToFt(261.47), incToFt(97.25)},
			{incToFt(261.47), incToFt(97.25) + incToFt(12.5362481) * 2 + 8.8},
			{27.0, incToFt(97.25) + incToFt(12.5362481) * 2 + 8.8},
			{27.0, incToFt(97.25)},
	};
	private double[][] redPlatInner = new double[][]{
			{incToFt(261.47 + 12.5362481), 27.0 / 2 - 4.4},
			{incToFt(261.47 + 12.5362481), 27.0 / 2 + 4.4},
			{27.0, 27.0 / 2 + 4.4},
			{27.0, 27.0 / 2 - 4.4},
	};
	private double[][] scaleBot = new double[][]{
			{incToFt(299.65), incToFt(71.57)},
			{incToFt(299.65), incToFt(71.57 + 36.0)},
			{incToFt(299.65 + 48.0), incToFt(71.57 + 36.0)},
			{incToFt(299.65 + 48.0), incToFt(71.57)},
	};
	private double[][] scaleMid = new double[][]{
			{27.0 - incToFt(6.25), incToFt(97.25)},
			{27.0 - incToFt(6.25), incToFt(97.25) + 10.791666},
			{27.0 + incToFt(6.25), incToFt(97.25) + 10.791666},
			{27.0 + incToFt(6.25), incToFt(97.25)},
			{27.0 - incToFt(6.25), incToFt(97.25)},
	};
	private double[][] nullTop = new double[][]{
			{incToFt(288.0), 0.0},
			{incToFt(288.0), incToFt(95.25)},
			{incToFt(288.0 + 72.0), incToFt(95.25)},
			{incToFt(288.0 + 72.0), 0.0},
			{incToFt(288.0 + 70.0), 0.0},
			{incToFt(288.0 + 70.0), incToFt(93.25)},
			{incToFt(288.0 + 2.0), incToFt(93.25)},
			{incToFt(288.0 + 2.0), 0.0},
	};
	private double[][] blueTopPortal = new double[][]{
			{-0.001, 27.001},
			{-0.001, 24.5258333},//27.0 - 2.4741667
			{3.0, 27.001},
			{-0.001, 27.001},
	};
	private double[][] redBotPortal = new double[][]{
			{54.001, -0.001},
			{54.001, 2.4741667},
			{51.0, -0.001},
			{54.001, -0.001},
	};

	{
		LinkedHashMap<String, double[][]> invAreas = new LinkedHashMap<>();
		invAreas.put("field border", fieldBorder);
		invAreas.put("left switch frame", leftSwitchFrame);
		invAreas.put("right switch frame", flipOverXAndY(leftSwitchFrame));
		invAreas.put("scale", scaleMid);
		invAreas.put("blue top portal", blueTopPortal);
		invAreas.put("blue bot portal", flipOverXAxis(blueTopPortal));
		invAreas.put("red bot portal", redBotPortal);
		invAreas.put("red top portal", flipOverXAxis(redBotPortal));

		invAreas.forEach((key, value) -> {
			double[] xPoints = new double[value.length], yPoints = new double[value.length];
			IntStream.range(0, value.length).forEach(i -> {
				xPoints[i] = value[i][0];
				yPoints[i] = value[i][1];
			});
			invalidAreas.add(new Polygon2D(xPoints, yPoints, xPoints.length, key));
		});

		elements.put("leftAutoLine0", leftAutoLine);
		elements.put("rightAutoLine0", flipOverXAndY(leftAutoLine));
		elements.put("nullLineTop0", nullTop);
		elements.put("nullLineBot0", flipOverXAndY(nullTop));
		elements.put("redExZone0", redExZone);
		elements.put("blueExZone0", flipOverXAndY(redExZone));
		elements.put("redPcZone0", redPcZone);
		elements.put("bluePcZone0", flipOverXAndY(redPcZone));
		elements.put("leftSwitchFrame", leftSwitchFrame);
		elements.put("rightSwitchFrame", flipOverXAndY(leftSwitchFrame));
		elements.put("leftSwitchTop0", leftSwitchTop);
		elements.put("leftSwitchBot0", leftSwitchBot);
		elements.put("rightSwitchTop0", flipOverXAndY(leftSwitchBot));
		elements.put("rightSwitchBot0", flipOverXAndY(leftSwitchTop));
		elements.put("redPlatZoneLineTop0", flipOverXAxis(redPlatZoneLineBot));
		elements.put("redPlatZoneLineBot0", redPlatZoneLineBot);
		elements.put("bluePlatZoneLineTop0", flipOverXAndY(redPlatZoneLineBot));
		elements.put("bluePlatZoneLineBot0", flipOverXAxis(flipOverXAndY(redPlatZoneLineBot)));
		elements.put("redPlat0", redPlat);
		elements.put("bluePlat0", flipOverXAndY(redPlat));
		elements.put("redPlatInner0", redPlatInner);
		elements.put("bluePlatInner0", flipOverXAndY(redPlatInner));
		elements.put("scaleMid0", scaleMid);
		elements.put("scaleTop0", flipOverXAndY(scaleBot));
		elements.put("scaleBot0", scaleBot);
		elements.put("blueTopPortal0", blueTopPortal);
		elements.put("blueBotPortal0", flipOverXAxis(blueTopPortal));
		elements.put("redTopPortal0", redBotPortal);
		elements.put("redBotPortal0", flipOverXAxis(redBotPortal));
		elements.put("fieldBorder", fieldBorder);
	}

	private double incToFt(double inches) {
		return inches / 12.0;
	}

	private double[][] flipOverXAndY(double[][] values) {
		double[][] temp = new double[values.length][values[0].length];
		IntStream.range(0, values.length).forEach(i -> {
			temp[i][0] = 54.0 - values[i][0];
			temp[i][1] = 27.0 - values[i][1];
		});
		return temp;
	}

	private double[][] flipOverXAxis(double[][] values) {
		double[][] temp = new double[values.length][values[0].length];
		IntStream.range(0, values.length).forEach(i -> {
			temp[i][0] = values[i][0];
			temp[i][1] = 27.0 - values[i][1];
		});
		return temp;
	}

	void plotField(Graphics2D g2, int h, double xScale, double yScale) {
		for(Map.Entry<String, double[][]> entry : elements.entrySet()) {
			String key = entry.getKey();
			if(key.contains("0")) {
				double[] xPoints = new double[entry.getValue().length], yPoints = new double[entry.getValue().length];
				IntStream.range(0, entry.getValue().length).forEach(i -> {
					xPoints[i] = 30 + xScale * entry.getValue()[i][0];
					yPoints[i] = h - 30 - yScale * entry.getValue()[i][1];
				});
				Polygon2D p = new Polygon2D(xPoints, yPoints, xPoints.length, entry.getKey());
				if(key.contains("blue"))
					if(key.contains("Inner"))
						g2.setPaint(new Color(33, 63, 153));
					else
						g2.setPaint(Color.blue);
				else if(key.contains("red"))
					if(key.contains("Inner"))
						g2.setPaint(new Color(163, 13, 13));
					else
						g2.setPaint(Color.red);
				else if(key.contains("null"))
					g2.setPaint(Color.white);
				else if(key.contains("Mid"))
					g2.setPaint(Color.lightGray);
				else
					g2.setPaint(Color.black);
				g2.fill(p);
				continue;
			}

			for(int i = 0; i < entry.getValue().length - 1; i++)
				for(int j = 0; j < entry.getValue()[0].length - 1; j++) {
					double x1 = 30 + xScale * entry.getValue()[i][j], x2 = 30 + xScale * entry.getValue()[i + 1][j];
					double y1 = h - 30 - yScale * entry.getValue()[i][j + 1], y2 = h - 30 - yScale * entry.getValue()[i + 1][j + 1];

					g2.setPaint(Color.black);
					g2.draw(new Line2D.Double(x1, y1, x2, y2));
				}
		}
	}
}