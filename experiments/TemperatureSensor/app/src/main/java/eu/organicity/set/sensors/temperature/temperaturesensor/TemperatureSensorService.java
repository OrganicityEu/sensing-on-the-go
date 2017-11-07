package eu.organicity.set.sensors.temperature.temperaturesensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;

public class TemperatureSensorService extends Service implements SensorEventListener {

    private static final String TAG = "NoiseSensorService";
    private static final int MSG_PLUGIN_INFO = 53;
    public static String CONTEXT_TYPE = "org.ambientdynamix.contextplugins.TemperaturePlugin";

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private float temperature;

    private ISensorCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");



    public TemperatureSensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //TODO setup here

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");

        if (mSensorManager != null) {
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Handler Thread handling all call back methods
        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed!");
        //TODO unregister listeners, unbind services and clean up here
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    /**
     * Stub implementation for Remote service
     */
    ISensorService.Stub mBinder = new ISensorService.Stub() {

        @Override
        public void getPluginInfo(ISensorCallback callback) throws RemoteException {

            Log.d(TAG, "getPluginInfo called!");
            sendMsgToHandler(callback, MSG_PLUGIN_INFO);
        }
    };

    /**
     * Create handler message to be sent
     *
     * @param callback
     * @param flag
     */
    void sendMsgToHandler(ISensorCallback callback, int flag) {

        mRemoteCallbacks = callback;

        Message message = mHandler.obtainMessage();

        message.what = flag;
        mHandler.sendMessage(message);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        temperature = sensorEvent.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * Handler class sending result in callback to respective
     * application
     */
    private class ServiceHandler extends Handler {
        int callbackIndex = 0;

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            callbackIndex = msg.arg1;

            switch (msg.what) {

                case MSG_PLUGIN_INFO:
                    //TODO sensor logic here

                    publishResults();
                    break;
            }
        }
    }

    public void publishResults() {
        List<Reading> r = new ArrayList<Reading>();
        JsonMessage info = new JsonMessage();
        try {
            JSONObject obj = new JSONObject();
            obj.put("org.ambientdynamix.contextplugins.AmbientTemperature", temperature);
            r.add(new Reading(obj.toString(), CONTEXT_TYPE));
            info.setPayload(r);
            info.setState("OK");

            try {
                if (mRemoteCallbacks != null) {
                    mRemoteCallbacks.handlePluginInfo(info);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
