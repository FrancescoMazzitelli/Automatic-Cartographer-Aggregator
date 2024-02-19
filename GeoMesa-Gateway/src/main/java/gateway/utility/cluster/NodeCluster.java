package gateway.utility.cluster;

import gateway.utility.PolygonNode;

import java.util.List;

public class NodeCluster {

    private int clusterId;
    private double totalFamilies;
    private List<PolygonNode> nodes;

    public NodeCluster(int clusterId, double totalFamilies, List<PolygonNode> nodes) {
        this.clusterId = clusterId;
        this.totalFamilies = totalFamilies;
        this.nodes = nodes;
    }

    public int getClusterId() {
        return clusterId;
    }

    public double getTotalFamilies() {
        return totalFamilies;
    }

    public List<PolygonNode> getNodes() {
        return nodes;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public void setTotalFamilies(double totalFamilies) {
        this.totalFamilies = totalFamilies;
    }

    public void setNodes(List<PolygonNode> nodes) {
        this.nodes = nodes;
    }
}
