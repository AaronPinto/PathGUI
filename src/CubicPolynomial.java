import org.apache.commons.math3.linear.*;

/**
 * ax^3 + bx^2
 */
public final class CubicPolynomial {
    private final double a;
    private final double b;
    RealMatrix A;
    RealVector B;
    RealVector x;

    CubicPolynomial(double d, double theta, double phi) {
        // Solve the linear equation A × X = B.
        double dcos = d * Math.cos(theta);

        // @formatter:off
        this.A = new Array2DRowRealMatrix(new double[][]{
                {dcos * dcos * dcos, dcos * dcos},
                {3 * (dcos * dcos), 2 * dcos}
        });
        this.B = new ArrayRealVector(new double[]{
                d * Math.sin(theta), Math.tan(phi)
        });
        // @formatter:on
        this.x = new LUDecomposition(this.A).getSolver().solve(B);

        this.a = this.x.getEntry(0);
        this.b = this.x.getEntry(1);

        System.out.println(a + "x^3 + " + b + "x^2");
    }

    public static void main(String[] args) {
        new CubicPolynomial(6.00444279957, Math.PI / 6.0, Math.PI / 4.0);
    }

    double calcPoint(double t) {
        return this.a * (t * t * t) + this.b * (t * t);
    }

    double calcFirstDeriv(double t) {
        return 3 * this.a * (t * t) + 2 * this.b * t;
    }

    double calcSecondDeriv(double t) {
        return 6 * this.a * t + 2 * this.b;
    }

    double calcThirdDeriv() {
        return 6 * this.a;
    }
}
