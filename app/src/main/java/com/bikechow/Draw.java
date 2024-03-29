package com.bikechow;

import com.microsoft.maps.Geopath;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.Geoposition;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapImage;
import com.microsoft.maps.MapPolyline;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class Draw {
    public MainActivity main; // Reference to MainActivity

    private final MapElementLayer iconLayer; // Layer for drawing icons on (on top of the map and route layer)
    private final MapElementLayer routeLayer; // Layer for drawing routes on (below the icon layer)
    private final Requests requestCreator; // Easy access to MainActivity requestCreator

    public MapIcon tapIcon = null; // Icon that represents the user's tap position
    private MapIcon userIcon = null; // Icon that represent's the user's specified/current location

    private final ArrayList<Route> storedRoutes = new ArrayList<Route>(); // A list of routes for later clearing

    public Draw(MainActivity main) {
        this.main = main;
        requestCreator = main.requestCreator;
        iconLayer = new MapElementLayer(); // Create a layer for drawing icons on
        iconLayer.setZIndex(1); // We want the icons to be in front
        routeLayer = new MapElementLayer();
        main.mMapView.getLayers().add(routeLayer);
        main.mMapView.getLayers().add(iconLayer); // Add these layer to our map view
    }

    // Function for drawing routes, given a route object
    public void drawRoute(Route route, int color) {
        drawRoute(route.points, color);
        storedRoutes.add(route); // We want to store the routes so we can later clear all of them when a new one is requested
    }

    // Function for drawing a route cleanly, give a route object
    public void drawRouteInterpolated(Route route, int color) {
        requestCreator.getSnappedData(route.points, json -> {
            try {
                Geopoint[] points = requestCreator.getSnappedPoints(json);
                InterpolatedRoute r = new InterpolatedRoute(route, points);
                drawRoute(r, color);

                // Add a point in the middle of the route
                String iconText = "";

                if(route.getElevationCost() != -1) {
                    iconText = String.format("Elevation Cost: %s, \nTravel Distance: %s km", route.getElevationCost(), route.getTravelDistance()).toString();
                } // Honestly can't think of an amazing way to handle this.

                r.iconIndex = addIcon(new IconData(r.midpoint, iconText));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    // Function for drawing routes, given points
    public void drawRoute(Geopoint[] points, int color) {
        for(int i = 0; i < points.length; i++) {
            if(i == points.length-1) break; // We don't want to draw a path for the final point

            ArrayList<Geoposition> geopoints = new ArrayList<Geoposition>();
            geopoints.add(points[i].getPosition());
            geopoints.add(points[i+1].getPosition());

            MapPolyline mapPolyline = new MapPolyline();
            mapPolyline.setPath(new Geopath(geopoints));
            mapPolyline.setStrokeColor(color);
            mapPolyline.setStrokeWidth(5);
            mapPolyline.setStrokeDashed(false);

            routeLayer.getElements().add(mapPolyline);
        }
    }

    // Function for adding icons to map view with Icon Data (the preferred way)
    public MapIcon addIcon(IconData iconData) {
        MapIcon mapIcon = new MapIcon();
        mapIcon.setLocation(iconData.location);
        mapIcon.setImage(iconData.image);
        mapIcon.setTitle(iconData.title);

        this.iconLayer.getElements().add(mapIcon);
        return mapIcon;
    }

    public void replaceTapPoint(Geopoint newPoint, String address) {
        if(tapIcon != null) {
            iconLayer.getElements().remove(tapIcon);
        }

        tapIcon = addIcon(new IconData(newPoint, address));
    }

    public void replaceUserPoint(Geopoint newPoint) {
        try {
            iconLayer.getElements().remove(userIcon);
            main.userData = new IconData(newPoint, "You", new MapImage(main.getAssets().open("mrchow.png")));
            userIcon = addIcon(main.userData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clearRoutes() {
        routeLayer.getElements().clear();

        for(int i = 0; i < storedRoutes.size(); i++) {
            iconLayer.getElements().remove(storedRoutes.get(i).iconIndex);
        }

        storedRoutes.clear();
    }

}
