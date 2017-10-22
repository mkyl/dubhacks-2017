package com.dubhacks.moderately_modest_group.readingaid;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.*;
import java.util.*;

/**
 * Created by maria on 10/21/2017.
 */

public class DigitizeText {
    public static final String subscriptionKey = "b6dec940d6944d59bbe081ecab0def94";
    public static final String uriBase = "https://westcentralus.api.cognitive.microsoft.com/vision/v1.0/ocr";
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
        request.setHeader("Content-Type", "image/jpeg");
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

            // TODO: what to do with this?
            return json;
        }

        throw new Exception("digitizeText method could not digitize text.");
    }

    public static List<List<String>> jsonToStringArray(JSONObject data) throws JSONException {
        JSONArray regions = data.getJSONArray("regions");
        List<List<String>> regionList = new ArrayList<>();

        for (int i = 0; i < regions.length(); i++) {
            List<String> wordList = new ArrayList<>();
            JSONObject region = regions.getJSONObject(i);
            JSONArray lines = region.getJSONArray("lines");
            for (int j = 0; j < lines.length(); j++) {
                JSONObject line = lines.getJSONObject(j);
                JSONArray words = line.getJSONArray("words");
                for (int k = 0; k < words.length(); k++) {
                    JSONObject word = words.getJSONObject(k);
                    String text = word.getString("text");
                    wordList.add(text);
                }
            }

            regionList.add(wordList);
        }

        return regionList;
    }
}

