package gateway.controller.clustering;

import gateway.services.clustering.EdgeClusteringService;
import gateway.utility.LineEdge;
import gateway.utility.cluster.LineCluster;
import org.codehaus.jettison.json.JSONException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.alg.clustering.KSpanningTreeClustering;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

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

    private boolean compareClusters(List<LineCluster> clusters1, List<LineCluster> clusters2) {
        if (clusters1.size() != clusters2.size()) {
            return false;
        }

        for (int i = 0; i < clusters1.size(); i++) {
            LineCluster nodeCluster1 = clusters1.get(i);
            LineCluster nodeCluster2 = clusters2.get(i);

            if (nodeCluster1.getClusterId() != nodeCluster2.getClusterId() ||
                    nodeCluster1.getTotalLength() != nodeCluster2.getTotalLength() ||
                    !compareEdges(nodeCluster1.getLines(), nodeCluster2.getLines())) {
                return false;
            }
        }

        return true;
    }

    private boolean compareEdges(List<LineEdge> nodes1, List<LineEdge> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        Set<String> nodeIds1 = nodes1.stream().map(LineEdge::getLineId).collect(Collectors.toSet());
        Set<String> nodeIds2 = nodes2.stream().map(LineEdge::getLineId).collect(Collectors.toSet());

        return nodeIds1.equals(nodeIds2);
    }

    public void partitionGraphWithThreshold(double threshold, String filepath, String outputPath) throws IOException, FactoryException, TransformException, JSONException {
        this.graph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
        LineEdge startEdge = EdgeClusteringService.findEdgeWithSmallestLength(this.graph);
        List<LineCluster> clusters1 = EdgeClusteringService.partitionGraphWithThreshold(this.graph, threshold, startEdge);
        EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);
    }

    public void partitionGraphWithNumClusters(int numClusters, String filepath, String outputPath) throws IOException, FactoryException, TransformException, JSONException {
        this.graph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
        List<LineCluster> clusters1 = EdgeClusteringService.partitionGraphWithNumClusters(this.graph, numClusters);
        EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);
    }

}
