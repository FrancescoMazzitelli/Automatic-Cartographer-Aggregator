package gateway.utility;

import org.locationtech.jts.geom.*;

public class PolygonNode extends Point {
    private String polygonId;
    private Point centroid;
    private double shapeLeng;
    private double shapeArea;
    private Geometry associatedPolygon;

    public PolygonNode(String polygonId, Point centroid, double shapeLeng, double shapeArea, Geometry associatedPolygon) {
        super(createCoordinateSequence(centroid.getCoordinate()), centroid.getFactory());
        this.polygonId = polygonId;
        this.shapeLeng = shapeLeng;
        this.shapeArea = shapeArea;
        this.associatedPolygon = associatedPolygon;
    }

    private static CoordinateSequence createCoordinateSequence(Coordinate coordinate) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = {coordinate};
        return geometryFactory.getCoordinateSequenceFactory().create(coordinates);
    }

    public String getPolygonId() {
        return polygonId;
    }

    @Override
    public Point getCentroid() {
        return centroid;
    }

    public double getShapeLeng() {
        return shapeLeng;
    }

    public double getShapeArea() {
        return shapeArea;
    }

    public Geometry getAssociatedPolygon() {
        return associatedPolygon;
    }

    public void setPolygonId(String polygonId) {
        this.polygonId = polygonId;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }

    public void setShapeLeng(double shapeLeng) {
        this.shapeLeng = shapeLeng;
    }

    public void setShapeArea(double shapeArea) {
        this.shapeArea = shapeArea;
    }

    public void setAssociatedPolygon(Geometry associatedPolygon) {
        this.associatedPolygon = associatedPolygon;
    }
}
