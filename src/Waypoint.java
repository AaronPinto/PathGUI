public class Waypoint {
    double x, y, yaw, v, a;

    Waypoint(double x, double y, Waypoint p) {
        this(x, y, p.yaw, p.v, p.a);
    }

    Waypoint(Waypoint p) {
        this(p.x, p.y, p.yaw, p.v, p.a);
    }

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

    public String toString() {
        return x + ", " + y + ", " + yaw + ", " + v + ", " + a;
    }
}
