package eu.organicity.set.app.operations;

import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import eu.organicity.set.app.fragments.HomeFragment;
import eu.smartsantander.androidExperimentation.jsonEntities.Entities;

public class AsyncGetOrganicityMarkersTask extends AsyncTask<HomeFragment, LatLng, String> {
    private final String TAG = "GetMarkersTask";
    private HomeFragment activity;

    @Override
    protected String doInBackground(final HomeFragment... params) {
        activity = params[0];
        try {
            final URL yahoo = new URL("http://ec2-54-68-181-32.us-west-2.compute.amazonaws.com:8090/v1/entities");
            final BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            yahoo.openStream()));

            String inputLine;
            final StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
            in.close();
            final List<Entities> entities = new ObjectMapper().readValue(sb.toString(),
                    new TypeReference<List<Entities>>() {
                    });
            for (final Entities entity : entities) {
                double lat = entity.getData().getLocation().getLatitude();
                double lon = entity.getData().getLocation().getLongitude();
                publishProgress(new LatLng(lat, lon));
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }

        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(final String result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(final LatLng... values) {
        final MarkerOptions marker = new MarkerOptions();
        marker.position(values[0]);

        activity.getMap().getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.addMarker(marker);
            }
        });
    }

    @Override
    protected void onCancelled() {
    }

}
