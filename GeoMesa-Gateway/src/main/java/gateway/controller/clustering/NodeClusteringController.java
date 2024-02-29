package gateway.controller.clustering;

import gateway.services.clustering.EdgeClusteringService;
import gateway.services.clustering.NodeClusteringService;
import gateway.utility.LineEdge;
import gateway.utility.cluster.LineCluster;
import gateway.utility.cluster.NodeCluster;
import gateway.utility.PolygonNode;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.opengis.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.*;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class NodeClusteringController {
    private Graph<PolygonNode, DefaultWeightedEdge> polygonGraph;
    private Graph<LineEdge, DefaultWeightedEdge> lineGraph;
    private String filepath;

    public NodeClusteringController() throws IOException {
    }
    public void printGraph() throws IOException {
        NodeClusteringService.printGraph(this.polygonGraph);
    }

    private boolean compareNodeClusters(List<NodeCluster> clusters1, List<NodeCluster> clusters2) {
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

    private boolean compareNodes(List<PolygonNode> nodes1, List<PolygonNode> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        Set<String> nodeIds1 = nodes1.stream().map(PolygonNode::getPolygonId).collect(Collectors.toSet());
        Set<String> nodeIds2 = nodes2.stream().map(PolygonNode::getPolygonId).collect(Collectors.toSet());

        return nodeIds1.equals(nodeIds2);
    }

    private boolean compareLines(List<LineEdge> nodes1, List<LineEdge> nodes2) {
        if (nodes1.size() != nodes2.size()) {
            return false;
        }

        Set<String> nodeIds1 = nodes1.stream().map(LineEdge::getLineId).collect(Collectors.toSet());
        Set<String> nodeIds2 = nodes2.stream().map(LineEdge::getLineId).collect(Collectors.toSet());

        return nodeIds1.equals(nodeIds2);
    }

    private String checkGeometryType(String filepath) throws IOException {
        try (InputStream inputStream = new FileInputStream(filepath)) {
            FeatureJSON featureJSON = new FeatureJSON();
            FileReader reader = new FileReader(filepath);
            BufferedReader br = new BufferedReader(reader);


            FeatureIterator<SimpleFeature> features = featureJSON.streamFeatureCollection(br);
            SimpleFeature feature = null;
            while(features.hasNext()){
                feature = features.next();
                if(feature != null)
                    break;
            }


            Geometry geom = (Geometry) feature.getDefaultGeometry();
            String geometryType = null;
            if(geom instanceof Polygon)
                geometryType = "Polygon";
            if(geom instanceof MultiPolygon)
                geometryType = "MultiPolygon";
            if(geom instanceof LineString)
                geometryType = "LineString";
            if(geom instanceof MultiLineString)
                geometryType = "MultiLineString";

            return geometryType;
        }
    }

    public void partitionGraphWithThreshold(double threshold, String filepath, String outputPath) throws JSONException, IOException {
        this.filepath = filepath;
        String geometryType = checkGeometryType(filepath);
        if(geometryType.equalsIgnoreCase("Polygon") || geometryType.equalsIgnoreCase("MultiPolygon")) {
            this.polygonGraph = NodeClusteringService.convertGeoJSONToGraph(filepath);
            PolygonNode startNode = NodeClusteringService.findNodeWithSmallestArea(this.polygonGraph);
            List<NodeCluster> clusters1 = NodeClusteringService.partitionGraphWithThreshold(this.polygonGraph, threshold, startNode);
            NodeClusteringService.createAndSavePartitionedGeoJSON(clusters1, outputPath);

            int maxIterations = 5; // Imposta il numero massimo di iterazioni desiderato
            int currentIteration = 0;

            while (currentIteration < maxIterations) {
                Graph<PolygonNode, DefaultWeightedEdge> graph = NodeClusteringService.convertGeoJSONToGraph(outputPath);
                PolygonNode startNode1 = NodeClusteringService.findNodeWithSmallestArea(graph);
                List<NodeCluster> clusters2 = NodeClusteringService.partitionGraphWithThreshold(graph, threshold, startNode1);
                NodeClusteringService.createAndSavePartitionedGeoJSON(clusters2, outputPath);

                if (compareNodeClusters(clusters1, clusters2)) {
                    break;
                } else {
                    clusters1 = clusters2;
                    currentIteration++;
                }
            }
        }
        else if (geometryType.equalsIgnoreCase("LineString") || geometryType.equalsIgnoreCase("MultiLineString")) {
            this.lineGraph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
            LineEdge startEdge = EdgeClusteringService.findEdgeWithSmallestLength(this.lineGraph);
            List<LineCluster> clusters = EdgeClusteringService.partitionGraphWithThreshold(this.lineGraph, threshold, startEdge, "users");
            EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters, outputPath);


            int maxIterations = 5; // Imposta il numero massimo di iterazioni desiderato
            int currentIteration = 0;

            while (currentIteration < maxIterations) {
                Graph<LineEdge, DefaultWeightedEdge> graph = EdgeClusteringService.convertGeoJSONToGraph(outputPath);
                LineEdge startEdge1 = EdgeClusteringService.findEdgeWithSmallestLength(graph);
                List<LineCluster> clusters2 = EdgeClusteringService.partitionGraphWithThreshold(graph, threshold, startEdge1, "users");
                EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters2, outputPath);

                if (compareLineClusters(clusters, clusters2)) {
                    break;
                } else {
                    clusters = clusters2;
                    currentIteration++;
                }
            }
        }
    }

    public void partitionGraphWithNumClusters(int numClsters, String filepath, String outputPath) throws JSONException, IOException {
        this.filepath = filepath;
        String geometryType = checkGeometryType(filepath);
        if(geometryType.equalsIgnoreCase("Polygon") || geometryType.equalsIgnoreCase("MultiPolygon")) {
            this.polygonGraph = NodeClusteringService.convertGeoJSONToGraph(filepath);
            List<NodeCluster> clusters = NodeClusteringService.partitionGraphWithNumClusters(this.polygonGraph, numClsters);
            NodeClusteringService.createAndSavePartitionedGeoJSON(clusters, outputPath);
        }
        else if (geometryType.equalsIgnoreCase("LineString") || geometryType.equalsIgnoreCase("MultiLineString")) {
            this.lineGraph = EdgeClusteringService.convertGeoJSONToGraph(filepath);
            List<LineCluster> clusters = EdgeClusteringService.partitionGraphWithNumClusters(this.lineGraph, numClsters);
            EdgeClusteringService.createAndSavePartitionedGeoJSON(clusters, outputPath);
        }
    }

    public JSONObject getCRS() throws IOException {
        String geometryType = checkGeometryType(this.filepath);
        JSONObject crs = null;
        if(geometryType.equalsIgnoreCase("Polygon") || geometryType.equalsIgnoreCase("MultiPolygon")) {
            crs = NodeClusteringService.getCrsFile();
        }
        else if (geometryType.equalsIgnoreCase("LineString") || geometryType.equalsIgnoreCase("MultiLineString")) {
            crs = EdgeClusteringService.getCrsFile();
        }

        return crs;
    }
}
