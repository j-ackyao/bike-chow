package com.bikechow;

import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

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

    public void getRoutesData(String[] waypoints, int amountOfRoutes, RequestCallback rcb) {
        StringBuilder concat = new StringBuilder(Data.ROUTES_API); // Initialize the string with the routes beginning
        for(int i = 0; i < waypoints.length; i++) { // Add on each waypoint
            if(i >= 25) {return;} // We don't want to exceed 25 waypoints
            concat.append(String.format("wp.%s=%s&", i + 1, waypoints[i])); // Add each waypoint as: wp.1=Vancouver&
        }
        concat.append(String.format("maxSolutions=%s&key=%s", amountOfRoutes, BuildConfig.CREDENTIALS_KEY)); // Append on the amount of routes and our key
        getRequest(concat.toString(), rcb);
    }
}

interface RequestCallback {
    void onCallback(String data);
}

