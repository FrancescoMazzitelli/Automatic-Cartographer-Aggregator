package gateway.utility.cluster;

import gateway.utility.LineEdge;
import org.locationtech.jts.geom.Geometry;

import java.util.ArrayList;
import java.util.List;

public class LineCluster {

    private int clusterId;
    private double totalLength;
    private List<LineEdge> lines;
    private List<String> lineNames;


    public LineCluster(int clusterId, double totalLength, List<LineEdge> lines) {
        this.clusterId = clusterId;
        this.totalLength = totalLength;
        this.lines = lines;
        this.lineNames = extractLineNames();
    }

    private List<String> extractLineNames(){
        List<String> lineNames = new ArrayList<>();
        for(LineEdge line : lines){
            lineNames.add(line.getLineName());
        }
        return lineNames;
    }

    public int getClusterId() {
        return clusterId;
    }

    public double getTotalLength() {
        return totalLength;
    }


    public List<LineEdge> getLines() {
        return lines;
    }

    public List<String> getLineNames() {
        return lineNames;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public void setTotalLength(double totalLength) {
        this.totalLength = totalLength;
    }

    public void setLines(List<LineEdge> lines) {
        this.lines = lines;
    }

    public void setLineNames(List<String> lineNames) {
        this.lineNames = lineNames;
    }
}
