package com.example.unknoqn.cc;

import android.graphics.Color;
import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by unknown on 7/20/2017.
 */

public class CCMap implements OnMapReadyCallback {

    private static final CCMap inst = new CCMap();

    GoogleMap map;
    CC cc;
    LatLng prev;
    Polyline pl;
    long updateCounter = 0;
    boolean current_pos = true;
    boolean moving = false;
    ArrayList<Polyline> segments = new ArrayList<>();

    CCMap() {
    }

    public static synchronized CCMap getInstance() {
        return inst;
    }

    public void init(CC _cc) {
        cc = _cc;
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap _map) {
        map = _map;
        map.setMyLocationEnabled(true);

        map.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                current_pos = true;
                moving = true;
                return false;
            }
        });

        map.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() { // @TODO its very unstable
                Log.d("MOVING", ""+moving);
                if(moving) {
                    moving = false;
                } else {
                    current_pos = false;
                }
            }
        });

        pl = map.addPolyline(new PolylineOptions().width(3).color(Color.BLUE));

        reloadSegments();
    }

    public void reloadSegments() {
        Iterator<Polyline> it = segments.iterator();
        while(it.hasNext()) {
            Polyline pl = it.next();
            pl.remove();
        }
        segments.clear();

        CCStrava strava = CCStrava.getInstance();

        Iterator<List<LatLng>> it2 = strava.getSegments().iterator();
        while(it2.hasNext()) {
            setSegment(it2.next());
        }
    }

    public void setSegment(List<LatLng> segment) {
        Polyline pl = map.addPolyline(new PolylineOptions().width(3).color(Color.GREEN));
        pl.setPoints(segment);
        segments.add(pl);
    }

    public void setLatLng(double la, double ln) {
        if(map == null) { return; }

        updateCounter++;
        LatLng ll = new LatLng(la, ln);

        if(0 == updateCounter % 10) {
            List<LatLng> l = pl.getPoints();
            l.add(ll);
            pl.setPoints(l);
        }

        Log.d("CURRENT_POS", ""+current_pos);
        if(current_pos) {
            moving = true;
            Log.d("MOVING", "to true");
            if (null == prev) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 15));
            } else {
                map.moveCamera(CameraUpdateFactory.newLatLng(ll));
            }
        }
        prev = ll;
    }

    public int checkSegmentStart(double la, double ln) {
        Location current_loc = new Location("A");
        current_loc.setLatitude(la);
        current_loc.setLongitude(ln);

        Iterator<Polyline> it = segments.iterator();
        while(it.hasNext()) {
            Polyline pl = it.next();
            List<LatLng> ps = pl.getPoints();
            if(! ps.isEmpty()) {
                LatLng ll = ps.get(0);
                Location l = new Location("B");
                l.setLatitude(ll.latitude);
                l.setLongitude(ll.longitude);
                float meters = l.distanceTo(current_loc);
                if(meters <= 500) {
                    Toast.makeText(cc, "Strava in "+meters+" meters", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(cc, "Empty segment?", Toast.LENGTH_LONG).show();
            }
        }
        return 0;
    }

    public void disable() {
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getView().setVisibility(View.GONE); // @TODO how to disable?
    }

    public void enable() {
        SupportMapFragment mf = (SupportMapFragment) cc.getSupportFragmentManager().findFragmentById(R.id.map);
        mf.getView().setVisibility(View.VISIBLE);
    }

}
