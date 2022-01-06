package util;

public record PointMarker(String pathName, int pointIndex) {
    public static final PointMarker DEFAULT = new PointMarker("", -1);

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
