package com.chattree.chattree.profile;

import android.Manifest;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chattree.chattree.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    TextView textView;
    Geocoder geocoder;
    List<Address> addresses;
    double lat;
    double lon;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        /*location code */
        Button btnGetLoc;
        btnGetLoc = (Button) findViewById(R.id.btnGetLoc);
        textView = (TextView) findViewById(R.id.location);
        geocoder = new Geocoder(this, Locale.getDefault());

        ActivityCompat.requestPermissions(ProfileActivity.this,new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION},123);


        btnGetLoc.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                GPS g = new GPS(getApplicationContext());
                Location l = g.getLocation();
                if(l != null){
                    lat = l.getLatitude();
                    lon = l.getLongitude();
                    //Toast.makeText(getApplicationContext(),"LAT: "+lat+" \n LON : "+lon,Toast.LENGTH_LONG).show();

                    try {
                        addresses = geocoder.getFromLocation(lat,lon,1);
                        String city = addresses.get(0).getLocality();
                        String country = addresses.get(0).getCountryName();

                        String fullAddress = city+", "+country;
                        textView.setText(fullAddress);
                    }catch (IOException e){
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}


