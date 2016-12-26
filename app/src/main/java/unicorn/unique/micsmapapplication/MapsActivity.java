package unicorn.unique.micsmapapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

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


import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnCameraChangeListener {


    private GoogleMap mMap;
    protected Location mLastLocation; //my Last location, used for onLocationChanged
    GoogleApiClient mGoogleApiClient; // API client
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    //Getting the list of stations upon start-up.
    AsyncTaskGetData stations;
    AsyncTaskGetVeloh velohStations;
    //List that fills according to the camera bounds
    ArrayList<stationProperties> stationsToDisplay = new ArrayList<>();
    ArrayList<velohProperties> velohStationsToDisplay = new ArrayList<>();
    //List of highlighted bus stops based on the slider.
    ArrayList<stationProperties> busListMaxDist = new ArrayList<>();
    ArrayList<velohProperties> velohListMaxDist = new ArrayList<>();

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
    float closestVelohDistance;

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
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
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
        stations = (AsyncTaskGetData) new AsyncTaskGetData().execute("https://api.tfl.lu/stations");
        //List of all veloh stations. I used pastebin instead of providing link from jcDecaux with the API key
        // because the API there provides JSON data with latitude as lat and longitude as lng. Since the
        // asyncTaskGetData class works with the names latitude and longitude it seems line a much easier task
        //for now.
        velohStations = (AsyncTaskGetVeloh) new AsyncTaskGetVeloh().execute("https://api.jcdecaux.com/vls/v1/stations?contract=Luxembourg&apiKey=d3369fd018b460c87544a5f04f0937a41e669a47");
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
                onCameraChange(cameraPosition);
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng),16));
                //Dynamically draw markers on Map.
                mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition cameraPosition) {
                        if(radiusOfSearch != null) {
                            radiusOfSearch.remove();
                        }
                        float[] results = new float[1];
                        Projection projection = mMap.getProjection();
                        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
                        for (int i = 0; i < stations.stations.size(); i++) {
                            LatLng position = stations.stations.get(i).latLng;
                            //Toast
                            if (bounds.contains(position)) {
                                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), position.latitude, position.longitude, results);
                                stationsToDisplay.add(new stationProperties(position, results[0]));
                                if(results[0]<closestStationDistance){
                                    closestStationDistance = results[0];
                                    closestStation = position;
                                }
                            }

                        }
                        for (int i = 0; i < velohStations.velohStations.size(); i++) {
                            LatLng position = velohStations.velohStations.get(i).latLng;
                            String name = velohStations.velohStations.get(i).name;
                            int avBike = velohStations.velohStations.get(i).availableBikes;
                            int avStands = velohStations.velohStations.get(i).availableStands;
                            Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), position.latitude, position.longitude, results);
                            if (bounds.contains(position)) {
                                velohStationsToDisplay.add(new velohProperties(position, name, avBike, avStands, results[0]));
                                if(results[0]<closestStationDistance){
                                    closestVelohDistance = results[0];
                                    closestVeloh = position;
                                }
                            }
                        }
                        drawMarkersForBusStations();
                        drawMarkersForVelohStation();

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

    private void drawMarkersForBusStations() {
        mMap.clear();
        float[] results = new float[1];
        //Log.d("Markers", "draw markers for Bus being called");
        LatLng latlng;
        for (int i = 0; i < stationsToDisplay.size(); i++) {
            latlng = stationsToDisplay.get(i).latLng;
            if (mLastLocation != null) {
                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), latlng.latitude, latlng.longitude, results);
                if (results[0] < 250) {
                    busListMaxDist.add(new stationProperties(latlng, results[0]));
                }
            }
            mMap.addMarker(new MarkerOptions()
                    .position(latlng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.stationinactive)
                    )
                    .anchor(0.5f, 0.5f));
        }
        Collections.sort(busListMaxDist, new Comparator<stationProperties>() {
            @Override
            public int compare(stationProperties o1, stationProperties o2) {
                return o1.distance.compareTo(o2.distance);
            }
        });

    }

    private void drawMarkersForVelohStation() {
        //velohStationsToDisplay.clear();
        //double distToClosestVeloh = 10000000;
        float[] results = new float[1];
        LatLng latlng;
        for (int i = 0; i < velohStationsToDisplay.size(); i++) {
            latlng = velohStationsToDisplay.get(i).latLng;
            String name = velohStationsToDisplay.get(i).name;
            int avBike = velohStationsToDisplay.get(i).availableBikes;
            int avStands = velohStationsToDisplay.get(i).availableStands;
            if (mLastLocation != null) {
                Location.distanceBetween(mLastLocation.getLatitude(),mLastLocation.getLongitude(),latlng.latitude,latlng.longitude,results);
                if (results[0] < 250) {
                    velohListMaxDist.add(new velohProperties(latlng, name,avBike, avStands, results[0]));
                    //Log.d("New Closest Veloh", closestVeloh.toString());
                }
            }
            mMap.addMarker(
                    new MarkerOptions().position(latlng)
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.mipmap.velohmarker)

                            )
                            .anchor(0.5f, 0.5f)
                    .title(name)
                    .snippet("Available Bikes: "+ avBike + "\n" + "Available Stands: "+ avStands)
            );
        }
        Collections.sort(velohListMaxDist, new Comparator<velohProperties>() {
            @Override
            public int compare(velohProperties o1, velohProperties o2) {
                return o1.distance.compareTo(o2.distance);
            }
        });

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

    public void findClosest(View view) {
        mMap.clear();
        drawCircle();
        for (int i = 0; i< velohStationsToDisplay.size(); i++) {
            velohProperties veloh = velohStationsToDisplay.get(i);
            mMap.addMarker(new MarkerOptions()
                    .position(veloh.latLng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.velohmarker)
                    ).title(veloh.name)
                    .snippet("Available Bikes: "+ veloh.availableBikes+ "\n" + "Available Stands: "+ veloh.availableStands)
                    .anchor(0.5f, 0.5f));
        }
        for (int i = 0; i< stationsToDisplay.size(); i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(stationsToDisplay.get(i).latLng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.stationinactive)
                    )
                    .anchor(0.5f, 0.5f));

        }

        if (busListMaxDist.size()!=0) {
            for (int i = 0; i < busListMaxDist.size(); i++) {
                if (busListMaxDist.get(i).distance <= compareDistance*25) {
                    mMap.addMarker(
                            new MarkerOptions().position(busListMaxDist.get(i).latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.stationactive)
                                    )
                                    .anchor(0.5f, 0.5f)

                    );
                } else{
                    Context context = getApplicationContext();
                    CharSequence text = "Closest bus is "+ busListMaxDist.get(0).distance + " m";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    mMap.addMarker(
                            new MarkerOptions().position(busListMaxDist.get(0).latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.stationactive)
                                    )
                                    .anchor(0.5f, 0.5f)

                    );
                }
            }
        }  else {
            mMap.addMarker(
                    new MarkerOptions().position(closestStation)
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.mipmap.stationactive)
                            )
                            .anchor(0.5f, 0.5f)

            );
        }

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
    private void velohWithinMaxDist(){
        drawCircle();

        for (int i = 0; i< stationsToDisplay.size(); i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(stationsToDisplay.get(i).latLng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.stationinactive)
                    )
                    .anchor(0.5f, 0.5f));

        }
        for (int i = 0; i< velohStationsToDisplay.size(); i++) {
            velohProperties veloh = velohStationsToDisplay.get(i);
            mMap.addMarker(new MarkerOptions()
                    .position(veloh.latLng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.velohmarker)
                    ).title(veloh.name)
                    .snippet("Available Bikes: "+ veloh.availableBikes+ "\n" + "Available Stands: "+ veloh.availableStands)
                    .anchor(0.5f, 0.5f));
        }
        if (velohListMaxDist.size()!=0) {
            for (int i = 0; i < velohListMaxDist.size(); i++) {
                velohProperties veloh = velohListMaxDist.get(i);
                if (velohListMaxDist.get(i).distance <= compareDistance*25) {
                    mMap.addMarker(
                            new MarkerOptions().position(veloh.latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.velohactive)
                                    )
                                    .title(veloh.name)
                                    .snippet("Available Bikes: "+ veloh.availableBikes+ "\n" + "Available Stands: "+ veloh.availableStands)
                                    .anchor(0.5f, 0.5f)

                    );
                } else {
                    Context context = getApplicationContext();
                    CharSequence text = "Closest veloh is "+ velohListMaxDist.get(0).distance + " m";
                    int duration = Toast.LENGTH_SHORT;

                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                    veloh = velohListMaxDist.get(0);
                    mMap.addMarker(
                            new MarkerOptions().position(veloh.latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.velohactive)
                                    )
                                    .title(veloh.name)
                                    .snippet("Available Bikes: "+ veloh.availableBikes+ "\n" + "Available Stands: "+ veloh.availableStands)
                                    .anchor(0.5f, 0.5f)

                    );
                }
            }
        }

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

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
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


    public void onClosestVeloh(View view) {
        mMap.clear();
        velohWithinMaxDist();
    }
}

