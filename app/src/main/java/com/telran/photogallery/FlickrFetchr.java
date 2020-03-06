package com.telran.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FlickrFetchr {
    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "bdc36256a32c27b9f74764cadbe40910";

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection =  (HttpURLConnection) url.openConnection();
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                throw new IOException(connection.getResponseMessage() + ": with" + urlSpec);
            }
            int bytesread = 0;
            byte[] buffer = new byte[1024];
            while((bytesread = in.read(buffer)) > 0){
                out.write(buffer,0,bytesread);
            }
            out.close();
            return out.toByteArray();
        }finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(int page){
        List<GalleryItem> items = new ArrayList<>();

        try{
            String url = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page",page+"")
                    .build().toString();

            String jsonString = getUrlString(url);

            JSONObject jsonBody = new JSONObject(jsonString);
            Log.d(TAG, "fetchItems: "+jsonBody);
            parseItems(items,jsonBody);

        }catch (IOException io){
            Log.e(TAG, "Failed to fetch items", io);
        }catch (JSONException js){
            Log.e(TAG, "Failed to parse JSON", js);
        }
        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws JSONException {
        JSONObject photosJson = jsonBody.getJSONObject("photos");
        JSONArray photosJsonArr = photosJson.getJSONArray("photo");
        for (int i = 0; i < photosJsonArr.length(); i++) {
            JSONObject photoJson = photosJsonArr.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJson.getString("id"));
            item.setCaption(photoJson.getString("title"));
            if (!photoJson.has("url_s")){
                continue;
            }
            item.setUrl(photoJson.getString("url_s"));
            items.add(item);
        }
    }
}
