public final class Waypoint {
    private double x;
    private double y;
    private double yaw;
    private double v;
    private double a;

    Waypoint(double x, double y, Waypoint p) {
        this(x, y, p.yaw, p.v, p.a);
    }

    Waypoint(Waypoint p) {
        this(p.x, p.y, p.yaw, p.v, p.a);
    }

    /**
     * @param x   the x position
     * @param y   the y position
     * @param yaw the angle in radians
     * @param v   the velocity
     * @param a   the acceleration
     */
    Waypoint(double x, double y, double yaw, double v, double a) {
        this.x = x;
        this.y = y;
        this.yaw = yaw;
        this.v = v;
        this.a = a;
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
        return x + ", " + y + ", " + Math.toDegrees(yaw) + ", " + v + ", " + a;
    }

    /**
     * @return a String with the yaw (deg), velocity and accel
     */
    public String getYawVelAcc() {
        return Math.toDegrees(yaw) + ", " + v + ", " + a;
    }

    /**
     * @param yaw the new angle in degrees
     * @param v   the new velocity
     * @param a   the new acceleration
     */
    public void setYawVelAcc(double yaw, double v, double a) {
        this.yaw = Math.toRadians(yaw);
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

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * @return the angle in radians
     */
    public double getYaw() {
        return yaw;
    }

    public double getV() {
        return v;
    }

    public double getA() {
        return a;
    }
}
