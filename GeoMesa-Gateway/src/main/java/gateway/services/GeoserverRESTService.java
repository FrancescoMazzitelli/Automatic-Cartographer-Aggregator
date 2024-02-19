package gateway.services;

import okhttp3.*;

import java.io.IOException;

public class GeoserverRESTService {

    public static Response featureReload() throws IOException {
        OkHttpClient client = new OkHttpClient();
        String credentials = Credentials.basic("admin", "geoserver");
        Request request = new Request.Builder()
                .url("http://localhost:8080/geoserver/rest/reset")
                .post(RequestBody.create(null, new byte[0]))
                //.addHeader("cookie", "JSESSIONID=node0teih0run4zfwttnnju6ih3zs23.node0")
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .addHeader("Authorization", credentials)
                .build();

        Response response = client.newCall(request).execute();;
        return response;
    }
}
