package unicorn.unique.micsmapapplication;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by HolySith on 19-Dec-16.
 */

public class stationProperties {
    LatLng latLng;
    Float distance;
    //Add other properties here.

    public stationProperties(LatLng latLng, Float distance){
        this.latLng = latLng;
        this.distance = distance;
    }
}
