package com.bikechow;

import androidx.appcompat.app.AppCompatActivity;
import com.microsoft.maps.MapRenderMode;
import com.microsoft.maps.MapView;

import android.os.Bundle;
import android.widget.FrameLayout;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = new MapView(this, MapRenderMode.VECTOR);  // or use MapRenderMode.RASTER for 2D map
        mMapView.setCredentialsKey("Aibv2h_ydIa8eDaAqyNeZ91zy8zPEjqltZzrUp6jf3JAR8G8tgPvz0JIBtd9L2De");
        ((FrameLayout) findViewById(R.id.map_view)).addView(mMapView);
        mMapView.onCreate(savedInstanceState);
    }
}