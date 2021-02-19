package com.bikechow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapTappedEventArgs;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapTappedListener;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;

import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnMapTappedListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private MapView mMapView;
    private MapElementLayer elementLayer;

    private LocationManager locationManager;
    private Geopoint startLocation = Data.RICHMOND;

    private Requests requestCreator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);  //
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);

        // Create our requests class, which will handle GET and POST requests
        requestCreator = new Requests(this);

        getLocationPermissions(); // Attempt to get our location permissions

        mMapView.setScene(MapScene.createFromLocationAndRadius(startLocation, Data.DEFAULT_RADIUS_IN_METERS), MapAnimationKind.LINEAR); // Moves the camera to the specified position with the specified animation

        mMapView.addOnMapTappedListener(this); // Add an on tap listener (See the implements)

        elementLayer = new MapElementLayer(); // Create a layer for drawing icons  on (Do we need another one for drawing routes?)
        mMapView.getLayers().add(elementLayer); // Add this layer to our map view
    }

    // Function for adding icons to map view with Icon Data (the preferred way)
    private MapIcon addIcon(IconData iconData) {
        MapIcon mapIcon = new MapIcon();
        mapIcon.setLocation(iconData.location);
        mapIcon.setImage(iconData.image);
        mapIcon.setTitle(iconData.title);

        elementLayer.getElements().add(mapIcon);

        return mapIcon;
    }


    @Override
    public boolean onMapTapped(MapTappedEventArgs mapTappedEventArgs) {
        addIcon(new IconData(mapTappedEventArgs.location));

        return true;
    }


    // When we have permissions to access locations
    @SuppressLint("MissingPermission")
    private void onLocationPermsAccepted() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        startLocation = getCurrentLocation() == null ? startLocation : getCurrentLocation();
    }

    // When the user rejects our request to access locations
    private void onLocationPermsDenied() {
        Toast.makeText(this, "Some features will be disabled without location permissions.", Toast.LENGTH_SHORT).show();
    }

    @SuppressLint("MissingPermission")
    private Geopoint getCurrentLocation() {
        Geopoint geopoint = null;

        if(!Data.locationPermsGranted) {
            return null;
        }

        Location l = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if(l != null) {
            geopoint = new Geopoint(l.getLatitude(), l.getLongitude());
        }

        return geopoint;
    }

    // Attempt to fetch location perms
    private void getLocationPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        boolean a = (ContextCompat.checkSelfPermission(getApplicationContext(), Data.FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        boolean b = (ContextCompat.checkSelfPermission(getApplicationContext(), Data.COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        Data.locationPermsGranted = (a && b);

        if (!Data.locationPermsGranted) {
            ActivityCompat.requestPermissions(this, permissions, Data.LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            onLocationPermsAccepted();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Data.locationPermsGranted = false;

        if (requestCode == Data.LOCATION_PERMISSION_REQUEST_CODE) {
            for (int grantResult : grantResults) {
                if (grantResult == PackageManager.PERMISSION_DENIED) {
                    onLocationPermsDenied();
                    return;
                }

                Data.locationPermsGranted = true;
                onLocationPermsAccepted();
                Toast.makeText(this, "Location permissions granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}