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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    AsyncTaskGetData stations;
    AsyncTaskGetData velohStations;

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
            // TODO: Consider calling
            return;
        }
        mMap.setMyLocationEnabled(true);

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

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if(mLastLocation != null){
                double lat = mLastLocation.getLatitude();
                double lng = mLastLocation.getLongitude();
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(lat, lng), 16));
                drawMarkersForLocation();
            }

        }
    }

    float distToClosestBus = 10000000;
    public LatLng closestBus;
    double distToClosestVeloh = 10000000;
    public LatLng closestVeloh;
    float[] results = new float[1];

    private void drawMarkersForLocation() {
        //Draw markers for location being called
        Log.d("Markers", "draw markers being called");
        LatLng latlng;
        LatLng tempLastLocation;
        for(int i =0; i<stations.stations.size(); i++){
            latlng = stations.stations.get(i);
            if( mLastLocation !=null){
                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), latlng.latitude, latlng.longitude, results);
                if(results[0]<distToClosestBus){
                    distToClosestBus = results[0];
                    closestBus = latlng;
                }
            }

            mMap.addMarker(new MarkerOptions()
                    .position(latlng)
                    .icon(BitmapDescriptorFactory
                            .fromResource(R.mipmap.stationinactive)
                    )
                    .anchor(0.5f,0.5f));

        }
        for(int i = 0; i< velohStations.stations.size(); i++){
            latlng = velohStations.stations.get(i);
            if( mLastLocation!= null) {
                Location.distanceBetween(mLastLocation.getLatitude(), mLastLocation.getLongitude(), latlng.latitude, latlng.longitude, results);
                if(results[0]<distToClosestVeloh){
                    distToClosestVeloh = results[0];
                    closestVeloh = latlng;
                    Log.d("New Closest Veloh", closestVeloh.toString());
                }
            }

            mMap.addMarker(
                    new MarkerOptions().position(latlng)
                    .icon(BitmapDescriptorFactory
                        .fromResource(R.mipmap.velohmarker)
                    )
                    .anchor(0.5f,0.5f)
            );
        }
    }

    //Method of GoogleApiClient.ConnectionCallbacks
    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    //Method of LocationListener
    @Override
    public void onLocationChanged(Location location) {
    Log.d("LOCATION", "location changed");
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

//        //move map camera
         mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
         mMap.clear();
         drawMarkersForLocation();

//        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        }
    }

    //Method of GoogleApiClient.OnConnectionFailedListener
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
    }


    public void findClosest(View view) {
        mMap.addMarker(
                new MarkerOptions().position(closestBus)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.mipmap.stationactive)
                        )
                        .anchor(0.5f,0.5f)

        );
        mMap.addMarker(
                new MarkerOptions().position(closestVeloh)
                        .icon(BitmapDescriptorFactory
                                .fromResource(R.mipmap.velohactive)
                        )
                        .anchor(0.5f,0.5f)
        );
    }
}
