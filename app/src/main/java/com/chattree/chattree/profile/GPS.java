package com.chattree.chattree.profile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Created by desir on 21/11/17.
 */

public class GPS implements LocationListener {

    private Context context;

    GPS(Context c) {
        context = c;
    }

    Location getLocation() {
        // Check is permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission not granted", Toast.LENGTH_SHORT).show();
            return null;
        }
        LocationManager lm          = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean         isGPSEnable = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnable) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 6000, 10, this);
            return lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else {
            Toast.makeText(context, "Please enable GPS", Toast.LENGTH_LONG).show();
        }
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

}
