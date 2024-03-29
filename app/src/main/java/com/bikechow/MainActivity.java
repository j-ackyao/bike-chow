package com.bikechow;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapAnimationKind;
import com.microsoft.maps.MapDoubleTappedEventArgs;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapScene;
import com.microsoft.maps.MapTappedEventArgs;
import com.microsoft.maps.MapView;
import com.microsoft.maps.OnMapDoubleTappedListener;
import com.microsoft.maps.OnMapTappedListener;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback, OnMapTappedListener, OnMapDoubleTappedListener {
    public MapView mMapView;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Menu navigationMenu;
    private Toolbar toolbar;

    private LocationManager locationManager;
    private Geopoint startLocation = Data.RICHMOND;

    public Requests requestCreator;
    private Draw draw;

    public IconData userData = new IconData();

    // Text box related stuff
    private EditText textBar;
    public boolean textModified;

    String covidCase = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey(BuildConfig.CREDENTIALS_KEY);  //
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.addOnMapTappedListener(this);
        mMapView.addOnMapDoubleTappedListener(this);
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

        // Grab reference to components
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationMenu = navigationView.getMenu();
        toolbar = findViewById(R.id.toolbar);
        FloatingActionButton searchButton = findViewById(R.id.searchButton);

        // add listener to our navigation drawer to detect item selected
        navigationView.setNavigationItemSelectedListener(navigationListener);

        // add listener to toolbar icon to open navigation drawer
        toolbar.setNavigationOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        textBar = findViewById(R.id.search_bar);
        textBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textModified = true;
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Handle the user pressing the search button
        searchButton.setOnClickListener(v -> {
            draw.clearRoutes();

            if(textBar.getText().toString().isEmpty()) {
                alertUser("No route specified! Please tap on a location on the map, or manually enter one with the search bar.");
                return;
            }

            if (!textModified) {
                Geopoint target = draw.tapIcon.getLocation();
                draw(userData.location, target);
            } else {
                requestCreator.getGeopoint(textBar.getText().toString() + " BC, Canada", target -> {
                    draw(userData.location, target);
                });
            }
        });

        // Basic webscraping thread to get COVID-19 data.
        new Thread(new WebscrapingThread(MainActivity.this)).start();

        // add listener to our floating button to return view to user
        findViewById(R.id.userPosReturn).setOnClickListener(v -> {
            updateUser();
            setScene(getCurrentLocation(), Data.DEFAULT_CLOSE_RADIUS, MapAnimationKind.BOW);
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

    void draw(Geopoint startPoint, Geopoint endPoint) {
        try {
            requestCreator.getRoutesPoints(startPoint, endPoint, 3, routes -> {
                requestCreator.sortRoutesByElevationCost(routes, sortedRoutes -> {
                    for (int i = 0; i < sortedRoutes.size(); i++) {
                        draw.drawRouteInterpolated(sortedRoutes.get(i), Data.COLOR_SEQUENCE[i]);
                    }
                    draw.replaceTapPoint(endPoint, String.format("Query: %s", textBar.getText().toString()));
                });
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!Data.searchedOnce) {
            Toast.makeText(this, String.format("Be careful! Active COVID-19 cases in BC: %s", covidCase), Toast.LENGTH_LONG).show();
            Data.searchedOnce = true;
        }
    }

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
            draw.replaceUserPoint(userData.location);
        }
    }

    private void moveUser(Geopoint newPoint) {
        userData.location = newPoint;
        draw.replaceUserPoint(newPoint);
    }

    // more to add here (hopefully)
    private void updateUser() {
        if(!Data.locationPermsGranted) {
            alertUser("You have not granted the app location permissions.");
            return;
        }
        userData.location = getCurrentLocation();
        draw.replaceUserPoint(userData.location);
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

        if(!Data.locationPermsGranted) { // Safety check to ensure that we have permission to fetch the location of the user
            alertUser("You have not granted this app location permissions.");
            return null;
        }

        Location l = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) != null ?
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) : // We'll first try using the Network Provider to get location
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // And if that doesn't work try GPS provider

        if(l != null) {
            geopoint = new Geopoint(l.getLatitude(), l.getLongitude());
        } else {
            alertUser("We couldn't get your location! Are you offline?");
        }

        return geopoint;
    }

   /*
   This is our request method for asking the user to grant this app location permissions.
    */
    private void getLocationPermissions() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}; // Required permissions

        boolean a = (ContextCompat.checkSelfPermission(getApplicationContext(), Data.FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
        boolean b = (ContextCompat.checkSelfPermission(getApplicationContext(), Data.COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED);

        Data.locationPermsGranted = (a && b); // We need both permissions to work!! (Actually double check this)

        if (!Data.locationPermsGranted) {
            ActivityCompat.requestPermissions(this, permissions, Data.LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            onLocationPermsAccepted();
        }
    }

    /*
    We'll call this function when the user decides whether to grant the user permissions.
    We use this function to decide whether to call onLocationPermsAccepted or onLocationPermsDenied.
     */
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

    private final ArrayList<Geopoint> cachedTapPoint = new ArrayList<>(); // A tap point to store to fall back to if onMapDoubleTapped is picked up.
    private final ArrayList<String> cachedTextTapPoint = new ArrayList<>(); // String to fall back to (so we don't use excess bandwidth)


    /* When the user taps on the screen, we want a point to be generated with the address of the location.
       Further implementation will include that it will append the location automatically to the text box for
       destination.
       We return false as we may still want other listeners to receive the event. */
    @Override
    public boolean onMapTapped(MapTappedEventArgs mapTappedEventArgs) {
        Geopoint tapPoint = mapTappedEventArgs.location;
        requestCreator.getAddress(tapPoint, request -> {
            draw.replaceTapPoint(tapPoint, request);
            textBar.setText(request);
            textModified = false;

            cachedTapPoint.add(tapPoint);
            cachedTextTapPoint.add(request);
        });
        return false;
    }

    /*
    When the user double taps on the screen, we want to relocate the user's position.
    We return false as we may still want other listeners to receive the event.
    Unfortunately the double tap function is still linked to onMapTapped (probably implemented the same way),
    so when the user wants to move the user it also changes the position of their destination (the tap point).
     */
    @Override
    public boolean onMapDoubleTapped(MapDoubleTappedEventArgs mapDoubleTappedEventArgs) {
        Geopoint tapPoint = mapDoubleTappedEventArgs.location;
        moveUser(tapPoint);

        if(cachedTapPoint.size() < 2 || cachedTextTapPoint.size() < 2) return true;

        String restoreString = cachedTextTapPoint.get(cachedTextTapPoint.size()-2);
        Geopoint restorePoint = cachedTapPoint.get(cachedTapPoint.size()-2);

        // As we picked up the double tap event, we need to restore the position of the original tap point.
        draw.replaceTapPoint(cachedTapPoint.get(cachedTapPoint.size()-2), cachedTextTapPoint.get(cachedTextTapPoint.size()-2));
        textBar.setText(textModified ? textBar.getText().toString() : cachedTextTapPoint.get(cachedTextTapPoint.size()-2));

        // We just want to keep the point and text we fell back to.
        cachedTapPoint.clear(); cachedTapPoint.add(restorePoint);
        cachedTextTapPoint.clear(); cachedTextTapPoint.add(restoreString);

        return true;
    }
}
