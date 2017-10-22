package com.dubhacks.moderately_modest_group.readingaid;

import android.util.Log;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 * Created by maria on 10/21/2017.
 */

public class DigitizeText {
    public static final String subscriptionKey = "b6dec940d6944d59bbe081ecab0def94";
    public static final String uriBase = "https://westus.api.cognitive.microsoft.com/vision/v1.0/ocr";
    // or this uriBase from Kayali? https://westus.api.cognitive.microsoft.com/vision/v1.0


    @SuppressWarnings("deprecation")
    public static JSONObject digitizeText(byte[] imageBytes) throws Exception {
        HttpClient httpClient = new DefaultHttpClient();
        URIBuilder uriBuilder = new URIBuilder(uriBase);

        uriBuilder.setParameter("language", "unk");
        uriBuilder.setParameter("detectOrientation ", "true");

        // Request parameters.
        URI uri = uriBuilder.build();
        HttpPost request = new HttpPost(uri);

        // Request headers.
        //request.setHeader("Content-Type", "image/jpeg");
        request.setHeader("Content-Type", "application/octet-stream");
        request.setHeader("Ocp-Apim-Subscription-Key", subscriptionKey);

        // Request body.
        ByteArrayEntity byteEntity = new ByteArrayEntity(imageBytes);
        request.setEntity(byteEntity);

        // Execute the REST API call and get the response entity.
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();

        if (entity != null)
        {
            // Format and display the JSON response.
            String jsonString = EntityUtils.toString(entity);
            JSONObject json = new JSONObject(jsonString);
            System.out.println("REST Response:\n");
            System.out.println(json.toString(2));
            Log.i("ReadingAid", json.toString(4) );

            // TODO: what to do with this?
            return json;
        }

        throw new Exception("digitizeText method could not digitize text.");
    }

}

