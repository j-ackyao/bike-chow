package com.bikechow;

import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.search.MapLocation;
import com.microsoft.maps.search.MapLocationFinder;
import com.microsoft.maps.search.MapLocationFinderStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class Requests{
    MainActivity main;
    RequestQueue requestQueue;

    public Requests(MainActivity main) {
        this.main = main;
        requestQueue = Volley.newRequestQueue(main);
    }

    public void getRequest(String url, RequestCallback rcb) {
        sendRequest(url, rcb, Request.Method.GET);
    }

    public void postRequest(String url, RequestCallback rcb) {
        sendRequest(url, rcb, Request.Method.POST);
    }

    public void sendRequest(String url, RequestCallback rcb, int requestMethod) {
        StringRequest req = new StringRequest(requestMethod, url, rcb::onCallback, error -> {
            Toast.makeText(main, "An internal error occurred attempting to make a request.", Toast.LENGTH_LONG).show();
            System.out.println("Request Error::::: "+error.toString());
        });
        req.setTag(Data.REQUEST_TAG);
        requestQueue.add(req);
    }

    public void clearRequests() {
        requestQueue.cancelAll(Data.REQUEST_TAG);
    }

    // gets the address from geopoint
    public void getAddress(Geopoint geopoint, RequestCallback rcb) {
        MapLocationFinder.findLocationsAt(geopoint, null, result -> {
            if(result.getStatus() == MapLocationFinderStatus.SUCCESS){
                List<MapLocation> mapLocationList = result.getLocations();
                rcb.onCallback(mapLocationList.get(0).getAddress().getAddressLine());
            }
            else{
                main.alertUser("Failed to find an address with a given set of coordinates");
            }
        });
    }

    // get geopoint from address string
    public void getGeopoint(String address, GeopointCallback gcb) {
        MapLocationFinder.findLocations(address, null, result -> {
            if(result.getStatus() == MapLocationFinderStatus.SUCCESS){
                gcb.onCallback(result.getLocations().get(0).getGeocodePoints().get(0).getPoint());
            }
            else{
                main.alertUser("Failed to find a set of coordinates with a given address");
            }
        });
    }

    public void getRoutesData(String[] waypoints, int amountOfRoutes, RequestCallback rcb) {
        StringBuilder concat = new StringBuilder(Data.ROUTES_API); // Initialize the string with the routes beginning
        for(int i = 0; i < waypoints.length; i++) { // Add on each waypoint
            if(i >= 25) {return;} // We don't want to exceed 25 waypoints
            concat.append(String.format("wp.%s=%s&", i + 1, waypoints[i])); // Add each waypoint as: wp.1=Vancouver&
        }
        concat.append(String.format("maxSolutions=%s&travelMode=%s&key=%s", amountOfRoutes, "Walking",BuildConfig.CREDENTIALS_KEY)); // Append on the amount of routes and our key
        getRequest(concat.toString(), rcb);
    }

    public JSONObject getRouteJSON(String routeData) throws JSONException {
        return new JSONObject(routeData);
    }

    public JSONArray getRouteLegs(JSONObject routeJSON) throws JSONException {
        JSONObject resourceSets = routeJSON.getJSONArray("resourceSets").getJSONObject(0);
        JSONObject resources = resourceSets.getJSONArray("resources").getJSONObject(0);

        return resources.getJSONArray("routeLegs");
    }

    public Geopoint[] getRoutePoints(JSONArray routeLegs, int routeIndex) throws JSONException {
        JSONObject routeObject = routeLegs.getJSONObject(routeIndex);
        JSONArray itineraryItems = routeObject.getJSONArray("itineraryItems");
        ArrayList<Geopoint> retData = new ArrayList<Geopoint>();

        for(int i = 0; i < itineraryItems.length(); i++) {
            JSONArray coordinates = itineraryItems.getJSONObject(i).getJSONObject("maneuverPoint").getJSONArray("coordinates");
            double lat = coordinates.getDouble(0);
            double longitude = coordinates.getDouble(1);

            retData.add(new Geopoint(lat, longitude));
        }

        return retData.toArray(new Geopoint[0]);
    }

    public void getSnappedData(Geopoint[] unsnappedPoints, RequestCallback rcb) {
        StringBuilder url = new StringBuilder(Data.SNAP_API + "points=");
        for(Geopoint p : unsnappedPoints) {
            url.append(String.format("%s;", Data.geopointToString(p)));
        }
        url.deleteCharAt(url.length()-1);
        url.append(String.format("&interpolate=true&key=%s", BuildConfig.CREDENTIALS_KEY));
        getRequest(url.toString(), rcb);
    }

    public Geopoint[] getSnappedPoints(String data) throws JSONException {
        JSONArray snappedPoints = new JSONObject(data).getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources").getJSONObject(0).getJSONArray("snappedPoints");
        ArrayList<Geopoint> retData = new ArrayList<Geopoint>();
        for(int i = 0; i < snappedPoints.length(); i++) {
            JSONObject coordinates = snappedPoints.getJSONObject(i).getJSONObject("coordinate");
            double latitude = coordinates.getDouble("latitude");
            double longitude = coordinates.getDouble("longitude");

            retData.add(new Geopoint(latitude, longitude));
        }
        return retData.toArray(new Geopoint[0]);
    }

}

interface RequestCallback {
    void onCallback(String data);
}

interface GeopointCallback {
    void onCallback(Geopoint point);
}

