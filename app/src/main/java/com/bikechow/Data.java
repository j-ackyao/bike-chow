package com.bikechow;

import android.Manifest;

import com.microsoft.maps.Geopoint;

public class Data {
    public static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 1234; // Permission code for location (doesn't really matter)
    public static final int DEFAULT_RADIUS_IN_METERS = 1500; // Zoom amount to default to

    public static boolean locationPermsGranted = false; // Bool to check if we have perms
    public static final Geopoint RICHMOND = new Geopoint(49.166592, -123.133568);
}
