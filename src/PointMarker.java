public final class PointMarker {
    static final PointMarker DEFAULT = new PointMarker("", -1);

    String pathName;
    int pointIndex;

    PointMarker(String pathName, int pointIndex) {
        this.pathName = pathName;
        this.pointIndex = pointIndex;
    }

    @Override
    public String toString() {
        return "pathName='" + pathName + '\'' + ", pointIndex=" + pointIndex;
    }
}
