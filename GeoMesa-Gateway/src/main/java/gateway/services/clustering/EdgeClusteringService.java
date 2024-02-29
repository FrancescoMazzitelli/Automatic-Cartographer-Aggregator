package gateway.services.clustering;

import gateway.utility.cluster.LineCluster;
import gateway.utility.LineEdge;
import org.apache.catalina.Cluster;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jgrapht.Graphs;
import org.jgrapht.alg.clustering.KSpanningTreeClustering;
import org.jgrapht.alg.interfaces.ClusteringAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.WeightedPseudograph;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.geotools.feature.FeatureIterator;
import org.geotools.geojson.feature.FeatureJSON;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.union.UnaryUnionOp;
import org.opengis.feature.simple.SimpleFeature;

import java.io.*;
import java.util.*;

import org.codehaus.jettison.json.JSONObject;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import smile.clustering.SpectralClustering;
import smile.math.matrix.Matrix;

public class EdgeClusteringService {

    private static Graph<LineEdge, DefaultWeightedEdge> finalgraph;
    private static JSONObject crsFile = null;

    public static Graph<LineEdge, DefaultWeightedEdge> convertGeoJSONToGraph(String filePath) throws IOException {
        FeatureJSON featureJSON = new FeatureJSON();
        FileReader reader = new FileReader(filePath);
        BufferedReader br = new BufferedReader(reader);

        FileReader readerCRS = new FileReader(filePath);
        BufferedReader brCRS = new BufferedReader(readerCRS);

        Graph<LineEdge, DefaultWeightedEdge> graph = new WeightedPseudograph<>(DefaultWeightedEdge.class);

        try {
            FeatureIterator<SimpleFeature> features = featureJSON.streamFeatureCollection(br);
            List<SimpleFeature> featuresList = new ArrayList<>();

            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                featuresList.add(feature);
            }

            JSONObject json = new JSONObject(brCRS.readLine());
            if(json.getJSONObject("crs").toString() != null)
                crsFile = json.getJSONObject("crs");

            for(SimpleFeature line1 : featuresList) {
                SimpleFeatureType featureType = line1.getFeatureType();
                List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();
                Geometry geometry1 = (Geometry) line1.getDefaultGeometry();
                Point centroid = geometry1.getCentroid();

                double length1 = 0;
                double families1 = 0;
                String lineId1 = line1.getID();
                String lineName1 = "Strada senza nome";

                for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                    Object attributeValue = line1.getAttribute(attributeDescriptor.getLocalName());
                    if(attributeValue != null) {
                        if (attributeDescriptor.getLocalName().toLowerCase().contains("lun") || attributeDescriptor.getLocalName().toLowerCase().contains("len")) {
                            length1 = Double.parseDouble(attributeValue.toString());
                        }
                        if (attributeDescriptor.getLocalName().toLowerCase().contains("fam") || attributeDescriptor.getLocalName().toLowerCase().contains("uten")) {
                            families1 = Double.parseDouble(attributeValue.toString());
                        }
                        if (attributeDescriptor.getLocalName().toLowerCase().contains("name")
                                || attributeDescriptor.getLocalName().toLowerCase().contains("nome")
                                || attributeDescriptor.getLocalName().toLowerCase().contains("str")) {
                            lineName1 = attributeValue.toString();
                        }
                        if (attributeDescriptor.getLocalName().toLowerCase().contains("partition")) {
                            lineId1 = attributeValue.toString();
                        }
                    }
                }
                LineEdge lineEdge1 = new LineEdge(centroid, length1, families1, lineId1, lineName1, geometry1);
                if(!graph.containsVertex(lineEdge1)) {
                    graph.addVertex(lineEdge1);
                }

                double maxDistanceThreshold = 0.00000001;
                for (SimpleFeature line2 : featuresList) {
                    boolean areAdjacent;
                    if (line2.getID().equalsIgnoreCase(lineId1)) {
                        continue;
                    }
                    Geometry geometry2 = (Geometry) line2.getDefaultGeometry();
                    areAdjacent = areGeometriesAdjacent(geometry1, geometry2, maxDistanceThreshold);

                    if (geometry2.intersects(geometry1) || areAdjacent) {
                        SimpleFeatureType featureType2 = line2.getFeatureType();
                        List<AttributeDescriptor> attributeDescriptors2 = featureType2.getAttributeDescriptors();
                        Point centroid2 = geometry2.getCentroid();

                        double length2 = 0;
                        double families2 = 0;
                        String lineId2 = line2.getID();
                        String lineName2 = "Strada senza nome";

                        for (AttributeDescriptor attributeDescriptor2 : attributeDescriptors2) {
                            Object attributeValue = line2.getAttribute(attributeDescriptor2.getLocalName());
                            if(attributeValue != null) {
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("lun") || attributeDescriptor2.getLocalName().toLowerCase().contains("len")) {
                                    length2 = Double.parseDouble(attributeValue.toString());
                                }
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("fam") || attributeDescriptor2.getLocalName().toLowerCase().contains("uten")) {
                                    families2 = Double.parseDouble(attributeValue.toString());
                                }
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("name")
                                        || attributeDescriptor2.getLocalName().toLowerCase().contains("nome")
                                        || attributeDescriptor2.getLocalName().toLowerCase().contains("str")) {
                                    lineName2 = attributeValue.toString();
                                }
                                if (attributeDescriptor2.getLocalName().toLowerCase().contains("partition")) {
                                    lineId2 = attributeValue.toString();
                                }
                            }
                        }

