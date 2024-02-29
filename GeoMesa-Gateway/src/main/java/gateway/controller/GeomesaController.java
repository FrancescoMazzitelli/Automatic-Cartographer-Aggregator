package gateway.controller;

import gateway.controller.clustering.EdgeClusteringController;
import gateway.controller.clustering.NodeClusteringController;
import gateway.services.GeoserverRESTService;
import gateway.utility.ThresholdData;
import gateway.utility.UploadData;
import gateway.services.GeomesaService;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.codehaus.jettison.json.*;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
public class GeomesaController {

    private static final String UPLOAD_DIR = "Shapefiles";
    private static final String OUTPUT_DIR = "Computed";
    public static String geojson;
    public static String filename;

    private static void flushDirs(){
        File dir1 = new File(UPLOAD_DIR);
        File dir2 = new File(OUTPUT_DIR);
        File[] dirs = new File[2];

        dirs[0] = dir1;
        dirs[1] = dir2;

        for(File dir : dirs){
            if(dir.listFiles().length > 0){
                for(File toDelete : dir.listFiles()){
                    toDelete.delete();
                }
            }
        }
    }
    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestBody UploadData fileData) throws IOException {
        flushDirs();
        RedisController.flushDB();
        if (fileData.getContent().length() == 0) {
            return new ResponseEntity<>("Il file è vuoto", HttpStatus.BAD_REQUEST);
        }
        geojson = fileData.getContent();
        filename = fileData.getFilename();
        String filePathGeojson = UPLOAD_DIR + "/"+ filename + ".geojson";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePathGeojson))) {
            writer.write(geojson);
        }
        System.out.println("File ricevuto con successo: "+ filename);
        return new ResponseEntity<>("File caricato con successo", HttpStatus.OK);
    }


    @PostMapping("/insert")
    private ResponseEntity<String> insert(@RequestBody ThresholdData fileData) throws Exception {
        RedisController.flushDB();
        GeoserverRESTService.featureReload();

        String requested_service = fileData.getService();
        int thresholdClustering = fileData.getThresholdClustering();
        int spectralClustering = fileData.getSpectralClustering();
        if(requested_service.toLowerCase().contains("raccolta")){
            String filePath = UPLOAD_DIR + "/"+ filename + ".geojson";
            String type2 = "Routput";

            String outputPath = OUTPUT_DIR + "/"+ filename + ".geojson";
            NodeClusteringController graphController = new NodeClusteringController();

            if(thresholdClustering == 1){
                double threshold = Double.parseDouble(fileData.getThreshold());
                graphController.partitionGraphWithThreshold(threshold, filePath, outputPath);
            }

            if(spectralClustering == 1){
                int numClsuters = Integer.parseInt(fileData.getThreshold());
                graphController.partitionGraphWithNumClusters(numClsuters, filePath, outputPath);
            }


            // Leggi il contenuto del file GeoJSON come stringa
            String geoJSONString2 = Files.readString(Paths.get(outputPath));

            // Converti la stringa GeoJSON in un oggetto JSONObject
            JSONObject json2 = new JSONObject(geoJSONString2);

            // Creazione del datastore
            DataStore dataStore2 = GeomesaService.createDataStore("redis://localhost:6379", "Routput");

            // Creazione dello schema del SimpleFeatureType
            SimpleFeatureType featureType2 = GeomesaService.getSchema(type2, json2);
            featureType2.getUserData().put("geomesa.mixed.geometries", "true");
            //featureType2.getDescriptor("geom").getUserData().put("precision", "6");

            // Creazione della lista di SimpleFeature
            List<SimpleFeature> features2 = GeomesaService.createSimpleFeatures(featureType2, json2, graphController.getCRS());

            // Creazione dello schema del datastore
            dataStore2.createSchema(featureType2);


            // Scrittura delle feature nel datastore
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer2 = dataStore2.getFeatureWriterAppend(featureType2.getTypeName(), Transaction.AUTO_COMMIT);


            for (SimpleFeature feature2 : features2) {
                if(feature2 != null) {
                    SimpleFeature toWrite2 = writer2.next();
                    toWrite2.setAttributes(feature2.getAttributes());
                    if(toWrite2 != null) {
                        writer2.write();
                    }
                }
            }
            writer2.close();
        }

        if(requested_service.toLowerCase().contains("strade")){
            String filePath = UPLOAD_DIR + "/"+ filename + ".geojson";
            String type2 = "Soutput";

            String outputPath = OUTPUT_DIR + "/"+ filename + ".geojson";
            EdgeClusteringController graphController = new EdgeClusteringController();

            if(thresholdClustering == 1){
                double threshold = Double.parseDouble(fileData.getThreshold());
                graphController.partitionGraphWithThreshold(threshold, filePath, outputPath);
            }

            if(spectralClustering == 1){
                int numClsuters = Integer.parseInt(fileData.getThreshold());
                graphController.partitionGraphWithNumClusters(numClsuters, filePath, outputPath);
            }

            // Leggi il contenuto del file GeoJSON come stringa
            String geoJSONString2 = Files.readString(Paths.get(outputPath));

            // Converti la stringa GeoJSON in un oggetto JSONObject
            JSONObject json2 = new JSONObject(geoJSONString2);

            // Creazione del datastore
            DataStore dataStore2 = GeomesaService.createDataStore("redis://localhost:6379", "Soutput");

            // Creazione dello schema del SimpleFeatureType
            SimpleFeatureType featureType2 = GeomesaService.getSchema(type2, json2);
            featureType2.getUserData().put("geomesa.mixed.geometries", "true");
            //featureType2.getDescriptor("geom").getUserData().put("precision", "6");

            // Creazione della lista di SimpleFeature
            List<SimpleFeature> features2 = GeomesaService.createSimpleFeatures(featureType2, json2, graphController.getCRS());

            // Creazione dello schema del datastore
            dataStore2.createSchema(featureType2);


            // Scrittura delle feature nel datastore
            FeatureWriter<SimpleFeatureType, SimpleFeature> writer2 = dataStore2.getFeatureWriterAppend(featureType2.getTypeName(), Transaction.AUTO_COMMIT);


            for (SimpleFeature feature2 : features2) {
                if(feature2 != null) {
                    SimpleFeature toWrite2 = writer2.next();
                    toWrite2.setAttributes(feature2.getAttributes());
                    if(toWrite2 != null) {
                        writer2.write();
                    }
                }
            }
            writer2.close();
        }

        return new ResponseEntity<>("File caricato con successo su Redis", HttpStatus.OK);
    }

}


