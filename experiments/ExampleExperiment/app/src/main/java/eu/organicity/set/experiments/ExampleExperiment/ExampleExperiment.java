package eu.organicity.set.experiments.ExampleExperiment;

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


//TODO: don't forget to change the service name in AndroidManifest.xml
public class ExampleExperiment extends Service {

    private static String TAG;
    public static String CONTEXT_TYPE;

    public ExampleExperiment() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT_TYPE = getApplicationContext().getPackageName();
        TAG = this.getClass().getSimpleName();

        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        return START_STICKY;
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
            /*
                Example of reading gps and noise data from sensors
             */

            String jsonReading = bundle.getString("eu.organicity.set.sensors.location.LocationSensorService");
            String jsonReadingNoiseScan = bundle.getString("eu.organicity.set.sensors.noise.NoiseSensorService");

            //Add timestamp
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("timestamp",  System.currentTimeMillis());

                Reading gpsReading = Reading.fromJson(jsonReading);
                Reading noiseReading = Reading.fromJson(jsonReadingNoiseScan);

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

                if (noiseReading != null) {
                    Log.w("Experiment Message:", noiseReading.toJson());
                    JSONObject noise = new JSONObject(noiseReading.getValue());
                    jsonObject.put("eu.organicity.set.sensors.noise.NoiseLevel", noise.get("eu.organicity.set.sensors.noise.NoiseLevel"));
                } else {
                    jsonObject.put("eu.organicity.set.sensors.noise.NoiseLevel", "");
                }

                if (gpsReading != null && noiseReading != null) {
                    jsonMessage1 = publishResult(jsonMessage1);
                }

                Log.d(TAG, "Result readings: " + jsonMessage1.getPayload());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private JsonMessage publishResult(JsonMessage result) {
        List<Reading> r = new ArrayList<>();
        r.add(new Reading(result.toString(), CONTEXT_TYPE));
        result.setPayload(r);

        return result;
    }

}
