package com.bikechow;

import android.Manifest;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.microsoft.maps.Geopoint;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;

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
    public static final Geopoint VANCOUVER = new Geopoint(49.246292, -123.116226);

    // API URLs
    public static final String ROUTES_API = "https://dev.virtualearth.net/REST/v1/Routes?";
    public static final String SNAP_API = "https://dev.virtualearth.net/REST/v1/Routes/SnapToRoad?";
    public static final String ELEVATION_API = "https://dev.virtualearth.net/REST/v1/Elevation/List?";

    // Colour sequence
    public static final int[] COLOR_SEQUENCE = new int[] {Color.rgb(0, 255, 0),Color.rgb(0, 155, 0), Color.rgb(0, 100, 0)};


    public static String geopointToString(Geopoint g) {
        return String.format("%s,%s", g.getPosition().getLatitude(), g.getPosition().getLongitude());
    }

    public static double distanceBetweenCoordinates(double lat1, double long1, double lat2, double long2){
        double earthRadius = 6371;
        lat1 = lat1 * Math.PI / 180;
        lat2 = lat2 * Math.PI / 180;
        long1 = long1 * Math.PI / 180;
        long2 = long2 * Math.PI / 180;

        double a = Math.pow(Math.sin((lat2-lat1)/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin((long2-long1)/2), 2);
        double b = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return earthRadius * b;
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
    public MapIcon iconIndex = null;

    public int elevationCost = -1;

    public double travelDistance;

    public double getTravelDistance() {
        return travelDistance;
    }

    public void setTravelDistance(double travelDistance) {
        this.travelDistance = travelDistance;
    }

    public int getElevationCost() {
        return elevationCost;
    }

    public void setElevationCost(int elevationCost) {
        this.elevationCost = elevationCost;
    }

    public Route(Geopoint[] points) {
        this.points = points;
        startingPoint = points[0];
        endingPoint = points[points.length-1];
    }

    public Route(Route r) { // Copy constructor
        this.points = r.points;
        this.startingPoint = r.startingPoint;
        this.endingPoint = r.endingPoint;
        this.iconIndex = r.iconIndex;
        this.elevationCost = r.elevationCost;
        this.travelDistance = r.travelDistance;
    }
}

class InterpolatedRoute extends Route {
    Geopoint midpoint;

    public InterpolatedRoute(Route r, Geopoint[] newRoute) {
        super(r);
        midpoint = r.points[r.points.length/2];
        this.points = newRoute;
    }

}