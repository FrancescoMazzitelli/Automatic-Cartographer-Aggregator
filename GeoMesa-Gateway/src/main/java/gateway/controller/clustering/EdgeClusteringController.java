package gateway.controller.clustering;

import gateway.services.clustering.EdgeClusteringService;
import gateway.utility.LineEdge;
import gateway.utility.cluster.LineCluster;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EdgeClusteringController {
    private Graph<LineEdge, DefaultWeightedEdge> graph;

    public EdgeClusteringController() throws IOException {
    }
    public void printGraph() throws IOException {
        EdgeClusteringService.printGraph(this.graph);
    }

    private boolean compareLineClusters(List<LineCluster> clusters1, List<LineCluster> clusters2) {
        if (clusters1.size() != clusters2.size()) {
            return false;
        }

        for (int i = 0; i < clusters1.size(); i++) {
            LineCluster nodeCluster1 = clusters1.get(i);
            LineCluster nodeCluster2 = clusters2.get(i);

            if (nodeCluster1.getClusterId() != nodeCluster2.getClusterId() ||
                    nodeCluster1.getTotalWeight() != nodeCluster2.getTotalWeight() ||
                    !compareLines(nodeCluster1.getLines(), nodeCluster2.getLines())) {
                return false;
            }
        }

        return true;
    }

    private boolean compareLines(List<LineEdge> nodes1, List<LineEdge> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        Set<String> nodeIds1 = nodes1.stream().map(LineEdge::getLineId).collect(Collectors.toSet());
        Set<String> nodeIds2 = nodes2.stream().map(LineEdge::getLineId).collect(Collectors.toSet());

        return nodeIds1.equals(nodeIds2);
    }

    public void partitionGraphWithThreshold(double threshold, String filepath, String outputPath) throws IOException, JSONException {
        this.graph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
        LineEdge startEdge = EdgeClusteringService.findEdgeWithSmallestLength(this.graph);
        List<LineCluster> clusters1 = EdgeClusteringService.partitionGraphWithThreshold(this.graph, threshold, startEdge, "length");
        EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);

        int maxIterations = 5; // Imposta il numero massimo di iterazioni desiderato
        int currentIteration = 0;

        while (currentIteration < maxIterations) {
            Graph<LineEdge, DefaultWeightedEdge> graph = EdgeClusteringService.convertGeoJSONToGraph(outputPath);
            LineEdge startEdge1 = EdgeClusteringService.findEdgeWithSmallestLength(graph);
            List<LineCluster> clusters2 = EdgeClusteringService.partitionGraphWithThreshold(graph, threshold, startEdge1, "length");
            EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters2, outputPath);

            if (compareLineClusters(clusters1, clusters2)) {
                break;
            } else {
                clusters1 = clusters2;
                currentIteration++;
            }
        }
    }

    public void partitionGraphWithNumClusters(int numClusters, String filepath, String outputPath) throws IOException, JSONException {
        this.graph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
        List<LineCluster> clusters1 = EdgeClusteringService.partitionGraphWithNumClusters(this.graph, numClusters);
        EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);
    }

    public JSONObject getCRS(){
        return EdgeClusteringService.getCrsFile();
    }
}
