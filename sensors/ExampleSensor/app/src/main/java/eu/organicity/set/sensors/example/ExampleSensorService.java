package eu.organicity.set.sensors.example;

import android.app.Service;
import android.content.Intent;
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

//TODO: don't forget to change the service name in AndroidManifest.xml
public class ExampleSensorService extends Service {
    static String TAG;

    private static final int MSG_PLUGIN_INFO = 53;
    public static String CONTEXT_TYPE;

    private ISensorCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");

    //TODO: declare variable

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT_TYPE = getApplicationContext().getPackageName();
        TAG = this.getClass().getSimpleName();

        Log.d(TAG, "Service created");

        //TODO: initialiaze variablbes


        // TODO: Example of requesting permissions
        /*
        List<String> perm = new ArrayList<>();
        perm.add(Manifest.permission.ACCESS_FINE_LOCATION);
        perm.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        PermissionManager permissionManager = PermissionManager.getInstance(getApplicationContext());
        permissionManager.checkPermissions(perm, new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                Toast.makeText(getApplicationContext(), "Location Sensors: Permissions Granted", Toast.LENGTH_SHORT).show();
                // TODO: Start threads, etc..
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getApplicationContext(), "Location Sensors: Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        });
        */
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Handler Thread handling all call back methods
        Log.d(TAG, "Service onBind was called!");

        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // TODO: unregister receiver, disconnect services, clear and remove threads

        mHandlerThread.quit();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;

        stopSelf();
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
                    String status;
                    JSONObject dataJson;

                    if (valid) {
                        dataJson = new JSONObject();
                        //TODO: put data here in the following format
                        /*
                            dataJson.put(CONTEXT_TYPE + ".Latitude", location.getLatitude());
                            dataJson.put(CONTEXT_TYPE + ".Longitude", location.getLongitude());
                         */

                        status = "valid";
                    } else {
                        dataJson = null;
                        status = "invalid";
                    }

                    // Send result to OrganiCity aoo
                    publishResult(dataJson, status);

                    break;
            }
        }
    }

    private void publishResult(JSONObject dataJson, String status) {
        if (dataJson != null) {
            Log.w(TAG, "Data JSON:" + dataJson.toString());

            // create response object info and fill it with data
            JsonMessage info = new JsonMessage();
            info.setState(status);
            List<Reading> r = new ArrayList<>();
            r.add(new Reading(Reading.Datatype.String, dataJson.toString(), CONTEXT_TYPE + "." + TAG));
            info.setPayload(r);
            Log.w(TAG, "Payload:" + info.getPayload());

            // Try sending the info back to OrganiCity app
            try {
                mRemoteCallbacks.handlePluginInfo(info);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        mRemoteCallbacks = null;
    }
}