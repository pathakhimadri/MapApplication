package unicorn.unique.micsmapapplication;

import java.io.IOException;
import java.util.HashMap;
import android.Manifest;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;
import android.net.Uri;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnCameraChangeListener, GoogleMap.OnMarkerClickListener,
        AsyncResponse,GoogleMap.OnMapLongClickListener, RoutingListener  {


    private GoogleMap mMap;
    protected Location mLastLocation; //my Last location, used for onLocationChanged
    GoogleApiClient mGoogleApiClient; // API client
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    //Getting the list of stations upon start-up.

    AsyncTaskGetData stations;
    ArrayList<stationLocations> allStations;

    AsyncTaskGetVeloh velohStations;
    ArrayList<stationLocations> allVelohLocations;

    ArrayList<stationLocations> stationsToDisplay = new ArrayList<>();
    ArrayList<stationLocations> velohStationsToDisplay = new ArrayList<>();

    //List of highlighted bus stops based on the slider.
    ArrayList<stationLocations> busListMaxDist = new ArrayList<>();
    ArrayList<stationLocations> velohListMaxDist = new ArrayList<>();

    CameraPosition cameraPosition;

    //UI elements.
    SeekBar seekBar;
    Button buttonForClosest;
    Button buttonForVeloh;

    Circle radiusOfSearch;
    LatLng closestStation = null;
    LatLng closestVeloh;

    int compareDistance;
    float closestStationDistance;
    int closestStationId;

    float closestVelohDistance;
    int closestVelohId;

    private ArrayList<Polyline> polylines;

    //HashMap that holds a MARKER and it's id.
    private HashMap<String, Integer> mHashMap = new HashMap<>();

    //Handling configuration Changes.

    public static void clearCache(Context context){
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir){
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //The list of all bus stations.
        stations = new AsyncTaskGetData();
        stations.delegateForStations = this;
        stations.execute("http://travelplanner.mobiliteit.lu/hafas/query.exe/dot?performLocating=2&tpl=stop2csv&look_maxdist=150000&look_x=6112550&look_y=49610700&stationProxy=yes.txt");
        //Get list of all veloh stations
        velohStations = new AsyncTaskGetVeloh();
        velohStations.delegateForVeloh = this;
        velohStations.execute("https://api.jcdecaux.com/vls/v1/stations?contract=Luxembourg&apiKey=d3369fd018b460c87544a5f04f0937a41e669a47");
        if(savedInstanceState!=null){
            cameraChanged();
        }
    }

    //Implement method of OnMapReady Callback
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    12);
            permissionsCheck();
            mMap.setMyLocationEnabled(true);
        } else {
            permissionsCheck();
            mMap.setMyLocationEnabled(true);
        }
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    //Permissions check
    private void permissionsCheck(){
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    // Method of GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        seekBarFunctions();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {

                double lat = mLastLocation.getLatitude();
                double lng = mLastLocation.getLongitude();

                onLocationChanged(mLastLocation);
                cameraPosition = new CameraPosition.Builder().target(new LatLng(lat, lng)).build();
                //Dynamically draw markers on Map.

                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        float minZoom = 14.5f;
                        if(cameraPosition.zoom < minZoom){
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(16.0f));
                        }
                        cameraChanged();
                    }
                });
            } else {
                permissionsCheck();
                mMap.setMyLocationEnabled(true);

                Location newLocation = new Location("");
                LatLng latlng = new LatLng(49.610456, 6.130668);

                newLocation.setLatitude(latlng.latitude);
                newLocation.setLongitude(latlng.longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latlng, 16));
                onLocationChanged(newLocation);
            }

        }
    }
    //TODO: Check why the bounds are not updating upon orientation change!

    private void cameraChanged(){
        if(radiusOfSearch != null) {
            radiusOfSearch.remove();
        }

        float[] results = new float[1];
        Projection projection = mMap.getProjection();
        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
        Log.d("Bounds", bounds.toString());
        stationsToDisplay.clear();
        velohStationsToDisplay.clear();
        for (int i = 0; i < allStations.size(); i++) {
            stationLocations newStation = allStations.get(i);
            LatLng position = newStation.latLng;
            int id = newStation.id;
            if (bounds.contains(position)) {
                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), position.latitude, position.longitude, results);
                stationsToDisplay.add(new stationLocations(position, results[0], id));
                if(results[0]<closestStationDistance){
                    closestStationDistance = results[0];
                    closestStation = position;
                    closestStationId = id;
                }
            }

        }
        for (int i = 0; i < allVelohLocations.size(); i++) {
            stationLocations newStation = allVelohLocations.get(i);
            LatLng position = newStation.latLng;
            int id = newStation.id;
            Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), position.latitude, position.longitude, results);
            if (bounds.contains(position)) {
                velohStationsToDisplay.add(new stationLocations(position, results[0], id));
                if(results[0]<closestVelohDistance){
                    closestVelohDistance = results[0];
                    closestVeloh = position;
                    closestVelohId = id;
                }
            }
        }
        drawMarkersForBusStations();
        drawMarkersForVelohStation();
    }

    private void drawMarkersForBusStations() {
        mHashMap.clear();
        mMap.clear();
        float[] results = new float[1];
        //Log.d("Markers", "draw markers for Bus being called");
        LatLng latlng;
        Marker marker;
        int id;
        for (int i = 0; i < stationsToDisplay.size(); i++) {
            latlng = stationsToDisplay.get(i).latLng;
            id = stationsToDisplay.get(i).id;
            if (mLastLocation != null) {
                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), latlng.latitude, latlng.longitude, results);
                if (results[0] < 250) {
                    busListMaxDist.add(new stationLocations(latlng, results[0], id));
                }
            }
            marker = addMarker(latlng, "stationinactive");
            mHashMap.put(marker.getId(),id);
        }
        //If there are no bus stations within 250m (the search distance, then add the closest one)
        if(busListMaxDist.size()==0){
            busListMaxDist.add(new stationLocations(closestStation, closestStationDistance, closestStationId));
        }
        Collections.sort(busListMaxDist, new Comparator<stationLocations>() {
            @Override
            public int compare(stationLocations o1, stationLocations o2) {
                return o1.distance.compareTo(o2.distance);
            }
        });
    }

    private void drawMarkersForVelohStation() {
        float[] results = new float[1];
        LatLng latlng;
        Marker marker;
        int id;
        for (int i = 0; i < velohStationsToDisplay.size(); i++) {
            latlng = velohStationsToDisplay.get(i).latLng;
             id = velohStationsToDisplay.get(i).id;
            if (mLastLocation != null) {
                Location.distanceBetween(mLastLocation.getLatitude(),mLastLocation.getLongitude(),latlng.latitude,latlng.longitude,results);
                if (results[0] < 250) {
                    velohListMaxDist.add(new stationLocations(latlng, results[0], id));
                }
            }
            marker = addMarker(latlng, "velohmarker");
            mHashMap.put(marker.getId(),id);
        }
        if(velohListMaxDist.size()==0){
            velohListMaxDist.add(new stationLocations(closestVeloh, closestVelohDistance,closestVelohId));
        }
        Collections.sort(velohListMaxDist, new Comparator<stationLocations>() {
            @Override
            public int compare(stationLocations o1, stationLocations o2) {
                return o1.distance.compareTo(o2.distance);
            }
        });
    }

    public void findClosest(View view) {
        mMap.clear();
        mHashMap.clear();
        drawCircle();

        for (int i = 0; i< velohStationsToDisplay.size(); i++) {
            stationLocations veloh = velohStationsToDisplay.get(i);
            addMarker(veloh.latLng, "velohmarker");
        }

        stationLocations stationsToDisp;
        int id;
        Marker marker;

        //First since map is cleared, draw all the markers again.
        for (int i = 0; i< stationsToDisplay.size(); i++) {
            stationsToDisp = stationsToDisplay.get(i);
            id = stationsToDisp.id;
            marker = addMarker(stationsToDisp.latLng, "stationinactive");
            mHashMap.put(marker.getId(), id);
        }

        int numberOfBusesInPerimeter = 0;
        //Add the markers whose distance fit the queried distance.
        if (busListMaxDist.size()!=0) {
            for (int i = 0; i < busListMaxDist.size(); i++) {
                if (busListMaxDist.get(i).distance <= compareDistance * 25) {
                    id = busListMaxDist.get(i).id;
                    marker = addMarker(busListMaxDist.get(i).latLng, "stationactive");
                    mHashMap.put(marker.getId(), id);
                    numberOfBusesInPerimeter++;
                }
            }
            if(numberOfBusesInPerimeter==0) {
                Context context = getApplicationContext();
                CharSequence text = "Closest bus is "+ busListMaxDist.get(0).distance + " m";
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
                marker = addMarker(busListMaxDist.get(0).latLng, "stationactive");
                mHashMap.put(marker.getId(), busListMaxDist.get(0).id);

            }
        }

    }

    public void onClosestVeloh(View view) {
        mMap.clear();
        mHashMap.clear();
        drawCircle();

        for (int i = 0; i< stationsToDisplay.size(); i++) {
            stationLocations bus = stationsToDisplay.get(i);
            addMarker(bus.latLng, "stationinactive");
        }

        stationLocations stationsToDisp;
        int id;
        Marker marker;

        //First since map is cleared, draw all the markers again.
        for (int i = 0; i< velohStationsToDisplay.size(); i++) {
            stationsToDisp = velohStationsToDisplay.get(i);
            id = stationsToDisp.id;
            marker = addMarker(stationsToDisp.latLng, "velohmarker");
            mHashMap.put(marker.getId(), id);
        }

        int numberOfVelohInPerimeter = 0;
        if (velohListMaxDist.size()!=0) {
            for (int i = 0; i < velohListMaxDist.size(); i++) {
                if (velohListMaxDist.get(i).distance <= compareDistance * 25) {
                    id = velohListMaxDist.get(i).id;
                    marker = addMarker(velohListMaxDist.get(i).latLng, "velohactive");
                    mHashMap.put(marker.getId(), id);
                    numberOfVelohInPerimeter++;
                }
            }
            if(numberOfVelohInPerimeter==0) {
               Context context = getApplicationContext();
                CharSequence text = "Closest Veloh! is "+ velohListMaxDist.get(0).distance + " m";
                Toast toast = Toast.makeText(context, text, Toast.LENGTH_SHORT);
                toast.show();
                marker = addMarker(velohListMaxDist.get(0).latLng, "velohactive");
                mHashMap.put(marker.getId(), velohListMaxDist.get(0).id);

            }
        }

    }

    private Marker addMarker(LatLng latLng, String resource){
        Marker marker;
        marker = mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory
                        .fromResource(getResources().getIdentifier(resource,"mipmap", "unicorn.unique.micsmapapplication" ))
                )
                .anchor(0.5f, 0.5f));
        return marker;
    }

    //Method of GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    //Method of LocationListener
    @Override
    public void onLocationChanged(Location location) {
        //mLocationRequest = new LocationRequest();
        mMap.clear();
        closestStationDistance = 100000;
        closestVelohDistance = 100000;
        busListMaxDist.clear();
        velohListMaxDist.clear();
        Log.d("onLocationChanged", "Location being changed");
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        //TODO: Reset camera zoom every time.
        //move map camera
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));