                        LineEdge lineEdge2 = new LineEdge(centroid2, length2, families2, lineId2, lineName2, geometry2);
                        if(!graph.containsVertex(lineEdge2)) {
                            graph.addVertex(lineEdge2);
                        }

                        if (!graph.containsEdge(lineEdge1, lineEdge2)) {
                            DefaultWeightedEdge edge = graph.addEdge(lineEdge1, lineEdge2);
                            Double weightLineEdge1 = lineEdge1.getWeight();
                            Double weightLineEdge2 = lineEdge2.getWeight();
                            double edgeWeight = weightLineEdge1 + weightLineEdge2;
                            graph.setEdgeWeight(edge, edgeWeight);
                        }
                    }
                }
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } finally {
            reader.close();
        }

        finalgraph = graph;
        return graph;
    }

    private static boolean areGeometriesAdjacent(Geometry geometry1, Geometry geometry2, double maxDistanceThreshold) {
        if (geometry2.distance(geometry1) <= maxDistanceThreshold){
            return true;
        }
        return false;
    }


    public static void printGraph(Graph<LineEdge, DefaultWeightedEdge> graph) {
        System.out.println("Adjacency Matrix:");

        List<LineEdge> vertices = new ArrayList<>(graph.vertexSet());
        int numVertices = vertices.size();

        // Stampa l'intestazione
        System.out.print("      ");
        for (LineEdge vertex : vertices) {
            System.out.print(String.format("%-5s", vertex.getLineId()));
        }
        System.out.println();

        // Stampa la matrice di adiacenza
        for (int i = 0; i < numVertices; i++) {
            System.out.print(String.format("%-5s", vertices.get(i).getLineId()) + ": ");
            for (int j = 0; j < numVertices; j++) {
                LineEdge sourceVertex = vertices.get(i);
                LineEdge targetVertex = vertices.get(j);

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

    public static void createAndSavePartitionedGeoJSON(List<LineCluster> partitions, String outputPath) throws IOException, JSONException {
        JSONArray features = new JSONArray();
        JSONObject geojsonData = new JSONObject();
        geojsonData.put("type", "FeatureCollection");

        for (LineCluster lineCluster : partitions) {
            for(LineEdge line : lineCluster.getLines()) {
                if (line != null) {

                    JSONObject feature = new JSONObject();
                    feature.put("type", "Feature");

                    JSONObject properties = new JSONObject();
                    properties.put("Partition_ID", lineCluster.getClusterId());
                    properties.put("Street", line.getLineName());
                    if (line.getWeight() != 0)
                        properties.put("Length", line.getWeight());
                    if(line.getFamilies() != 0)
                        properties.put("Families", line.getFamilies());

                    feature.put("properties", properties);

                    JSONObject lineJson = new JSONObject();
                    JSONArray coordinates = new JSONArray();


                    addLineCoordinates(line.getLineString(), coordinates, lineJson);


                    lineJson.put("type", "MultiLineString");
                    lineJson.put("coordinates", coordinates);
                    feature.put("geometry", lineJson);

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

        if(crsFile != null)
            geojsonData.put("crs", crsFile);
        else {
            geojsonData.put("crs", crs);
            crsFile = crs;
        }
        try (FileWriter writer = new FileWriter(outputPath)) {
            geojsonData.write(writer);
        }
    }

    private static void addLineCoordinates(Geometry line, JSONArray coordinates, JSONObject lineJson) throws JSONException {
        if (line instanceof MultiLineString) {
            MultiLineString multiLineString = (MultiLineString) line;
            for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
                LineString lineString = (LineString) multiLineString.getGeometryN(i);
                addSingleLineCoordinates(lineString, coordinates);
            }
        } else if (line instanceof LineString) {
            addSingleLineCoordinates((LineString) line, coordinates);
        }
    }

    private static void addSingleLineCoordinates(LineString lineString, JSONArray coordinates) throws JSONException {
        // Aggiungi le coordinate per il LineString
        JSONArray lineCoordinates = new JSONArray();
        Coordinate[] lineStringCoordinates = lineString.getCoordinates();

        for (Coordinate c : lineStringCoordinates) {
            JSONArray pair = new JSONArray();
            pair.put(c.getX());
            pair.put(c.getY());
            lineCoordinates.put(pair);
        }

        coordinates.put(lineCoordinates);
    }

    private static List<Geometry> getPartitionGeometries(List<LineEdge> partitionNodes) {
        PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.maximumPreciseValue);
        List<Geometry> partitionGeometries = new ArrayList<>();

        for(LineEdge line : partitionNodes){
            partitionGeometries.add(line.getLineString());
        }
        return partitionGeometries;
    }


    public static Geometry unionGeometries(Collection<Geometry> geom) {
        if (geom.size() == 1) {
            return geom.iterator().next();
        }

        UnaryUnionOp union = new UnaryUnionOp(geom);
        return union.union();
    }

    private static double calculateCurrentSum(List<LineEdge> currentCluster, String flag) {
        double currentSum = 0;
        if (flag.equalsIgnoreCase("length")) {
            for (LineEdge sumNode : currentCluster) {
                double sum = sumNode.getWeight();
                currentSum += sum;
            }
        }
        if (flag.equalsIgnoreCase("users")) {
            for (LineEdge sumNode : currentCluster) {
                double sum = sumNode.getFamilies();
                currentSum += sum;
            }
        }
        return currentSum;
    }


    public static List<LineCluster> partitionGraphWithThreshold(Graph<LineEdge, DefaultWeightedEdge> graph,
                                                   double maxFamilies, LineEdge startNode, String flag) {

        List<LineCluster> nodeClusters = new ArrayList<>();
        Set<LineEdge> visited = new HashSet<>();
        int clusterIdCounter = 0;

        List<LineEdge> clusterNodes = exploreCluster(graph, startNode, maxFamilies, visited, flag);
        nodeClusters.add(new LineCluster(clusterIdCounter++, calculateCurrentSum(clusterNodes, flag), clusterNodes));
        visited.addAll(clusterNodes);

        for (LineEdge node : graph.vertexSet()) {
            if (!visited.contains(node)) {
                clusterNodes = exploreCluster(graph, node, maxFamilies, visited, flag);
                nodeClusters.add(new LineCluster(clusterIdCounter++, calculateCurrentSum(clusterNodes, flag), clusterNodes));
                visited.addAll(clusterNodes);
            }
        }

        for (LineEdge node : graph.vertexSet()) {
            if (!visited.contains(node)) {
                clusterNodes = singleClusters(graph, node, maxFamilies, visited, flag);
                nodeClusters.add(new LineCluster(clusterIdCounter++, calculateCurrentSum(clusterNodes, flag), clusterNodes));
                visited.addAll(clusterNodes);
            }
        }

        return nodeClusters;
    }

    private static List<LineEdge> exploreCluster(Graph<LineEdge, DefaultWeightedEdge> graph,
                                                    LineEdge startNode, double maxFamilies, Set<LineEdge> visited, String flag) {
        List<LineEdge> cluster = new ArrayList<>();
        if (flag.equalsIgnoreCase("length")) {
            Queue<LineEdge> queue = new LinkedList<>();
            queue.add(startNode);

            while (!queue.isEmpty()) {
                LineEdge currentNode = queue.poll();
                if (!visited.contains(currentNode)) {
                    double currentSum = calculateCurrentSum(cluster, flag);
                    if (currentSum + currentNode.getWeight() <= maxFamilies) {
                        cluster.add(currentNode);
                        visited.add(currentNode);
                        queue.addAll(getUnvisitedNeighbors(graph, currentNode, visited));
                    } else {
                        break;
                    }
                }
            }
        }
        if (flag.equalsIgnoreCase("users")) {
            Queue<LineEdge> queue = new LinkedList<>();
            queue.add(startNode);

            while (!queue.isEmpty()) {
                LineEdge currentNode = queue.poll();
                if (!visited.contains(currentNode)) {
                    double currentSum = calculateCurrentSum(cluster, flag);
                    if (currentSum + currentNode.getFamilies() <= maxFamilies) {
                        cluster.add(currentNode);
                        visited.add(currentNode);
                        queue.addAll(getUnvisitedNeighbors(graph, currentNode, visited));
                    } else {
                        break;
                    }
                }
            }
        }
        return cluster;
    }

    private static List<LineEdge> singleClusters(Graph<LineEdge, DefaultWeightedEdge> graph,
                                                 LineEdge startNode, double maxFamilies, Set<LineEdge> visited, String flag) {
        List<LineEdge> cluster = new ArrayList<>();
        if (flag.equalsIgnoreCase("length")) {
            if (!visited.contains(startNode)) {
                if (startNode.getWeight() > maxFamilies) {
                    cluster.add(startNode);
                    visited.add(startNode);
                }
            }
        }
        if (flag.equalsIgnoreCase("users")) {
            if (!visited.contains(startNode)) {
                if (startNode.getFamilies() > maxFamilies) {
                    cluster.add(startNode);
                    visited.add(startNode);
                }
            }
        }
        return cluster;
    }

    private static List<LineEdge> getUnvisitedNeighbors(Graph<LineEdge, DefaultWeightedEdge> graph,
                                                           LineEdge node, Set<LineEdge> visited) {
        List<LineEdge> neighbors = new ArrayList<>();
        for (DefaultWeightedEdge edge : graph.edgesOf(node)) {
            LineEdge neighbor = Graphs.getOppositeVertex(graph, edge, node);
            if (!visited.contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }
        return neighbors;
    }


    public static LineEdge findEdgeWithSmallestLength(Graph<LineEdge, DefaultWeightedEdge> graph) {
        LineEdge minNode = null;
        double minArea = Double.MAX_VALUE;

        for (LineEdge node : graph.vertexSet()) {
            double nodeLength = node.getWeight();
            if (nodeLength < minArea) {
                minArea = nodeLength;
                minNode = node;
            }
        }

        return minNode;
    }

    // Metodo per calcolare la matrice di similarità da un grafo
    public static double[][] calculateSimilarityMatrix(Graph<LineEdge, DefaultWeightedEdge> graph) {
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
            LineEdge sourceVertex = graph.getEdgeSource(edge);
            LineEdge targetVertex = graph.getEdgeTarget(edge);

            int sourceIndex = getIndex(graph, sourceVertex);
            int targetIndex = getIndex(graph, targetVertex);

            similarityMatrix[sourceIndex][targetIndex] = 1.0;
            similarityMatrix[targetIndex][sourceIndex] = 1.0;
        }

        return similarityMatrix;
    }

    // Metodo di utilità per ottenere l'indice di un vertice nel grafo
    private static int getIndex(Graph<LineEdge, DefaultWeightedEdge> graph, LineEdge vertex) {
        List<LineEdge> verticesList = new ArrayList<>(graph.vertexSet());
        return verticesList.indexOf(vertex);
    }


    // Metodo per applicare lo spectral clustering utilizzando Smile
    public static Map<LineEdge, Integer> spectralClustering(Graph<LineEdge, DefaultWeightedEdge> graph, double[][] similarityMatrix, int numClusters) {
        Matrix matrix = new Matrix(similarityMatrix);
        var clusters = SpectralClustering.fit(matrix, numClusters);
        Map<LineEdge, Integer> nodeLabels = new HashMap<>();

        // Associa gli indici del clustering con i nomi dei nodi
        List<LineEdge> verticesList = new ArrayList<>(graph.vertexSet());
        for (int i = 0; i < verticesList.size(); i++) {
            nodeLabels.put(verticesList.get(i), clusters.y[i]);
        }

        return nodeLabels;
    }

    public static List<LineCluster> partitionGraphWithNumClusters(Graph<LineEdge, DefaultWeightedEdge> graph, int k){
        List<LineCluster> clusters = new ArrayList<>();
        // Calcola la matrice di similarità
        double[][] similarityMatrix = calculateSimilarityMatrix(graph);

        // Applica lo spectral clustering
        Map<LineEdge, Integer> nodeLabels = spectralClustering(graph, similarityMatrix, k);

        // Visualizza i risultati dello spectral clustering
        int numClusters = (int) nodeLabels.values().stream().distinct().count();
        for(int i = 0; i<numClusters; i++){
            List<LineEdge> edges = new ArrayList<>();
            for (Map.Entry<LineEdge, Integer> entry : nodeLabels.entrySet()) {
                if(entry.getValue().equals(i)){
                    edges.add(entry.getKey());
                }
            }
            double totalLength = 0;
            for(LineEdge line : edges){
                totalLength += line.getLength();
            }
            LineCluster cluster = new LineCluster(i, totalLength, edges);
            clusters.add(cluster);
        }

        return clusters;
    }

    public static JSONObject getCrsFile(){
        return crsFile;
    }

}
