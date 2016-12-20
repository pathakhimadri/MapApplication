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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener, GoogleMap.OnCameraChangeListener {

    public LatLng closestBus;
    public LatLng closestVeloh;
    protected Location mLastLocation;
    GoogleApiClient mGoogleApiClient;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    AsyncTaskGetData stations;
    AsyncTaskGetData velohStations;
    ArrayList<LatLng> stationsToDisplay = new ArrayList<>();
    ArrayList<stationProperties> busListMaxDist = new ArrayList<>();
    ArrayList<LatLng> velohStationsToDisplay = new ArrayList<>();
    ArrayList<stationProperties> velohListMaxDist = new ArrayList<>();
    CameraPosition cameraPosition;
    private GoogleMap mMap;
    SeekBar seekBar;
    Button buttonForClosest;
    int compareDistance;

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
        velohStations = (AsyncTaskGetData) new AsyncTaskGetData().execute("http://pastebin.com/raw/S5w1nvxw");
    }

    //Implement method of OnMapReady Callback
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                        stationsToDisplay.clear();
                        velohStationsToDisplay.clear();
                        Projection projection = mMap.getProjection();
                        LatLngBounds bounds = projection.getVisibleRegion().latLngBounds;
                        for (int i = 0; i < stations.stations.size(); i++) {
                            LatLng position = stations.stations.get(i);
                            if (bounds.contains(position)) {
                                stationsToDisplay.add(position);
                                //mMap.addMarker(new MarkerOptions().position(position));
                                //Log.d("Camera", "Marker Count: " + count);
                            }

                        }
                        for (int i = 0; i < velohStations.stations.size(); i++) {
                            LatLng position = velohStations.stations.get(i);
                            if (bounds.contains(position)) {
                                velohStationsToDisplay.add(position);
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
        //float distToClosestBus = 10000000;
        float[] results = new float[1];
        //Log.d("Markers", "draw markers for Bus being called");
        LatLng latlng;
        for (int i = 0; i < stationsToDisplay.size(); i++) {
            latlng = stationsToDisplay.get(i);
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
        //double distToClosestVeloh = 10000000;
        float[] results = new float[1];
        LatLng latlng;
        for (int i = 0; i < velohStationsToDisplay.size(); i++) {
            latlng = velohStationsToDisplay.get(i);
            if (mLastLocation != null) {
                Location.distanceBetween(mLastLocation.getLatitude(),mLastLocation.getLongitude(),latlng.latitude,latlng.longitude,results);
                if (results[0] < 250) {
                    velohListMaxDist.add(new stationProperties(latlng, results[0]));
                    //Log.d("New Closest Veloh", closestVeloh.toString());
                }
            }
            mMap.addMarker(
                    new MarkerOptions().position(latlng)
                            .icon(BitmapDescriptorFactory
                                    .fromResource(R.mipmap.velohmarker)
                            )
                            .anchor(0.5f, 0.5f)
            );
        }
        Collections.sort(velohListMaxDist, new Comparator<stationProperties>() {
            @Override
            public int compare(stationProperties o1, stationProperties o2) {
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
        for (int i = 0; i< stationsToDisplay.size(); i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(stationsToDisplay.get(i))
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
                } else {
                    mMap.addMarker(
                            new MarkerOptions().position(busListMaxDist.get(0).latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.stationactive)
                                    )
                                    .anchor(0.5f, 0.5f)

                    );
                }
            }
        }
        velohWithinMaxDist();

    }
    private void velohWithinMaxDist(){
        for (int i = 0; i< velohStationsToDisplay.size(); i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(velohStationsToDisplay.get(i))
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.velohmarker)
                    )
                    .anchor(0.5f, 0.5f));
        }
        if (velohListMaxDist.size()!=0) {
            for (int i = 0; i < velohListMaxDist.size(); i++) {
                if (velohListMaxDist.get(i).distance <= compareDistance*25) {
                    mMap.addMarker(
                            new MarkerOptions().position(velohListMaxDist.get(i).latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.velohactive)
                                    )
                                    .anchor(0.5f, 0.5f)

                    );
                } else {
                    mMap.addMarker(
                            new MarkerOptions().position(velohListMaxDist.get(0).latLng)
                                    .icon(BitmapDescriptorFactory
                                            .fromResource(R.mipmap.velohactive)
                                    )
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
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                compareDistance = progress;
                if (progress != 0) {
                    buttonForClosest.setText("Find stations within "+Integer.toString(progress*25) +" m");
                } else{
                    buttonForClosest.setText("Find closest station");
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


}

