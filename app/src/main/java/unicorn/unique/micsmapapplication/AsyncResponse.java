package unicorn.unique.micsmapapplication;
import java.util.ArrayList;

/**
 * Created by HolySith on 27-Dec-16.
 */

public interface AsyncResponse {
    void processFinish(String output);
    void listProcessFinish(ArrayList<stationLocations> out);
    void velohlistProcessFinish(ArrayList<stationLocations> out);
    void velohRTProcessFinish(String output);
}
