package com.bikechow;

import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Requests{
    MainActivity main;
    RequestQueue requestQueue;

    public Requests(MainActivity main) {
        this.main = main;
        requestQueue = Volley.newRequestQueue(main);
    }

    public RequestCallback GetRequest(String url, RequestCallback rcb) {
        StringRequest req = new StringRequest(Request.Method.GET, url, rcb::onCallback, error -> Toast.makeText(main, "An error occurred attempting to make a GET request", Toast.LENGTH_SHORT).show()); // Create string request, and push the callback onto RCB object
        requestQueue.add(req); // Add the request to the request queue
        return rcb;
    }
}

interface RequestCallback {
    void onCallback(String data);
}

