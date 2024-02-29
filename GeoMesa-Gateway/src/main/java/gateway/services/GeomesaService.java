package gateway.services;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.util.*;
import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GeomesaService {

    private static String geomFlag;
    private static JSONObject crsFile;
    private GeomesaService(){

    }

    public static DataStore createDataStore(String connectionString, String catalog) throws IOException {
        Map<String, Serializable> params = new HashMap<>();
        params.put("redis.url", connectionString);
        params.put("redis.catalog", catalog);
        params.put("geomesa.security.force-empty-auths", true);

        DataStore dataStore = DataStoreFinder.getDataStore(params);
        return dataStore;
    }

    public static SimpleFeatureType getSchema(String typeName, JSONObject featureJson) throws JSONException {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        builder.setName(typeName);

        // Estrai la singola feature
        JSONObject singleFeature = featureJson.getJSONArray("features").getJSONObject(0);
        JSONObject properties = singleFeature.getJSONObject("properties");
        JSONObject geometry = singleFeature.getJSONObject("geometry");

        // Aggiungi attributi dinamicamente da "properties"
        Iterator<String> keysIterator = properties.keys();
        while (keysIterator.hasNext()) {
            String attributeName = keysIterator.next();
            Object attributeValue = properties.get(attributeName);
            if (attributeValue instanceof Number) {
                builder.add(attributeName, Double.class);
            } else if (attributeValue instanceof String) {
                builder.add(attributeName, String.class);
            }
        }

        // Aggiungi campo "geometry"
        String geometryType = geometry.getString("type");

        switch (geometryType.toLowerCase()) {
            case "point":
                builder.add("geom", Geometry.class);
                geomFlag = "Point";
                break;
            case "linestring":
                builder.add("geom", Geometry.class);
                geomFlag = "LineString";
                break;
            case "multilinestring":
                builder.add("geom", Geometry.class);
                geomFlag = "LineString";
                break;
            case "polygon":
                builder.add("geom", Geometry.class);
                geomFlag = "Polygon";
                break;
            // Aggiungi altri tipi di geometria se necessario
            default:
                throw new IllegalArgumentException("Tipo di geometria non gestito: " + geometryType);
        }

        return builder.buildFeatureType();
    }


    public static List<SimpleFeature> createSimpleFeatures(SimpleFeatureType type, JSONObject featureJson, JSONObject crs) throws JSONException, FactoryException, TransformException, IOException {
        crsFile = crs;
        List<SimpleFeature> featureList = new ArrayList<>();

        // Estrai la lista di features dal FeatureCollection
        JSONArray features = featureJson.getJSONArray("features");

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(type);

        for (int i = 0; i < features.length(); i++) {
            // Estrai la singola feature
            JSONObject singleFeature = features.getJSONObject(i);

            // Ottieni la lista dei campi dalla definizione del tipo di feature
            List<AttributeDescriptor> attributeDescriptors = type.getAttributeDescriptors();

            for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
                // Ottieni il nome del campo
                String attributeName = attributeDescriptor.getLocalName();

                // Verifica se il campo è presente nei "properties"
                if (singleFeature.getJSONObject("properties").has(attributeName)) {
                    // Imposta il valore del campo
                    Object attributeValue = singleFeature.getJSONObject("properties").get(attributeName);
                    featureBuilder.set(attributeName, attributeValue);
                }
            }

            if(geomFlag.equalsIgnoreCase("polygon")) {
                // Imposta la geometria solo se il descrittore di geometria non è nullo
                if (type.getGeometryDescriptor() != null) {
                    JSONObject geometryJson = singleFeature.getJSONObject("geometry");
                    featureBuilder.set("geom", buildGeometryFromJson(geometryJson, type.getGeometryDescriptor().getType().getBinding()));
                }
            }

            if(geomFlag.equalsIgnoreCase("linestring")) {
                if (type.getGeometryDescriptor() != null) {
                    Object elem = singleFeature.get("geometry");
                    if(elem instanceof JSONObject){
                        JSONObject geometryJson = singleFeature.getJSONObject("geometry");
                        featureBuilder.set("geom", buildLineStringFromJson(geometryJson, type.getGeometryDescriptor().getType().getBinding()));
                    }
                    else {
                        continue;
                    }
                }
            }

            // Crea il SimpleFeature e aggiungilo alla lista
            SimpleFeature simpleFeature = featureBuilder.buildFeature(null);
            if (simpleFeature != null)
                featureList.add(simpleFeature);
        }

        return featureList;
    }


    private static Geometry buildGeometryFromJson(JSONObject geometryJson, Class<?> geometryBinding) throws JSONException, FactoryException, TransformException, IOException {
        JSONArray coordinates = geometryJson.getJSONArray("coordinates");

        Coordinate[][] jtsCoordinates = new Coordinate[coordinates.length()][];

        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray coordArray = coordinates.getJSONArray(i);
            Coordinate[] innerCoordinates = new Coordinate[coordArray.length()];

            for (int j = 0; j < coordArray.length(); j++) {
                JSONArray singleCoord = coordArray.getJSONArray(j);

                double x, y, z;
                if (singleCoord.length() >= 2) {
                    x = singleCoord.getDouble(0);
                    y = singleCoord.getDouble(1);

                    // Converte le coordinate cartesiane in coordinate geografiche
                    double[] geographicCoordinates = cartesianToGeographic(x, y, 0.0);

                    // Utilizza le coordinate geografiche convertite
                    x = geographicCoordinates[0];
                    y = geographicCoordinates[1];
                    z = geographicCoordinates[2];
                } else {
                    throw new JSONException("Invalid coordinate array format");
                }

                innerCoordinates[j] = new Coordinate(x, y, z);
            }

            jtsCoordinates[i] = innerCoordinates;
        }

        // Usa GeometryFactory per creare il poligono
        PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
        GeometryFactory geometryFactory = new GeometryFactory(precisionModel);

        // Crea LinearRing per ogni set di coordinate
        LinearRing outerRing = geometryFactory.createLinearRing(jtsCoordinates[0]);
        LinearRing[] innerRings = new LinearRing[jtsCoordinates.length - 1];
        for (int i = 1; i < jtsCoordinates.length; i++) {
            innerRings[i - 1] = geometryFactory.createLinearRing(jtsCoordinates[i]);
        }

        // Crea il poligono usando il LinearRing esterno e gli eventuali LinearRing interni
        Polygon polygon = geometryFactory.createPolygon(outerRing, innerRings);
        //polygon.setSRID(4326);

        return polygon;
    }



    private static Geometry buildLineStringFromJson(JSONObject geometryJson, Class<?> geometryBinding) throws JSONException, FactoryException, TransformException, IOException {
        JSONArray coordinates = geometryJson.getJSONArray("coordinates");
        GeometryFactory geometryFactory = new GeometryFactory();
        List<Geometry> allGeometries = new ArrayList<>();
        List<Coordinate> singleLine = new ArrayList<>();
        List<LineString> multiLine = new ArrayList<>();

        for (int i = 0; i < coordinates.length(); i++) {
            JSONArray coordObject = (JSONArray) coordinates.get(i);
            Object elem = coordObject.get(0);

            if (elem instanceof String || elem instanceof Number) {
                // Se è una coordinata singola
                JSONArray singleCoord = (JSONArray) coordObject;
                double x, y, z;
                if (singleCoord.length() >= 2) {
                    x = singleCoord.getDouble(0);
                    y = singleCoord.getDouble(1);
                    // Converte le coordinate cartesiane in coordinate geografiche
                    double[] geographicCoordinates = cartesianToGeographic(x, y, 0.0);
                    // Utilizza le coordinate geografiche convertite
                    x = geographicCoordinates[0];
                    y = geographicCoordinates[1];
                    z = geographicCoordinates[2];
                    singleLine.add(new Coordinate(x, y, z));
                } else {
                    throw new JSONException("Invalid coordinate array format");
                }
            } else if (elem instanceof JSONArray) {
                // Se è una lista di coordinate (LineString o MultiLineString)
                JSONArray multiCoords = (JSONArray) coordObject;
                List<Coordinate> multiLineCoords = new ArrayList<>();

                for (int j = 0; j < multiCoords.length(); j++) {
                    JSONArray singleCoord = multiCoords.getJSONArray(j);
                    double x, y, z;
                    if (singleCoord.length() >= 2) {
                        x = singleCoord.getDouble(0);
                        y = singleCoord.getDouble(1);
                        // Converte le coordinate cartesiane in coordinate geografiche
                        double[] geographicCoordinates = cartesianToGeographic(x, y, 0.0);
                        // Utilizza le coordinate geografiche convertite
                        x = geographicCoordinates[0];
                        y = geographicCoordinates[1];
                        z = geographicCoordinates[2];
                        multiLineCoords.add(new Coordinate(x, y, z));
                    } else {
                        throw new JSONException("Invalid coordinate array format");
                    }
                }

                if (multiLineCoords.size() > 1) {
                    // Se ci sono più coordinate, crea un MultiLineString
                    Coordinate[] innerCoords = multiLineCoords.toArray(new Coordinate[0]);
                    LineString lineString = geometryFactory.createLineString(innerCoords);
                    multiLine.add(lineString);
                } else {
                    throw new JSONException("Invalid coordinate array format");
                }
            } else {
                throw new JSONException("Invalid coordinate format");
            }
        }

        if (singleLine.size() > 1) {
            allGeometries.add(geometryFactory.createLineString(singleLine.toArray(new Coordinate[0])));
        }

        if(multiLine.size() > 1){
            LineString[] lineArray;
            lineArray = multiLine.toArray(new LineString[0]);
            allGeometries.add(geometryFactory.createMultiLineString(lineArray));
        } else if (multiLine.size() == 1) {
            // Se c'è solo una LineString, aggiungila direttamente
            allGeometries.addAll(multiLine);
        }

        // Unisci tutte le geometrie in un'unica sequenza
        List<LineString> filteredLineStrings = multiLine.stream()
                .filter(LineString.class::isInstance)
                .map(LineString.class::cast)
                .collect(Collectors.toList());

        // Crea l'array di LineString
        LineString[] lineArray = filteredLineStrings.toArray(new LineString[0]);

        // Crea il MultiLineString
        return geometryFactory.createMultiLineString(lineArray);
    }


    public static double[] cartesianToGeographic(double x, double y, double z) throws FactoryException, TransformException, JSONException {
        // Definisci il sistema di riferimento di origine (assumendo che sia lo stesso per le coordinate cartesiane)
        JSONObject properties = crsFile.getJSONObject("properties");

        Pattern pattern = Pattern.compile("EPSG::(\\d+)");
        Matcher matcher = pattern.matcher(properties.toString());

        String crs = null;
        if (matcher.find()) {
            crs = matcher.group(1);
        }

        CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:"+crs); // Sostituisci XXXX con il codice EPSG corretto
        CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:4326");
        // Costruisci un oggetto Geometry nel sistema di riferimento di origine
        PrecisionModel precisionModel = new PrecisionModel(PrecisionModel.FLOATING);
        GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 32633);

        Point sourcePoint = geometryFactory.createPoint(new Coordinate(x, y, z));

        // Trasforma il punto dal sistema di riferimento di origine a EPSG:4326
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
        Geometry targetGeometry = JTS.transform(sourcePoint, transform);

        // Estrai le coordinate geografiche
        double longitude = targetGeometry.getCoordinate().getX();
        double latitude = targetGeometry.getCoordinate().getY();

        // Debug
        //System.out.println("Coordinate prima della conversione: (" + x + ", " + y + ", " + z + ")");
        //System.out.println("Coordinate dopo la conversione: (" + longitude + ", " + latitude + ", " + z + ")");

        return new double[]{longitude, latitude, z};
    }
}

