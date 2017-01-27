package unicorn.unique.micsmapapplication;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.google.android.gms.fitness.data.Application;
import com.google.android.gms.games.appcontent.AppContentAction;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by HolySith on 11-Jan-17.
 */

public class AsyncRealTimeVeloh extends AsyncTask<String, String, String> {

    public AsyncResponse delegate = null;
    private String velohInfo = "";

    @Override
    protected String doInBackground(String... params) {

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;


        try {
            URL stationsURL = new URL(params[0]);
            urlConnection = (HttpURLConnection) stationsURL.openConnection();
            try {
                urlConnection.connect();
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                reader = new BufferedReader(new InputStreamReader(in));
                StringBuffer buffer = new StringBuffer();

                String line = "";
                while((line = reader.readLine()) !=null){
                    buffer.append(line);
                }
                //Save the buffer value into a string.
                String JSONData = buffer.toString();
                JSONArray parentArray = new JSONArray(JSONData);

                for(int i =0; i< parentArray.length(); i++) {
                    JSONObject parentObject = parentArray.getJSONObject(i);
                    int id = Integer.parseInt(parentObject.getString("number"));
                    if(id == Integer.parseInt(params[1])){
                        String velohStationName = parentObject.getString("name");

                        String address = parentObject.getString("address");
                        Integer bike_stands = Integer.parseInt(parentObject.getString("bike_stands"));
                        Integer available_bike_stands = Integer.parseInt(parentObject.getString("available_bike_stands"));
                        Integer available_bikes = Integer.parseInt(parentObject.getString("available_bikes"));
                        velohInfo = "<b>" + velohStationName + "</b>" + "<br />"
                                    + "<i>"+ address + "</i>" + "<br />" + "<br />"
                                    + "Available Bikes: " + available_bikes + "<br />"
                                    + "Available Bike Stands: " + available_bike_stands + "/" + bike_stands;
                    }
                }

                return velohInfo;
                //We now return the complete JSON data fetched from the URL.
                //return buffer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                urlConnection.disconnect();
                if(reader != null) {
                    reader.close();
                }
            }
        } catch (IOException e){}
        return null;
    }


    @Override
    protected void onPostExecute(String s) {
        delegate.velohRTProcessFinish(s);

    }
}
