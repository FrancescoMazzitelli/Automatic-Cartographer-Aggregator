package gateway.services.clustering;

import gateway.utility.LineEdge;
import gateway.utility.cluster.LineCluster;
import gateway.utility.cluster.NodeCluster;
import gateway.utility.PolygonNode;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jgrapht.Graphs;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.opengis.feature.simple.SimpleFeature;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import org.codehaus.jettison.json.JSONObject;
import org.opengis.referencing.operation.TransformException;
import smile.clustering.SpectralClustering;
import smile.math.matrix.Matrix;

public class NodeClusteringService {

    private static Map<PolygonNode, Double> nodeWeights;


    public static Graph<PolygonNode, DefaultWeightedEdge> convertGeoJSONToGraph(String filePath) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        FileReader reader = new FileReader(filePath);
        BufferedReader br = new BufferedReader(reader);

        Graph<PolygonNode, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        nodeWeights = new HashMap<>();

        try {
            FeatureIterator<SimpleFeature> features = featureJSON.streamFeatureCollection(br);
            List<SimpleFeature> featuresList = new ArrayList<>();

            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                featuresList.add(feature);
            }

            for(SimpleFeature polygon1 : featuresList) {
                SimpleFeatureType featureType = polygon1.getFeatureType();
                List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
                Geometry geometry1 = (Geometry) polygon1.getDefaultGeometry();
                Point centroid = geometry1.getCentroid();
                double shapeLeng = geometry1.getLength();
                double shapeArea = geometry1.getArea();

                double famiglie1 = 0;
                String polygonId = polygon1.getID();

                for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                    Object attributeValue = polygon1.getAttribute(attributeDescriptor.getLocalName());
                    if (attributeDescriptor.getLocalName().toLowerCase().contains("fam")) {
                        famiglie1 = Double.parseDouble(attributeValue.toString());
                    }
                    if (attributeDescriptor.getLocalName().toLowerCase().contains("partition")) {
                        polygonId = attributeValue.toString();
                    }
                }

                PolygonNode polygonNode1 = new PolygonNode(polygonId, centroid, shapeLeng, shapeArea, geometry1);
                if(!graph.containsVertex(polygonNode1)) {
                    graph.addVertex(polygonNode1);
                    if(!nodeWeights.containsKey(polygonNode1))
                        nodeWeights.put(polygonNode1, famiglie1);
                }

