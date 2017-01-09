package unicorn.unique.micsmapapplication;

import android.os.AsyncTask;

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
import android.util.Log;

/**
 * Created by HolySith on 27-Dec-16.
 */

public class AsyncRealTimeBus extends AsyncTask<String, String, String> {

    public AsyncResponse delegate = null;
    private String stationsInfo = "";
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
                JSONObject parentObject = new JSONObject(JSONData);
                JSONArray parent = parentObject.getJSONArray("Departure");
                String stationName = parent.getJSONObject(0).getString("stop");
                stationsInfo+= "<b>"+stationName+"</b>"+"<br /><br />";
                for(int i =0; i< parent.length(); i++) {
                    JSONObject child = parent.getJSONObject(i);
                    String name = child.getString("name");
                    String time = child.getString("time");
                    String direction = child.getString("direction");

                    stationsInfo += "Number: "+ name + "<br />Time: " + time+ "<br />Destination: "+ direction +"<br />";
                    stationsInfo += "<br />";
                }
                return stationsInfo;
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
        delegate.processFinish(s);

    }
}
