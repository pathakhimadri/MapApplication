package unicorn.unique.micsmapapplication;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by HolySith on 26-Dec-16.
 */

public class stationLocations {
    LatLng latLng;
    Float distance;
    int id;
    //Add other properties here.

    public stationLocations(LatLng latLng, Float distance, int id){
        this.latLng = latLng;
        this.distance = distance;
        this.id = id;
    }
}
