package util;

public final class Waypoint {
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
        this.rad = rad;
        this.v = v;
        this.a = a;
        this.deg = Math.toDegrees(rad);
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
        return x + ", " + y + ", " + deg + ", " + v + ", " + a;
    }

    /**
     * @return a String with the yaw (deg), velocity and accel
     */
    public String getYawVelAcc() {
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
    public void setAngleVelAcc(double deg, double v, double a) {
        this.rad = Math.toRadians(deg);
        this.deg = deg;
        this.v = v;
        this.a = a;
    }

    /**
     * @return the angle in radians
     */
    public double getRad() {
        return rad;
    }

    /**
     * @param x the new x position
     * @param y the new y position
     */
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setRad(double rad) {
        this.rad = rad;
        this.deg = Math.toDegrees(rad);
    }

    public double getV() {
        return v;
    }

    public double getA() {
        return a;
    }

    public void setV(double v) {
        this.v = v;
    }
}
