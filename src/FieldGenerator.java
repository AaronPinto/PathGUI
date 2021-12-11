import java.awt.*;
import java.awt.geom.Line2D;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is used for storing and drawing all the field elements. Each element is stored in its own 2D array, and then it is added to a
 * LinkedHashMap. The LinkedHashMap is used to store all the field elements that need to be drawn. The values in the 2D arrays are in feet,
 * and the x-values are in the first columns followed by the y-values in the 2nd columns. All the 2D arrays are converted to new Polygon2D's
 * which are then drawn.
 */
final class FieldGenerator {
    private static final LinkedHashMap<String, double[][]> elements = new LinkedHashMap<>();

    // @formatter:off
    private final double[][] fieldBorder = new double[][]{
        {0.0, 0.0},
        {54.0, 0},
        {54.0, 27.0},
        {0, 27.0},
        {0.0, 0.0}
    };

    private final double[][] leftAutoLine = new double[][]{
        {0.0 + incToFt(120), 0.0},
        {0.0 + incToFt(120), 27.0},
        {0.0 + incToFt(120) + incToFt(2.0), 27.0},
        {0.0 + incToFt(120) + incToFt(2.0), 0.0}
    };

    private final double[][] redExZone = new double[][]{
        {0.0, 27.0 / 2 + incToFt(12.0)},
        {incToFt(36.0), 27.0 / 2 + incToFt(12.0)},
        {incToFt(36.0), 27.0 / 2 + incToFt(60.0)},
        {0.0, 27.0 / 2 + incToFt(60.0)},
        {0.0, 27.0 / 2 + incToFt(58.0)},
        {incToFt(36.0) - incToFt(2.0), 27.0 / 2 + incToFt(58.0)},
        {incToFt(36.0) - incToFt(2.0), 27.0 / 2 + incToFt(14.0)},
        {0.0, 27.0 / 2 + incToFt(14.0)}
    };

    private final double[][] redPcZone = new double[][]{
        {incToFt(140.0), 27.0 / 2 - incToFt(45.0 / 2)},
        {incToFt(98.0), 27.0 / 2 - incToFt(45.0 / 2)},
        {incToFt(98.0), 27.0 / 2 + incToFt(45.0 / 2)},
        {incToFt(140.0), 27.0 / 2 + incToFt(45.0 / 2)},
        {incToFt(140.0), 27.0 / 2 + incToFt(41.0 / 2)},
        {incToFt(100.0), 27.0 / 2 + incToFt(41.0 / 2)},
        {incToFt(100.0), 27.0 / 2 - incToFt(41.0 / 2)},
        {incToFt(140.0), 27.0 / 2 - incToFt(41.0 / 2)}
    };

