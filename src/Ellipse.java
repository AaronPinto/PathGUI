import org.apache.commons.math3.linear.*;

/**
 * ax^2 + by^2 + cx = 0
 */
public final class Ellipse {
    private final double a;
    private final double b;
    private final double c;
    RealMatrix A;
    RealVector B;
    RealVector x;

    Ellipse(double d, double theta, double phi) {
        // Solve the linear equation A × X = B.
        double dcos = d * Math.cos(theta), dsin = d * Math.sin(theta);

        // @formatter:off
        this.A = new Array2DRowRealMatrix(new double[][]{
                {dsin * dsin, dcos * dcos, dsin},
                {2 * dsin, 0, 1},
                {0, 2 * dcos, 0}
        });
        this.B = new ArrayRealVector(new double[]{
                0, 1.0 / Math.tan(phi), Math.tan(phi)
        });
        // @formatter:on
        this.x = new LUDecomposition(this.A).getSolver().solve(B);

        this.a = this.x.getEntry(0);
        this.b = this.x.getEntry(1);
        this.c = this.x.getEntry(2);

        System.out.println(a + "x^2 + " + b + "y^2 +" + c + "x = 0");
    }

    double calcPoint(double x, double y) {
        return this.a * (x * x) + this.b * (y * y) + this.c * x;
    }
}
