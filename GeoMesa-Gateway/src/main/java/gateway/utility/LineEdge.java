package gateway.utility;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.locationtech.jts.geom.*;


public class LineEdge extends Point {

    private String lineId;
    private String lineName;
    private Geometry lineString;
    private double weight;
    private double families;
    private Point centroid;

    public LineEdge(Point centroid, double weight, double families, String lineId, String lineName, Geometry lineString) {
        super(createCoordinateSequence(centroid.getCoordinate()), centroid.getFactory());
        this.lineId = lineId;
        this.lineName = lineName;
        this.weight = weight;
        this.families = families;
        this.lineString = lineString;
    }

    private static CoordinateSequence createCoordinateSequence(Coordinate coordinate) {
        GeometryFactory geometryFactory = new GeometryFactory();
        Coordinate[] coordinates = {coordinate};
        return geometryFactory.getCoordinateSequenceFactory().create(coordinates);
    }

    public String getLineId() {
        return lineId;
    }

    public String getLineName() {
        return lineName;
    }

    public Geometry getLineString() {
        return lineString;
    }

    public double getWeight() {
        return weight;
    }

    public double getFamilies() {
        return families;
    }

    @Override
    public Point getCentroid() {
        return centroid;
    }

    public void setLineId(String lineId) {
        this.lineId = lineId;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public void setLineString(Geometry lineString) {
        this.lineString = lineString;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void setFamilies(double families) {
        this.families = families;
    }

    public void setCentroid(Point centroid) {
        this.centroid = centroid;
    }
}
