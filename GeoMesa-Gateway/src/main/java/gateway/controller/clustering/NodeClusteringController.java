package gateway.controller.clustering;

import gateway.services.clustering.NodeClusteringService;
import gateway.utility.cluster.NodeCluster;
import gateway.utility.PolygonNode;
import org.codehaus.jettison.json.JSONException;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeClusteringController {
    private Graph<PolygonNode, DefaultWeightedEdge> graph;

    public NodeClusteringController() throws IOException {
    }
    public void printGraph() throws IOException {
        NodeClusteringService.printGraph(this.graph);
    }

    private boolean compareClusters(List<NodeCluster> clusters1, List<NodeCluster> clusters2) {
        if (clusters1.size() != clusters2.size()) {
            return false;
        }

        for (int i = 0; i < clusters1.size(); i++) {
            NodeCluster nodeCluster1 = clusters1.get(i);
            NodeCluster nodeCluster2 = clusters2.get(i);

            if (nodeCluster1.getClusterId() != nodeCluster2.getClusterId() ||
                    nodeCluster1.getTotalFamilies() != nodeCluster2.getTotalFamilies() ||
                    !compareNodes(nodeCluster1.getNodes(), nodeCluster2.getNodes())) {
                return false;
            }
        }

        return true;
    }

    private boolean compareNodes(List<PolygonNode> nodes1, List<PolygonNode> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        Set<String> nodeIds1 = nodes1.stream().map(PolygonNode::getPolygonId).collect(Collectors.toSet());
        Set<String> nodeIds2 = nodes2.stream().map(PolygonNode::getPolygonId).collect(Collectors.toSet());

        return nodeIds1.equals(nodeIds2);
    }

    public void partitionGraphWithThreshold(double threshold, String filepath, String outputPath) throws FactoryException, TransformException, JSONException, IOException {

        this.graph = NodeClusteringService.convertGeoJSONToGraph(filepath);
        PolygonNode startNode = NodeClusteringService.findNodeWithSmallestArea(this.graph);
        List<NodeCluster> clusters1 = NodeClusteringService.partitionGraphWithThreshold(this.graph, threshold, startNode);
        NodeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);

        int maxIterations = 5; // Imposta il numero massimo di iterazioni desiderato
        int currentIteration = 0;

        while (currentIteration < maxIterations) {
            Graph<PolygonNode, DefaultWeightedEdge> graph = NodeClusteringService.convertGeoJSONToGraph(outputPath);
            PolygonNode startNode1 = NodeClusteringService.findNodeWithSmallestArea(graph);
            List<NodeCluster> clusters2 = NodeClusteringService.partitionGraphWithThreshold(graph, threshold, startNode1);
            NodeClusteringService.createAndSavePartitionedGeoJSON(clusters2, outputPath);

            if (compareClusters(clusters1, clusters2)) {
                break;
            } else {
                clusters1 = clusters2;
                currentIteration++;
            }
        }
    }

    public void partitionGraphWithNumClusters(int numClsters, String filepath, String outputPath) throws FactoryException, TransformException, JSONException, IOException {
        this.graph = NodeClusteringService.convertGeoJSONToGraph(filepath);
        List<NodeCluster> clusters1 = NodeClusteringService.partitionGraphWithNumClusters(this.graph, numClsters);
        NodeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);
    }
}
