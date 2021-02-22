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


    public void getRoutesPoints(Geopoint startPoint, Geopoint endPoint, int amountOfRoutes, RoutesCallback rcb) throws JSONException{
        b_getRoutesData(new String[] {Data.geopointToString(startPoint), Data.geopointToString(endPoint)}, amountOfRoutes, routeData -> {
            ArrayList<Route> retData = new ArrayList<>();
            for(int i = 0; i < amountOfRoutes; i++) {
                try {
                    JSONObject resources = b_getRouteResources(b_getRouteJSON(routeData), i);
                    Geopoint[] points = b_getRoutePoints(resources);
                    Route route = new Route(points);
                    retData.add(route);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            rcb.onCallback(retData);
        });
    }


    public void getRoutesAddresses(String startAddress, String endAddress, int amountOfRoutes, RoutesCallback rcb) throws JSONException {
        getGeopoint(startAddress, startPoint -> { // startPoint is startAddress geopoint
            getGeopoint(endAddress, endPoint -> { // endPoint is endAddress geopoint
                try {
                    getRoutesPoints(startPoint, endPoint, amountOfRoutes, rcb);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        });
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
        ArrayList<Geopoint> rawData = new ArrayList<Geopoint>();
        for(int i = 0; i < snappedPoints.length(); i++) {
            JSONObject coordinates = snappedPoints.getJSONObject(i).getJSONObject("coordinate");
            double latitude = coordinates.getDouble("latitude");
            double longitude = coordinates.getDouble("longitude");

            rawData.add(new Geopoint(latitude, longitude));
        }

        ArrayList<Geopoint> retData = new ArrayList<>();

        int skipped = 0;
        int curIndex = 5;
        int nextIndex = 6;
        double minDiff = 0.0001;
        double maxDiff = 0.1;
        int maxSkip = 10;

        for(int i = 0; i < curIndex; i++) {
            retData.add(rawData.get(i));
        }

        for(int i = curIndex; i < rawData.size(); i++) {
            if(i == rawData.size()-1) {
                retData.add(rawData.get(i));
                break;
            };

            Geopoint cur = rawData.get(curIndex);
            Geopoint next = rawData.get(nextIndex);

            double latDiff = Math.abs(next.getPosition().getLatitude() - cur.getPosition().getLatitude());
            double longDiff = Math.abs(next.getPosition().getLongitude() - cur.getPosition().getLongitude());

            if(((latDiff < minDiff && longDiff < maxDiff) || (longDiff < minDiff && latDiff < maxDiff)) && nextIndex-curIndex < maxSkip) {
                nextIndex++;
                skipped++;
            }
            else {
                retData.add(rawData.get(curIndex));
                curIndex = nextIndex;
                nextIndex++;
            }
        }

        System.out.println("We skipped appending " + skipped + " points.");
        return retData.toArray(new Geopoint[0]);
    }




    private void b_getRoutesData(String[] waypoints, int amountOfRoutes, RequestCallback rcb) {
        StringBuilder concat = new StringBuilder(Data.ROUTES_API); // Initialize the string with the routes beginning
        for(int i = 0; i < waypoints.length; i++) { // Add on each waypoint
            if(i >= 25) {return;} // We don't want to exceed 25 waypoints
            concat.append(String.format("wp.%s=%s&", i + 1, waypoints[i])); // Add each waypoint as: wp.1=Vancouver&
        }
        concat.append(String.format("maxSolutions=%s&travelMode=%s&key=%s", amountOfRoutes, "Walking",BuildConfig.CREDENTIALS_KEY)); // Append on the amount of routes and our key
        getRequest(concat.toString(), rcb);
    }

    private JSONObject b_getRouteJSON(String routeData) throws JSONException {
        return new JSONObject(routeData);
    }

    private JSONObject b_getRouteResources(JSONObject routeJSON, int routeIndex) throws JSONException {
        JSONObject resourceSets = routeJSON.getJSONArray("resourceSets").getJSONObject(0);
        JSONObject resources = resourceSets.getJSONArray("resources").getJSONObject(routeIndex);

        return resources;
    }

    private Geopoint[] b_getRoutePoints(JSONObject resources) throws JSONException {
        JSONArray routeLegs = resources.getJSONArray("routeLegs");
        JSONObject routeObject = routeLegs.getJSONObject(0);
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

}

interface RequestCallback {
    void onCallback(String data);
}

interface GeopointCallback {
    void onCallback(Geopoint point);
}

interface RoutesCallback {
    void onCallback(ArrayList<Route> routes);
}
