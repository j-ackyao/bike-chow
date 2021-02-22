package com.bikechow;

import android.Manifest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapImage;

import java.util.ArrayList;

public class Data {
    // Permission related statics
    public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1234; // Permission code for location (doesn't really matter)
    public static boolean locationPermsGranted = false; // Bool to check if we have perms

    // Navigation
    public static final int DEFAULT_FAR_RADIUS = 1500; // Zoom amount to default to // changed this to far radius because its kinda far
    public static final int DEFAULT_CLOSE_RADIUS = 300; // this is a better view for user to view their surroundings

    // Request handling
    public static final String REQUEST_TAG = "REQ"; // All requests are tagged with this

    // Location constants
    public static final Geopoint RICHMOND = new Geopoint(49.166592, -123.133568);

    // API URLs
    public static final String ROUTES_API = "https://dev.virtualearth.net/REST/v1/Routes?";
    public static final String SNAP_API = "https://dev.virtualearth.net/REST/v1/Routes/SnapToRoad?";


    public static String geopointToString(Geopoint g) {
        return String.format("%s,%s", g.getPosition().getLatitude(), g.getPosition().getLongitude());
    }

}

// A class for storing icon data. This is for default parameters.
class IconData {
    public MapImage image = null;
    public String title = "";
    public Geopoint location;

    public IconData(){ }

    public IconData(Geopoint location) {
        this.location = location;
    }

    public IconData(Geopoint location, String title) {
        this.location = location;
        this.title = title;
    }

    public IconData(Geopoint location, MapImage image) {
        this.location = location;
        this.image = image;
    }

    public IconData(Geopoint location, String title, MapImage image) {
        this.location = location;
        this.title = title;
        this.image = image;
    }
}

// Stores data about a route
class Route {
    public Geopoint[] points;
    public Geopoint startingPoint;
    public Geopoint endingPoint;

    public Route(Geopoint[] points) {
        this.points = points;
        startingPoint = points[0];
        startingPoint = points[points.length-1];
    }
}

