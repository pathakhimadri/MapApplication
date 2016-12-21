package unicorn.unique.micsmapapplication;

import com.google.android.gms.maps.model.LatLng;
/**
 * Created by HolySith on 21-Dec-16.
 */

public class velohProperties {
    LatLng latLng;
    String name;
    Float distance;
    int availableBikes;
    int availableStands;

    public velohProperties(LatLng latLng, String name, int availableBikes, int availableStands, float distance){
        this.latLng = latLng;
        this.name = name;
        this.availableBikes = availableBikes;
        this.availableStands = availableStands;
        this.distance = distance;
    }

}
