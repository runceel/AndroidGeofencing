package com.example.kazuki.myapplication;

import android.*;
import android.app.IntentService;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

import java.util.LinkedList;
import java.util.List;

public class GeofenceTransitionsIntentService extends Service implements GoogleApiClient.ConnectionCallbacks{
    private List<Geofence> geofences = new LinkedList<>();
    private GoogleApiClient client;

    @Override
    public void onCreate() {
        super.onCreate();
        client = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        client.connect();
        Log.d("Sample", "onCreate");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        client.disconnect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return super.onStartCommand(intent, flags, startId);
        }
        Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(this.client);
        if (lastLocation == null) {
            Log.d("Sample", "lastLocation is null");
        } else {
            float[] distances = new float[3];
            Location.distanceBetween(34.38511, 132.4539817, lastLocation.getLatitude(), lastLocation.getLongitude(), distances);
            Log.d("Sample", "lastLocation: " + lastLocation.getLatitude() + ", " + lastLocation.getLongitude() + ", " + distances[0] + "m");
        }
        GeofencingEvent e = GeofencingEvent.fromIntent(intent);
        if (e == null || e.getTriggeringLocation() == null) {
            Log.d("Sample", "GeofencingEvent#getTriggeringLocation is null");
            return super.onStartCommand(intent, flags, startId);
        }

        String reason = "";
        if (e.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_ENTER) {
            reason = "ENTER";
        } else if (e.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_EXIT) {
            reason = "EXIT";
        } else if (e.getGeofenceTransition() == Geofence.GEOFENCE_TRANSITION_DWELL) {
            reason = "DWELL";
        }
        Log.d("Sample", reason + ": " + e.getTriggeringLocation().getLatitude() + ", " + e.getTriggeringLocation().getLongitude());
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        this.setupGeofence();
    }

    private void setupGeofence() {
        if (!this.geofences.isEmpty()) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.d("Sample", "setupGeofence");
        this.geofences.add(
                new Geofence.Builder()
                        .setRequestId("home")
                        .setCircularRegion(
                                34.38511,
                                132.4539817,
                                1000
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                        .build()
        );

        GeofencingRequest req = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER | GeofencingRequest.INITIAL_TRIGGER_EXIT | GeofencingRequest.INITIAL_TRIGGER_DWELL)
                .addGeofences(this.geofences)
                .build();

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        LocationServices.GeofencingApi.addGeofences(
                this.client,
                req,
                pendingIntent
        );

    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