                for (SimpleFeature polygon2 : featuresList) {
                    if (polygon2.getID().equalsIgnoreCase(polygonId)) {
                        continue;
                    }
                    Geometry geometry2 = (Geometry) polygon2.getDefaultGeometry();
                    if (geometry2.touches(geometry1)) {
                        Geometry intersection = geometry2.intersection(geometry1);
                        if (intersection.getNumPoints() > 1) {
                            SimpleFeatureType featureType2 = polygon2.getFeatureType();
                            List<AttributeDescriptor> attributeDescriptors2 = featureType2.getAttributeDescriptors();
                            Point centroid2 = geometry2.getCentroid();
                            double shapeLeng2 = geometry2.getLength();
                            double shapeArea2 = geometry2.getArea();

                            double famiglie2 = 0;
                            String polygon2Id = polygon2.getID();

                            for (AttributeDescriptor attributeDescriptor2 : attributeDescriptors2) {
                                Object attributeValue = polygon2.getAttribute(attributeDescriptor2.getLocalName());
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("fam")) {
                                    famiglie2 = Double.parseDouble(attributeValue.toString());
                                }
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("partition")) {
                                   polygon2Id = attributeValue.toString();
                                }
                            }

                            PolygonNode polygonNode2 = new PolygonNode(polygon2Id, centroid2, shapeLeng2, shapeArea2, geometry2);
                            if(!graph.containsVertex(polygonNode2)) {
                                graph.addVertex(polygonNode2);
                                if(!nodeWeights.containsKey(polygonNode2))
                                    nodeWeights.put(polygonNode2, famiglie2);
                            }

                            if (!graph.containsEdge(polygonNode1, polygonNode2)) {
                                DefaultWeightedEdge edge = graph.addEdge(polygonNode1, polygonNode2);
                                Double weightPolygonNode1 = nodeWeights.get(polygonNode1);
                                Double weightPolygonNode2 = nodeWeights.get(polygonNode2);
                                double edgeWeight = weightPolygonNode1 + weightPolygonNode2;
                                graph.setEdgeWeight(edge, edgeWeight);
                            }
                        }
                    }
                }
            }
        } finally {
            reader.close();
        }
        return graph;
    }


    public static void printGraph(Graph<PolygonNode, DefaultWeightedEdge> graph) {
        System.out.println("Adjacency Matrix:");

        List<PolygonNode> vertices = new ArrayList<>(graph.vertexSet());
        int numVertices = vertices.size();

        // Stampa l'intestazione
        System.out.print("      ");
        for (PolygonNode vertex : vertices) {
            System.out.print(String.format("%-5s", vertex.getPolygonId()));
        }
        System.out.println();

        // Stampa la matrice di adiacenza
        for (int i = 0; i < numVertices; i++) {
            System.out.print(String.format("%-5s", vertices.get(i).getPolygonId()) + ": ");
            for (int j = 0; j < numVertices; j++) {
                PolygonNode sourceVertex = vertices.get(i);
                PolygonNode targetVertex = vertices.get(j);

                DefaultWeightedEdge edge = graph.getEdge(sourceVertex, targetVertex);
                if (graph.containsEdge(edge)) {
                    System.out.print(String.format("%-5s", 1));
                } else {
                    System.out.print(String.format("%-5s", 0));
                }
            }
            System.out.println();
        }
    }


    public static void createAndSavePartitionedGeoJSON(List<NodeCluster> partitions, String outputPath) throws IOException, JSONException, FactoryException, TransformException {
        JSONArray features = new JSONArray();
        JSONObject geojsonData = new JSONObject();
        geojsonData.put("type", "FeatureCollection");

        for (NodeCluster nodeCluster : partitions) {
            for(PolygonNode node : nodeCluster.getNodes()) {
                if (node != null) {
                    double perimeter = node.getShapeLeng();
                    double area = node.getShapeArea();


                    JSONObject feature = new JSONObject();
                    feature.put("type", "Feature");

                    JSONObject properties = new JSONObject();
                    properties.put("Partition_ID", nodeCluster.getClusterId());
                    properties.put("Families", nodeWeights.get(node));

                    properties.put("Perimeter", perimeter);
                    properties.put("Area", area);

                    feature.put("properties", properties);


                    JSONObject polygonJson = new JSONObject();
                    JSONArray coordinates = new JSONArray();


                    if (node.getAssociatedPolygon() instanceof Polygon) {
                        addPolygonCoordinates((Polygon) node.getAssociatedPolygon(), coordinates);
                    } else if (node.getAssociatedPolygon() instanceof MultiPolygon) {
                        MultiPolygon multiPolygon = (MultiPolygon) node.getAssociatedPolygon();
                        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                            Geometry geometry = multiPolygon.getGeometryN(i);
                            if (geometry instanceof Polygon) {
                                addPolygonCoordinates((Polygon) geometry, coordinates);
                            } else {
                                throw new IllegalArgumentException("Unsupported geometry type: " + geometry.getGeometryType());
                            }
                        }
                    }


                    polygonJson.put("type", "Polygon");
                    polygonJson.put("coordinates", coordinates);
                    feature.put("geometry", polygonJson);

                    features.put(feature);
                }
            }
        }

        geojsonData.put("features", features);
        JSONObject crs = new JSONObject();
        JSONObject crsName = new JSONObject();
        crsName.put("name", "urn:ogc:def:crs:EPSG::32633");
        crs.put("type", "name");
        crs.put("properties", crsName);
        geojsonData.put("crs", crs);

        try (FileWriter writer = new FileWriter(outputPath)) {
            geojsonData.write(writer);
        }
    }

    private static void addPolygonCoordinates(Polygon polygon, JSONArray coordinates) throws JSONException {
        // Aggiungi l'outer ring
        JSONArray outerRing = new JSONArray();
        Coordinate[] outerRingCoordinates = polygon.getExteriorRing().getCoordinates();

        for (Coordinate c : outerRingCoordinates) {
            JSONArray pair = new JSONArray();
            pair.put(c.getX());
            pair.put(c.getY());
            outerRing.put(pair);
        }
        coordinates.put(outerRing);

        // Aggiungi gli inner ring
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            JSONArray innerRing = new JSONArray();
            Coordinate[] innerRingCoordinates = polygon.getInteriorRingN(i).getCoordinates();

            for (Coordinate c : innerRingCoordinates) {
                JSONArray pair = new JSONArray();
                pair.put(c.getX());
                pair.put(c.getY());
                innerRing.put(pair);
            }

            coordinates.put(innerRing);
        }
    }


    private static List<Geometry> getPartitionGeometries(List<PolygonNode> partitionNodes) {
        PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.maximumPreciseValue);
        List<Geometry> partitionGeometries = new ArrayList<>();

        for(PolygonNode node : partitionNodes){
            partitionGeometries.add(node.getAssociatedPolygon());
        }
        return partitionGeometries;
    }

    public static Geometry unionGeometries(List<Geometry> geometries) {
        GeometryCollection geometryCollection = new GeometryCollection(geometries.toArray(new Geometry[0]), new GeometryFactory());
        return geometryCollection.union();
    }

    private static double calculateCurrentSum(List<PolygonNode> currentCluster) {
        double currentSum = 0;
        for (PolygonNode sumNode : currentCluster) {
            double sum = nodeWeights.get(sumNode);
            currentSum += sum;
        }
        return currentSum;
    }


    public static List<NodeCluster> partitionGraphWithThreshold(Graph<PolygonNode, DefaultWeightedEdge> graph,
                                                   double maxFamilies, PolygonNode startNode) {

        List<NodeCluster> nodeClusters = new ArrayList<>();
        Set<PolygonNode> visited = new HashSet<>();
        int clusterIdCounter = 0;

        List<PolygonNode> clusterNodes = exploreCluster(graph, startNode, maxFamilies, visited);
        nodeClusters.add(new NodeCluster(clusterIdCounter++, calculateCurrentSum(clusterNodes), clusterNodes));
        visited.addAll(clusterNodes);

        for (PolygonNode node : graph.vertexSet()) {
            if (!visited.contains(node)) {
                clusterNodes = exploreCluster(graph, node, maxFamilies, visited);
                nodeClusters.add(new NodeCluster(clusterIdCounter++, calculateCurrentSum(clusterNodes), clusterNodes));
                visited.addAll(clusterNodes);
            }
        }

        return nodeClusters;
    }

    private static List<PolygonNode> exploreCluster(Graph<PolygonNode, DefaultWeightedEdge> graph,
                                                    PolygonNode startNode, double maxFamilies, Set<PolygonNode> visited) {
        List<PolygonNode> cluster = new ArrayList<>();
        Queue<PolygonNode> queue = new LinkedList<>();
        queue.add(startNode);

        while (!queue.isEmpty()) {
            PolygonNode currentNode = queue.poll();
            if (!visited.contains(currentNode)) {
                double currentSum = calculateCurrentSum(cluster);
                if (currentSum + nodeWeights.get(currentNode) <= maxFamilies) {
                    cluster.add(currentNode);
                    visited.add(currentNode);
                    queue.addAll(getUnvisitedNeighbors(graph, currentNode, visited));
                } else {
                    // Inizia un nuovo cluster quando la soglia è superata
                    break;
                }
            }
        }

        return cluster;
    }

    private static List<PolygonNode> getUnvisitedNeighbors(Graph<PolygonNode, DefaultWeightedEdge> graph,
                                                           PolygonNode node, Set<PolygonNode> visited) {
        List<PolygonNode> neighbors = new ArrayList<>();
        for (DefaultWeightedEdge edge : graph.edgesOf(node)) {
            PolygonNode neighbor = Graphs.getOppositeVertex(graph, edge, node);
            if (!visited.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }


    public static PolygonNode findNodeWithSmallestArea(Graph<PolygonNode, DefaultWeightedEdge> graph) {
        PolygonNode minNode = null;
        double minArea = Double.MAX_VALUE;

        for (PolygonNode node : graph.vertexSet()) {
            double nodeArea = node.getShapeArea();
            if (nodeArea < minArea) {
                minArea = nodeArea;
                minNode = node;
            }
        }

        return minNode;
    }

    // Metodo per calcolare la matrice di similarità da un grafo
    public static double[][] calculateSimilarityMatrix(Graph<PolygonNode, DefaultWeightedEdge> graph) {
        int numVertices = graph.vertexSet().size();
        double[][] similarityMatrix = new double[numVertices][numVertices];

        // Inizializza la matrice di similarità
        for (int i = 0; i < numVertices; i++) {
            for (int j = 0; j < numVertices; j++) {
                similarityMatrix[i][j] = i == j ? 1.0 : 0.0;
            }
        }

        // Calcola la matrice di similarità basata sulla connettività dei vertici
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            PolygonNode sourceVertex = graph.getEdgeSource(edge);
            PolygonNode targetVertex = graph.getEdgeTarget(edge);

            int sourceIndex = getIndex(graph, sourceVertex);
            int targetIndex = getIndex(graph, targetVertex);

            similarityMatrix[sourceIndex][targetIndex] = 1;
            similarityMatrix[targetIndex][sourceIndex] = 1;
        }

        return similarityMatrix;
    }

    // Metodo di utilità per ottenere l'indice di un vertice nel grafo
    private static int getIndex(Graph<PolygonNode, DefaultWeightedEdge> graph, PolygonNode vertex) {
        List<PolygonNode> verticesList = new ArrayList<>(graph.vertexSet());
        return verticesList.indexOf(vertex);
    }

    // Metodo per applicare lo spectral clustering utilizzando Smile
    public static Map<PolygonNode, Integer> spectralClustering(Graph<PolygonNode, DefaultWeightedEdge> graph, double[][] similarityMatrix, int numClusters) {
        Matrix matrix = new Matrix(similarityMatrix);
        var clusters = SpectralClustering.fit(matrix, numClusters);

        Map<PolygonNode, Integer> nodeLabels = new HashMap<>();

        // Associa gli indici del clustering con i nomi dei nodi
        List<PolygonNode> verticesList = new ArrayList<>(graph.vertexSet());
        for (int i = 0; i < verticesList.size(); i++) {
            nodeLabels.put(verticesList.get(i), clusters.y[i]);
        }

        return nodeLabels;
    }

    public static List<NodeCluster> partitionGraphWithNumClusters(Graph<PolygonNode, DefaultWeightedEdge> graph, int k){
        List<NodeCluster> clusters = new ArrayList<>();
        // Calcola la matrice di similarità
        double[][] similarityMatrix = calculateSimilarityMatrix(graph);

        // Applica lo spectral clustering
        Map<PolygonNode, Integer> nodeLabels = spectralClustering(graph, similarityMatrix, k);

        // Visualizza i risultati dello spectral clustering
        int numClusters = (int) nodeLabels.values().stream().distinct().count();
        for(int i = 0; i<numClusters; i++){
            List<PolygonNode> nodes = new ArrayList<>();
            for (Map.Entry<PolygonNode, Integer> entry : nodeLabels.entrySet()) {
                if(entry.getValue().equals(i)){
                    nodes.add(entry.getKey());
                }
            }
            double totalfamilies = 0;
            for(PolygonNode node : nodes){
                double families = nodeWeights.get(node);
                totalfamilies += families;
            }
            NodeCluster cluster = new NodeCluster(i, totalfamilies, nodes);
            clusters.add(cluster);
        }

        return clusters;
    }

}
