package com.bikechow;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.Geoposition;
import com.microsoft.maps.search.MapLocation;
import com.microsoft.maps.search.MapLocationFinder;
import com.microsoft.maps.search.MapLocationFinderStatus;
import com.microsoft.maps.search.MapLocationOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
            main.alertUser("An internal error occurred attempting make a request.");
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
        MapLocationOptions options = new MapLocationOptions();
        options.setIncludeCountryCode(true);
        MapLocationFinder.findLocationsAt(geopoint, options, result -> {
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
        address += ", BC"; // This is a fallback measure. It may be removed in the future.
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
                    double travelDistance = b_getTravelDistance(resources);

                    Route route = new Route(points);
                    route.setTravelDistance(travelDistance);
                    retData.add(route);

                } catch (JSONException e) {
                    // We likely failed to generate the amount of routes, or another problem occured.
                }
            }

            if(retData.size() < amountOfRoutes) {
                main.alertUser("Failed to generate amount of routes requested. Found: " + retData.size() + ", wanted: " + amountOfRoutes);
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
        double minDiff = 0.00001;
        double maxDiff = 0.1;
        int maxSkip = 10;

        if(rawData.size() < curIndex) {
            retData = (ArrayList<Geopoint>)rawData.clone();
            return retData.toArray(new Geopoint[0]);
        }

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

            double latDiff = Math.abs(next.getPosition().getLatitude() - cur.getPosition().getLatitude()) ;
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
//        concat.append(String.format("travelMode=%s&avoid=%s&maxSolutions=%s&key=%s", "Walking", "highways",amountOfRoutes ,BuildConfig.CREDENTIALS_KEY)); // Append on the amount of routes and our key
        concat.append(String.format("maxSolutions=%s&travelMode=%s&key=%s", amountOfRoutes, "Walking", BuildConfig.CREDENTIALS_KEY)); // Append on the amount of routes and our key

        getRequest(concat.toString(), rcb);
    }

    private JSONObject b_getRouteJSON(String routeData) throws JSONException {
        return new JSONObject(routeData);
    }

    private JSONObject b_getRouteResources(JSONObject routeJSON, int routeIndex) throws JSONException {
        JSONObject resourceSets = routeJSON.getJSONArray("resourceSets").getJSONObject(0);

        return resourceSets.getJSONArray("resources").getJSONObject(routeIndex);
    }

    private double b_getTravelDistance(JSONObject resources) throws JSONException {
        return resources.getDouble("travelDistance");
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

    private void b_getElevationData(Geopoint startPoint, Geopoint endPoint, RequestCallback rcb) {
        getRequest(Data.ELEVATION_API + "points=" + Data.geopointToString(startPoint) + "," + Data.geopointToString(endPoint) + "&key=" + BuildConfig.CREDENTIALS_KEY, rcb);
    }

    private int[] b_getElevations(String elevationData) throws JSONException {
        JSONObject elevationJSON = new JSONObject(elevationData);

        JSONArray arr = elevationJSON.getJSONArray("resourceSets").getJSONObject(0).getJSONArray("resources").getJSONObject(0).getJSONArray("elevations");
        return new int[] {(int)arr.get(0), (int)arr.get(1)};
    }

    private boolean b_isIntArrFilled(int[][] arr) {
        for (int[] ints : arr) {
            if (Arrays.equals(ints, new int[]{0, 0})) {
                return false;
            }
        }
        return true;
    }

    public void getElevations(Route route, ElevationsCallback ecb) {
        int length = route.points.length;
        ArrayList<int[]> retData = new ArrayList<int[]>(length-1);
        int[][] rawData = new int[length-1][2];

        for(int i = 0; i < length-1; i++) {
            int a = i;
            b_getElevationData(route.points[i], route.points[i+1], unparsed -> {
                try {
                    int[] data = b_getElevations(unparsed);
                    rawData[a] = data;

                    if(b_isIntArrFilled(rawData))  {
                        retData.addAll(Arrays.asList(rawData));
                        ecb.onCallback(retData);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private int b_indexOf(int[] arr, int x) {
        for(int i = 0; i < arr.length; i++) {
            if (x == arr[i]) {
                return i;
            }
        }

        throw new java.lang.Error("COULD NOT FIND NUMBER IN ARRAY");
    }

    public void setRouteElevation(Route r, Callback finished) { // Assign the route elevation of a route
        getElevations(r, elevations -> {
            double squared = 0;
            double[] distances = new double[elevations.size()];

            for(int i = 0; i < elevations.size(); i++){
                Geoposition point1 = r.points[i].getPosition();
                Geoposition point2 = r.points[i+1].getPosition();

                double latitude1 = point1.getLatitude();
                double longitude1 = point1.getLongitude();

                double latitude2 = point2.getLatitude();
                double longitude2 = point2.getLongitude();

                distances[i] = Data.distanceBetweenCoordinates(latitude1, longitude1, latitude2, longitude2);
            }

            for(int i = 0; i < elevations.size(); i++) {
                int[] pairedPoints = elevations.get(i);
                double change = Math.abs((pairedPoints[1] - pairedPoints[0])/distances[i]);

                squared += Math.pow(change, 2);
            }
            r.setElevationCost((int) squared);
            finished.onCallback();
        });
    }

    public void sortRoutesByElevationCost(ArrayList<Route> routes, RoutesCallback rcb) { // Sort an arraylist of routes
        AtomicInteger sorted = new AtomicInteger();

        for(Route route : routes) {
            setRouteElevation(route, () -> {
                sorted.getAndIncrement();

                if(sorted.get() == routes.size()) {
                    routes.sort((o1, o2) -> o1.getElevationCost() - o2.getElevationCost());

                    rcb.onCallback(routes);
                }

            });
        }
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

interface ElevationsCallback {
    void onCallback(ArrayList<int[]> elevations);
}

interface Callback {
    void onCallback();
}
