package unicorn.unique.micsmapapplication;

import android.location.Location;
import android.os.AsyncTask;
import android.os.Debug;
import android.util.Log;

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
import java.util.ArrayList;


/**
 * Created by HolySith on 14-Dec-16.
 */
// This class is used to fetch data from a URL.
public class AsyncTaskGetData extends AsyncTask<String, String, ArrayList<stationLocations>> {

    public AsyncResponse delegateForStations = null;
    private ArrayList<stationLocations> stations = new ArrayList<>();

    @Override
    protected ArrayList<stationLocations> doInBackground(String... params) {
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
                    double longitude = Double.parseDouble(parentObject.getString("longitude"));
                    double latitude = Double.parseDouble(parentObject.getString("latitude"));
                    int id = Integer.parseInt(parentObject.getString("id"));
                    stations.add(new stationLocations(new  LatLng(latitude, longitude),0.0f , id));

                }
                return stations;
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
    protected void onPostExecute(ArrayList<stationLocations> fetchedData) {
        delegateForStations.listProcessFinish(fetchedData);
        //super.onPostExecute(fetchedData);
    }

}