/*        IF we would want to stop receiving location updates we'd use the below line.
          This is especially necessary for my android phone which keeps getting on location changed
          every second and resets the active markers
//        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//
        }
*/
    }


    //Method of GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
            return;
        }
        mMap.setMyLocationEnabled(true);
    }



    private void drawCircle(){
        if(radiusOfSearch != null) {
            radiusOfSearch.remove();
        }
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()))
                .radius(compareDistance*25)
                .fillColor(0x96BBDEFB)
                .strokeWidth(5)
                .strokeColor(0xFF64B5F6)
                ;

        radiusOfSearch = mMap.addCircle(circleOptions);

    }

    @Override
    protected void onDestroy(){
    super.onDestroy();
    try{
        clearCache(this);
    }catch (Exception e){
        e.printStackTrace();
    }

}

    private void seekBarFunctions(){
        seekBar = (SeekBar) findViewById(R.id.distanceSeek);
        buttonForClosest = (Button) findViewById(R.id.button7);
        buttonForVeloh = (Button) findViewById(R.id.veloh);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                compareDistance = progress;
                if (progress != 0) {
                    buttonForClosest.setText("Find stations within "+Integer.toString(progress*25) +" m");
                    buttonForVeloh.setText("Find Veloh within "+Integer.toString(progress*25) +" m");
                } else{
                    buttonForClosest.setText("Find closest station");
                    buttonForVeloh.setText("Find Closest Veloh");
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        try {
            String id = marker.getId();
            int stationID = mHashMap.get(id);

            Intent intent = new Intent(MapsActivity.this, PopUp.class);
            intent.putExtra("stationID", stationID);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void processFinish(String output) {
    }

    @Override
    public void listProcessFinish(ArrayList<stationLocations> out) {
        allStations = out;
    }

    @Override
    public void velohlistProcessFinish(ArrayList<stationLocations> out) {
        allVelohLocations = out;
    }

    @Override
    public void velohRTProcessFinish(String output) {

    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
    }
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    public void geoLocate(View view) throws IOException {

        EditText editText = (EditText) findViewById(R.id.editText);
        try {
            String searchedLocation = editText.getText().toString();

            //Takes the text location and converts it into LAT and LONG
            Geocoder geocoder = new Geocoder(this);

            //Where 1 is the number of results to return. We will need a few more later on.
            List<Address> list = geocoder.getFromLocationName(searchedLocation, 1);
            android.location.Address address = list.get(0);

            //This part is not needed however it looks pretty neat.
            String locality = address.getLocality();

            Toast.makeText(this, locality, Toast.LENGTH_SHORT).show();

            double lat = address.getLatitude();
            double lng = address.getLongitude();

            LatLng ll = new LatLng(lat, lng);
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
        } catch (IndexOutOfBoundsException e) {

        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        traceRoute(mLastLocation,latLng);
    }

    private void traceRoute(Location mLastLocation, LatLng destination){
        Routing routing = new Routing.Builder()
                .travelMode(Routing.TravelMode.WALKING)
                .withListener(this)
                .waypoints(new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()), destination)
                .build();

        // execute the request
        routing.execute();
    }

    @Override
    public void onRoutingFailure(RouteException e) {

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        // remove the previous polylines
        if (polylines != null && polylines.size() > 0) {
            for (Polyline poly : polylines) {
                poly.remove();
            }
        }

        // create a new array of polylines
        polylines = new ArrayList<>();

        // select the shortest path
        Route path = route.get(shortestRouteIndex);

        // add polylines to the map and the array
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(0x92303F9F);
        polyOptions.width(13);
        polyOptions.addAll(path.getPoints());
        Polyline polyline = mMap.addPolyline(polyOptions);
        polylines.add(polyline);
    }

    @Override
    public void onRoutingCancelled() {

    }
}
