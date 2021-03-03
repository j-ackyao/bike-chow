package com.bikechow;

import com.microsoft.maps.Geopath;
import com.microsoft.maps.Geopoint;
import com.microsoft.maps.Geoposition;
import com.microsoft.maps.MapElement;
import com.microsoft.maps.MapElementLayer;
import com.microsoft.maps.MapIcon;
import com.microsoft.maps.MapPolyline;

import org.json.JSONException;

import java.util.ArrayList;

public class Draw {
    public MainActivity main;

    private final MapElementLayer iconLayer;
    private final MapElementLayer routeLayer;
    private final Requests requestCreator;
    private MapIcon tapIcon = null;
    private MapIcon userIcon = null;

    private final ArrayList<Route> storedRoutes = new ArrayList<Route>();

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

        // Add a point in the middle of the route
        Geopoint middlePoint = route.points[route.points.length/2];
        String iconText = "";

//        System.out.println(String.format("Route Elevation Cost: %s", route.getElevationCost()));

        if(route.getElevationCost() != -1) {
            iconText = String.format("Elevation Cost: %s", route.getElevationCost()).toString();
        }

//        System.out.println("Entered Draw Route with a given route. Elevation cost is: " + route.elevationCost);

        route.iconIndex = addIcon(new IconData(middlePoint, iconText));

        storedRoutes.add(route);

    }

    // Function for drawing a route cleanly, give a route object
    public void drawRouteInterpolated(Route route, int color) {
        requestCreator.getSnappedData(route.points, json -> {
            try {
                Geopoint[] points = requestCreator.getSnappedPoints(json);
                Route r = new Route(points);
                r.setElevationCost(route.getElevationCost());
                drawRoute(r, color);
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
        iconLayer.getElements().remove(userIcon);
        main.userData = new IconData(newPoint, "You");
        userIcon = addIcon(main.userData);
    }

    public void clearRoutes() {
        routeLayer.getElements().clear();

        for(int i = 0; i < storedRoutes.size(); i++) {
            iconLayer.getElements().remove(storedRoutes.get(i).iconIndex);
        }

        storedRoutes.clear();
    }

}
