package com.bikechow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapView;
import com.microsoft.maps.search.MapLocationAddress;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    public MapView mMapView;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Menu navigationMenu;
    private Toolbar toolbar;

    private LocationManager locationManager;
    private MapLocationAddress mapLocationAddress;
    private Geopoint startLocation = Data.RICHMOND;

    public Requests requestCreator;
    private Draw draw;

    private IconData userData = new IconData();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);  //
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);

        // initialize our visuals
        initView();

        // Create our requests class, which will handle GET and POST requests
        requestCreator = new Requests(this);

        // Create our draw class, which will enable us to draw onto the map
        draw = new Draw(this);

        getLocationPermissions(); // Attempt to get our location permissions

        initUser(); // Initiate user point
    }

    private void initView() {

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationMenu = navigationView.getMenu();
        toolbar = findViewById(R.id.toolbar);

        // add listener to our navigation drawer to detect item selected
        navigationView.setNavigationItemSelectedListener(navigationListener);

        // add listener to toolbar icon to open navigation drawer
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // add listener to our floating button to return view to user
        findViewById(R.id.userPosReturn).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                updateUser();
                setScene(getCurrentLocation(), Data.DEFAULT_CLOSE_RADIUS, MapAnimationKind.BOW);
            }
        });

        // maps in navigation drawer enabled on default
        // does not work
        //navigationMenu.getItem(R.id.nav_map).setChecked(true);

        // disable the ugly buttons
        mMapView.getUserInterfaceOptions().setZoomButtonsVisible(false);
        mMapView.getUserInterfaceOptions().setCompassButtonVisible(false);
        mMapView.getUserInterfaceOptions().setTiltButtonVisible(false);


    }

    // do whatever when an item on navigation drawer is pressed
    // when returned true, selected item would be highlighted and not highlighted if returned false
    // we will use this navigation drawer to handle out different "pages"
    NavigationView.OnNavigationItemSelectedListener navigationListener = new NavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(MenuItem item) {

            switch (item.getItemId()) {
                case R.id.nav_map:

                    // close navigation drawer
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;

                case R.id.nav_directions:

                    return true;

                case R.id.nav_blank:
                    alertUser("work in progress");

            }

            return false;
        }
    };

    // this should make things neater or something
    void alertUser(String text){
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private void initUser(){
        // getAssets().open() should open the assets folder and u can access a file through its name, but i wouldnt know because i cant add the map icon
        try{
            userData = new IconData(startLocation, "You", new MapImage(getAssets().open("mrchow.png")));
            // found it kinda annoying each time i booted the app it takes a second for me to find my own location, so i made this no transition
            setScene(startLocation, Data.DEFAULT_FAR_RADIUS, MapAnimationKind.NONE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(startLocation != null){
            draw.addIcon(userData);
        }
    }

    // more to add here (hopefully)
    private void updateUser() {
        userData.location = getCurrentLocation();
    }

    // set view (moved to its own function for accessibility)
    private void setScene(Geopoint location, int radius, MapAnimationKind transition){
        mMapView.setScene(MapScene.createFromLocationAndRadius(location, radius), transition);
        // Moves the camera to the specified position with the specified animation
    }

    // When we have permissions to access locations
    @SuppressLint("MissingPermission")
    private void onLocationPermsAccepted() {
        System.out.println("Entered location perms accepted condition");

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        startLocation = getCurrentLocation() == null ? startLocation : getCurrentLocation();
    }

    // When the user rejects our request to access locations
    private void onLocationPermsDenied() {
        alertUser("Some features will be disabled without location permissions.");
    }

    @SuppressLint("MissingPermission")
    private Geopoint getCurrentLocation() {
        Geopoint geopoint = null;

        if(!Data.locationPermsGranted) {
            return null;
        }

        Location l = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

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
               alertUser("Location permissions granted!");
            }
        }
    }

}