    private final double[][] leftSwitchFrame = new double[][]{
        {incToFt(140.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2},
        {incToFt(140.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
        {incToFt(196.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
        {incToFt(196.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2},
        {incToFt(140.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2}
    };

    private final double[][] leftSwitchTop = new double[][]{
        {incToFt(144.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(38.25)},
        {incToFt(144.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(2.25)},
        {incToFt(192.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(2.25)},
        {incToFt(192.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2 - incToFt(38.25)}
    };

    private final double[][] leftSwitchBot = new double[][]{
        {incToFt(144.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(38.25)},
        {incToFt(144.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(2.25)},
        {incToFt(192.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(2.25)},
        {incToFt(192.0), 27.0 / 2 - (12.0 + incToFt(9.5)) / 2 + incToFt(38.25)}
    };

    private final double[][] redPlatZoneLineBot = new double[][]{
        {incToFt(196.0), incToFt(95.25)},
        {27.0, incToFt(95.25)},
        {27.0, incToFt(97.25)},
        {incToFt(196.0), incToFt(97.25)}
    };

    private final double[][] redPlat = new double[][]{
        {incToFt(261.47), incToFt(97.25)},
        {incToFt(261.47), incToFt(97.25) + incToFt(12.5362481) * 2 + 8.8},
        {27.0, incToFt(97.25) + incToFt(12.5362481) * 2 + 8.8},
        {27.0, incToFt(97.25)}
    };

    private final double[][] redPlatInner = new double[][]{
        {incToFt(261.47 + 12.5362481), 27.0 / 2 - 4.4},
        {incToFt(261.47 + 12.5362481), 27.0 / 2 + 4.4},
        {27.0, 27.0 / 2 + 4.4},
        {27.0, 27.0 / 2 - 4.4}
    };

    private final double[][] scaleBot = new double[][]{
        {incToFt(299.65), incToFt(71.57)},
        {incToFt(299.65), incToFt(71.57 + 36.0)},
        {incToFt(299.65 + 48.0), incToFt(71.57 + 36.0)},
        {incToFt(299.65 + 48.0), incToFt(71.57)}
    };

    private final double[][] scaleMid = new double[][]{
        {27.0 - incToFt(6.25), incToFt(97.25)},
        {27.0 - incToFt(6.25), incToFt(97.25) + 10.791666},
        {27.0 + incToFt(6.25), incToFt(97.25) + 10.791666},
        {27.0 + incToFt(6.25), incToFt(97.25)},
        {27.0 - incToFt(6.25), incToFt(97.25)}
    };

    private final double[][] nullTop = new double[][]{
        {incToFt(288.0), 0.0},
        {incToFt(288.0), incToFt(95.25)},
        {incToFt(288.0 + 72.0), incToFt(95.25)},
        {incToFt(288.0 + 72.0), 0.0},
        {incToFt(288.0 + 70.0), 0.0},
        {incToFt(288.0 + 70.0), incToFt(93.25)},
        {incToFt(288.0 + 2.0), incToFt(93.25)},
        {incToFt(288.0 + 2.0), 0.0}
    };

    private final double[][] blueTopPortal = new double[][]{
        {-0.001, 27.001},
        {-0.001, 24.5258333}, // 27.0 - 2.4741667
        {3.0, 27.001},
        {-0.001, 27.001}
    };

    private final double[][] redBotPortal = new double[][]{
        {54.001, -0.001},
        {54.001, 2.4741667},
        {51.0, -0.001},
        {54.001, -0.001}
    };

    private final double[][] topLeftPC = new double[][]{
        {incToFt(196.0), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
        {incToFt(196.0) + incToFt(13), 27.0 / 2 + (12.0 + incToFt(9.5)) / 2},
        {incToFt(196.0) + incToFt(13), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(13)},
        {incToFt(196.0), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(13)}
    };

    private final double[][] secondTopLeftPC = new double[][]{
        {incToFt(196.0), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(15.1 + 13)},
        {incToFt(196.0) + incToFt(13), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(15.1 + 13)},
        {incToFt(196.0) + incToFt(13), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(15.1 + 26)},
        {incToFt(196.0), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(15.1 + 26)}
    };

    private final double[][] thirdTopLeftPC = new double[][]{
        {incToFt(196.0), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(30.2 + 26)},
        {incToFt(196.0) + incToFt(13), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(30.2 + 26)},
        {incToFt(196.0) + incToFt(13), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(30.2 + 39)},
        {incToFt(196.0), (27.0 / 2 + (12.0 + incToFt(9.5)) / 2) - incToFt(30.2 + 39)}
    };

    private final double[][] redCableSwitchToPlat = new double[][]{
        {incToFt(196.0), incToFt(97.25) + (10.791666 / 2)},
        {incToFt(261.47), incToFt(97.25) + (10.791666 / 2)}
    };

    private final double[][] cableThroughScale = new double[][]{
        {27.0, 27.0},
        {27.0, 0}
    };
    // @formatter:on

    // Add all the elements to their respective data structures on object creation.
    {
        elements.put("leftAutoLine0", leftAutoLine);
        elements.put("rightAutoLine0", flipOverXAndY(leftAutoLine));
        elements.put("nullLineTop0", nullTop);
        elements.put("nullLineBot0", flipOverXAndY(nullTop));
        elements.put("redExZone0", redExZone);
        elements.put("blueExZone0", flipOverXAndY(redExZone));
        elements.put("redPcZone0", redPcZone); // Power cube
        elements.put("bluePcZone0", flipOverXAndY(redPcZone));
        elements.put("redPlatZoneLineTop0", flipOverXAxis(redPlatZoneLineBot));
        elements.put("redPlatZoneLineBot0", redPlatZoneLineBot);
        elements.put("bluePlatZoneLineTop0", flipOverXAndY(redPlatZoneLineBot));
        elements.put("bluePlatZoneLineBot0", flipOverXAxis(flipOverXAndY(redPlatZoneLineBot)));
        elements.put("topLeftPC0", topLeftPC);
        elements.put("bottomLeftPC0", flipOverXAxis(topLeftPC));
        elements.put("topRightPC0", flipOverXAndY(topLeftPC));
        elements.put("bottomRightPC0", flipOverXAxis(flipOverXAndY(topLeftPC)));
        elements.put("secondTopLeftPC0", secondTopLeftPC);
        elements.put("secondBottomLeftPC0", flipOverXAxis(secondTopLeftPC));
        elements.put("secondTopRightPC0", flipOverXAndY(secondTopLeftPC));
        elements.put("secondBottomRightPC0", flipOverXAxis(flipOverXAndY(secondTopLeftPC)));
        elements.put("thirdTopLeftPC0", thirdTopLeftPC);
        elements.put("thirdBottomLeftPC0", flipOverXAxis(thirdTopLeftPC));
        elements.put("thirdTopRightPC0", flipOverXAndY(thirdTopLeftPC));
        elements.put("thirdBottomRightPC0", flipOverXAxis(flipOverXAndY(thirdTopLeftPC)));
        elements.put("redCableSwitchToPlat", redCableSwitchToPlat);
        elements.put("blueCableSwitchToPlat", flipOverXAndY(redCableSwitchToPlat));
        elements.put("leftSwitchFrame", leftSwitchFrame);
        elements.put("rightSwitchFrame", flipOverXAndY(leftSwitchFrame));
        elements.put("leftSwitchTop0", leftSwitchTop);
        elements.put("leftSwitchBot0", leftSwitchBot);
        elements.put("rightSwitchTop0", flipOverXAndY(leftSwitchBot));
        elements.put("rightSwitchBot0", flipOverXAndY(leftSwitchTop));
        elements.put("redPlat0", redPlat);
        elements.put("bluePlat0", flipOverXAndY(redPlat));
        elements.put("redPlatInner0", redPlatInner);
        elements.put("bluePlatInner0", flipOverXAndY(redPlatInner));
        elements.put("cableThroughScale", cableThroughScale);
        elements.put("scaleMid0", scaleMid);
        elements.put("scaleTop0", flipOverXAndY(scaleBot));
        elements.put("scaleBot0", scaleBot);
        elements.put("blueTopPortal0", blueTopPortal);
        elements.put("blueBotPortal0", flipOverXAxis(blueTopPortal));
        elements.put("redTopPortal0", redBotPortal);
        elements.put("redBotPortal0", flipOverXAxis(redBotPortal));
        elements.put("fieldBorder", fieldBorder);
    }

    /**
     * This functions converts inches to feet
     *
     * @param inches the number of inches to convert
     *
     * @return the values of those inches in feet
     */
    private double incToFt(double inches) {
        return inches / 12.0;
    }

    /**
     * This function flips the x and y values over the y and x axes respectively.
     *
     * @param values the points to flip
     *
     * @return the 2D array with the flipped points
     */
    private double[][] flipOverXAndY(double[][] values) {
        double[][] temp = new double[values.length][values[0].length];

        for (int i = 0; i < values.length; i++) {
            temp[i][0] = 54.0 - values[i][0];
            temp[i][1] = 27.0 - values[i][1];
        }

        return temp;
    }

    /**
     * This function just flips the y values over the x-axis.
     *
     * @param values the values to flip
     *
     * @return the flipped values
     */
    private double[][] flipOverXAxis(double[][] values) {
        double[][] temp = new double[values.length][values[0].length];

        for (int i = 0; i < values.length; i++) {
            temp[i][0] = values[i][0];
            temp[i][1] = 27.0 - values[i][1];
        }

        return temp;
    }

    /**
     * This function is called every time the GUI updates. It redraws all the field elements and sets the colors accordingly.
     *
     * @param g2     the 2D graphics object for the JFrame
     * @param h      the height of the field
     * @param xScale the ratio for number of pixels per foot for the x-axis
     * @param yScale the ratio for number of pixels per foot for the y-axis
     */
    void plotField(Graphics2D g2, int h, double xScale, double yScale) {
        for (Map.Entry<String, double[][]> entry : elements.entrySet()) {
            String key = entry.getKey();
            // If the key contains a 0 we want to fill this field element.
            if (key.contains("0")) {
                double[] xPoints = new double[entry.getValue().length], yPoints = new double[entry.getValue().length];

                for (int i = 0; i < entry.getValue().length; i++) {
                    xPoints[i] = 30 + xScale * entry.getValue()[i][0];
                    yPoints[i] = h - 30 - yScale * entry.getValue()[i][1];
                }

                Polygon2D p = new Polygon2D(xPoints, yPoints, xPoints.length);

                if (key.contains("blue")) {
                    if (key.contains("Inner")) {
                        g2.setPaint(new Color(33, 63, 153));
                    } else {
                        g2.setPaint(Color.blue);
                    }
                } else if (key.contains("red")) {
                    if (key.contains("Inner")) {
                        g2.setPaint(new Color(163, 13, 13));
                    } else {
                        g2.setPaint(Color.red);
                    }
                } else if (key.contains("null")) {
                    g2.setPaint(Color.white);
                } else if (key.contains("Mid")) {
                    g2.setPaint(Color.lightGray);
                } else if (key.contains("PC")) {
                    g2.setPaint(Color.yellow);
                } else {
                    g2.setPaint(Color.black);
                }

                g2.fill(p);
                continue;
            }

            // Otherwise, we just want to draw the border.
            for (int i = 0; i < entry.getValue().length - 1; i++) {
                for (int j = 0; j < entry.getValue()[0].length - 1; j++) {
                    double x1 = 30 + xScale * entry.getValue()[i][j], x2 = 30 + xScale * entry.getValue()[i + 1][j];
                    double y1 = h - 30 - yScale * entry.getValue()[i][j + 1], y2 = h - 30 - yScale * entry.getValue()[i + 1][j + 1];

                    g2.setPaint(Color.black);
                    g2.draw(new Line2D.Double(x1, y1, x2, y2));
                }
            }
        }
    }
}
