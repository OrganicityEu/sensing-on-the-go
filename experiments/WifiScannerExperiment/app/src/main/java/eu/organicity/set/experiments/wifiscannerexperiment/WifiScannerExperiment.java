package eu.organicity.set.experiments.wifiscannerexperiment;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.app.sdk.IExperimentService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;

public class WifiScannerExperiment extends Service {

    private static final String TAG = "WiFiSensorService";
    public static String CONTEXT_TYPE;

    public WifiScannerExperiment() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT_TYPE = getApplicationContext().getPackageName();

        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        if (intent != null) {
            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed!");
        super.onDestroy();
    }

    /**
     * Stub implementation for Remote service
     */
    IExperimentService.Stub mBinder = new IExperimentService.Stub() {

        @Override
        public void getExperimentResult(Bundle bundle, JsonMessage jsonMessage1) throws RemoteException {

            String jsonReading = bundle.getString("eu.organicity.set.sensors.location.LocationSensorService");
            String jsonReadingWifiScan = bundle.getString("eu.organicity.set.sensors.wifi.WifiSensorService");

            //Add timestamp
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("timestamp",  System.currentTimeMillis());

                Reading gpsReading = Reading.fromJson(jsonReading);
                Reading wifiReading = Reading.fromJson(jsonReadingWifiScan);

                jsonMessage1.setState("ACTIVE");

                if (gpsReading != null) {
                    Log.w("Experiment Message:", gpsReading.toJson());
                    JSONObject gps = new JSONObject(gpsReading.getValue());
                    jsonObject.put("eu.organicity.set.sensors.location.Longitude", gps.get("eu.organicity.set.sensors.location.Longitude"));
                    jsonObject.put("eu.organicity.set.sensors.location.Latitude", gps.get("eu.organicity.set.sensors.location.Latitude"));
                } else {
                    jsonObject.put("eu.organicity.set.sensors.location.Longitude", "");
                    jsonObject.put("eu.organicity.set.sensors.location.Latitude", "");
                }

                if (wifiReading != null) {
                    Log.w("Experiment Message:", wifiReading.toJson());
                    JSONObject noise = new JSONObject(wifiReading.getValue());
                    jsonObject.put("eu.organicity.set.sensors.wifi.WifiList", noise.get("eu.organicity.set.sensors.wifi.WifiList"));
                } else {
                    jsonObject.put("eu.organicity.set.sensors.wifi.WifiList", "");
                }

                if (gpsReading != null && wifiReading != null) {
//                    jsonMessage1.setJSON(jsonObject);

                    List<Reading> r = new ArrayList<>();
                    r.add(new Reading(jsonObject.toString(), CONTEXT_TYPE));
                    jsonMessage1.setPayload(r);
                }



                Log.d(TAG, "Result readings: " + jsonMessage1.getPayload());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

}
