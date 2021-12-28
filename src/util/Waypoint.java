package util;

import java.io.Serializable;

public final class Waypoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private double rad; // radians
    private double deg; // degrees
    private double v;
    private double a;

    public Waypoint(double x, double y, Waypoint p) {
        this(x, y, p.rad, p.v, p.a);
    }

    public Waypoint(Waypoint p) {
        this(p.x, p.y, p.rad, p.v, p.a);
    }

    /**
     * @param x   the x position
     * @param y   the y position
     * @param rad the angle in radians
     * @param v   the velocity
     * @param a   the acceleration
     */
    public Waypoint(double x, double y, double rad, double v, double a) {
        this.x = x;
        this.y = y;
        this.v = v;
        this.a = a;

        if (Math.abs(rad) > 2.0 * Math.PI) { // Assume degrees if >2PI, but alert user
            this.rad = Math.toRadians(rad);
            this.deg = rad;
            throw new RuntimeException("Processed input angle as degrees!");
        } else {
            this.rad = rad;
            this.deg = Math.toDegrees(rad);
        }
    }

    @Override
    public boolean equals(Object p) {
        if (p instanceof Waypoint) {
            Waypoint temp = (Waypoint) p;

            return this.x == temp.x && this.y == temp.y;
        }

        return super.equals(p);
    }

    /**
     * @return a String with the x and y positions, yaw (deg), velocity, and accel
     */
    @Override
    public String toString() {
        return x + ", " + y + ", Math.toRadians(" + deg + "), " + v + ", " + a;
    }

    /**
     * @return a String with the yaw (deg), velocity and accel
     */
    public String getDegVelAcc() {
        return deg + ", " + v + ", " + a;
    }

    /**
     * @return the angle in degrees
     */
    public double getDeg() {
        return deg;
    }

    public void setDeg(double deg) {
        this.deg = deg;
        this.rad = Math.toRadians(deg);
    }

    /**
     * @param deg the new angle in degrees
     * @param v   the new velocity
     * @param a   the new acceleration
     */
    public void setDegVelAcc(double deg, double v, double a) {
        this.rad = Math.toRadians(deg);
        this.deg = deg;
        this.v = v;
        this.a = a;
    }

    /**
     * @param x the new x position
     * @param y the new y position
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void incrementPosition(double x_inc, double y_inc) {
        this.x += x_inc;
        this.y += y_inc;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * @return the angle in radians
     */
    public double getRad() {
        return rad;
    }

    public void setRad(double rad) {
        this.rad = rad;
        this.deg = Math.toDegrees(rad);
    }

    public double getV() {
        return v;
    }

    public void setV(double v) {
        this.v = v;
    }

    public double getA() {
        return a;
    }
}
