import org.apache.commons.math3.linear.*;

/**
 * @author https://github.com/AtsushiSakai/PythonRobotics/blob/master/PathPlanning/QuinticPolynomialsPlanner/quinticPolynomialsPlanner.py
 * Modified by: Aaron Pinto ax^5 + bx^4 + cx^3 + dx^2 + fx + g
 */
public final class QuinticPolynomial {
    private final double a;
    private final double b;
    private final double c;
    private final double d;
    private final double f;
    private final double g;

    QuinticPolynomial(double xs, double vxs, double axs, double xe, double vxe, double axe, double T) {
        this.g = xs;
        this.f = vxs;
        this.d = axs / 2.0;

        double T2 = T * T, T3 = T * T * T, T4 = T * T * T * T, T5 = T * T * T * T * T;

        // Solve the linear equation A × X = B. @formatter:off
        RealMatrix a1 = new Array2DRowRealMatrix(new double[][]{
                {T3, T4, T5},
                {3.0 * T2, 4.0 * T3, 5.0 * T4},
                {6.0 * T, 12.0 * T2, 20.0 * T3}
        });
        RealVector b1 = new ArrayRealVector(new double[]{
                xe - this.g - this.f * T - this.d * T2, vxe - this.f - 2.0 * this.d * T, axe - 2.0 * this.d
        });
        // @formatter:on
        RealVector x = new LUDecomposition(a1).getSolver().solve(b1);

        this.c = x.getEntry(0);
        this.b = x.getEntry(1);
        this.a = x.getEntry(2);
    }

    void printCoeffs() {
        System.out.println(this.a + "x^5 + " + this.b + "x^4 + " + this.c + "x^3 + " + this.d + "x^2 + " + this.f + "x + " + this.g);
    }

    double calcPoint(double t) {
        return this.a * (t * t * t * t * t) + this.b * (t * t * t * t) + this.c * (t * t * t) + this.d * (t * t) + this.f * t + this.g;
    }

    double calcFirstDeriv(double t) {
        return 5 * this.a * (t * t * t * t) + 4 * this.b * (t * t * t) + 3 * this.c * (t * t) + 2 * this.d * t + this.f;
    }

    double calcSecondDeriv(double t) {
        return 20 * this.a * (t * t * t) + 12 * this.b * (t * t) + 6 * this.c * t + 2 * this.d;
    }

    double calcThirdDeriv(double t) {
        return 60 * this.a * (t * t) + 24 * this.b * t + 6 * this.c;
    }
}
