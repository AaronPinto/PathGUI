package util;

public final class PointMarker {
    public static final PointMarker DEFAULT = new PointMarker("", -1);
    private final String pathName;
    private final int pointIndex;

    public PointMarker(String pathName, int pointIndex) {
        this.pathName = pathName;
        this.pointIndex = pointIndex;
    }

    public String getPathName() {
        return pathName;
    }

    public int getPointIndex() {
        return pointIndex;
    }

    @Override
    public String toString() {
        return "pathName='" + pathName + '\'' + ", pointIndex=" + pointIndex;
    }
}
