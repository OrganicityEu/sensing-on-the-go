package eu.organicity.set.sensors.wifi;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.intentfilter.androidpermissions.PermissionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.sensors.sdk.IAidlCallback;
import eu.organicity.set.sensors.sdk.IAidlService;
import eu.organicity.set.sensors.sdk.JsonMessage;

public class WiFiSensorService extends Service {

    private static final String TAG = "WiFiSensorService";
    private static final int MSG_PLUGIN_INFO = 53;

    private IAidlCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");

    private WifiManager wifiManager;
    private WifiReceiver wifiReceiver;

    public WiFiSensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created");

        List<String> perm = new ArrayList<>();
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
                Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        });

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiReceiver = new WifiReceiver();
        registerReceiver(wifiReceiver, new IntentFilter(
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        return super.onStartCommand(intent, flags, startId);
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
        unregisterReceiver(wifiReceiver);
        super.onDestroy();
    }

    /**
     * Stub implementation for Remote service
     */
    IAidlService.Stub mBinder = new IAidlService.Stub() {

        @Override
        public void getPluginInfo(IAidlCallback callback) throws RemoteException {

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
    void sendMsgToHandler(IAidlCallback callback, int flag) {

//        mRemoteCallbacks.add(callback);
        mRemoteCallbacks = callback;

        Message message = mHandler.obtainMessage();
//        message.arg1 = mRemoteCallbacks.size() - 1;

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
                    if (wifiManager != null) {
                        wifiManager.startScan();
                    }
                    break;
            }
        }
    }

    class WifiReceiver extends BroadcastReceiver {
        public void onReceive(Context c, Intent intent) {
            if (!intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)) {
                return;
            }

            JsonMessage message = new JsonMessage();
            JSONArray jsonArray = new JSONArray();

            List<ScanResult> wifiList = wifiManager.getScanResults();

            for (ScanResult result : wifiList) {
                JSONObject jsonObject = new JSONObject();

                String[] stringResult = result.toString().split(",");
                for (String str : stringResult) {
                    String[] keyValue = str.split(":");
                    try {
                        jsonObject.put(keyValue[0].trim(), keyValue[1].trim());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                jsonArray.put(jsonObject);
            }

            message.put("results", jsonArray);

            try {
                if (mRemoteCallbacks != null) {
                    mRemoteCallbacks.handlePluginInfo(message);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }
}
