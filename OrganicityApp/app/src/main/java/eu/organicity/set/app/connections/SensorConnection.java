package eu.organicity.set.app.connections;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;

/**
 * Created by chris on 26/10/2017.
 */

public class SensorConnection implements ServiceConnection {

    private static final String TAG = "SensorConnection";
    public ISensorService service;
    public ISensorCallback callback;
    String serviceName;
    String servicePackage;
    SensorCallback scheduler;

    public SensorConnection(String name, String pkg, final SensorCallback scheduler) {
        this.serviceName = name;
        this.servicePackage = pkg;
        this.scheduler = scheduler;

        this.callback = new ISensorCallback.Stub() {
            @Override
            public void handlePluginInfo(JsonMessage message) throws RemoteException {
                try {
                    JSONArray arr = new JSONArray(message.getPayload());
                    Reading reading = Reading.fromJson(arr.get(0).toString());

                    Log.d(TAG, serviceName + ": Message received: " + reading.toJson());
                    AppModel.instance.readingStorage.pushReading(reading);

                    scheduler.updateSensorResults(serviceName, message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "Service " + serviceName + " connected");
        service = ISensorService.Stub.asInterface(iBinder);
        if (scheduler != null) {
            Log.d(TAG, "Service " + serviceName + " notifying listener");
            scheduler.sensorConnected(serviceName);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.e(TAG, "Service has unexpectedly disconnected");
        callback = null;

        if (scheduler != null) {
            scheduler.sensorDisconnected(serviceName);
        }
    }

    public interface SensorCallback {
        void sensorConnected(String service);
        void sensorDisconnected(String service);
        void updateSensorResults(String service, JsonMessage result);
    }
}