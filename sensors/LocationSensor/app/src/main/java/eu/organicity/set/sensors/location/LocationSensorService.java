package eu.organicity.set.sensors.location;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.intentfilter.androidpermissions.PermissionManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.sensors.sdk.IAidlCallback;
import eu.organicity.set.sensors.sdk.IAidlService;
import eu.organicity.set.sensors.sdk.JsonMessage;

public class LocationSensorService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    static final String TAG = "LocationSensorService";

    private static final int MSG_PLUGIN_INFO = 53;
    private static final String KEY_LAT = "LAT";
    private static final String KEY_LNG = "LNG";

    private IAidlCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");

    private LocationListener locationListener;
    private LocationManager locationManager;

    private double lat;
    private double lng;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private String stringLocation = "unknown";
    private JSONObject locationJson;
    private String status;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created");

        buildGoogleApiClient();
        mGoogleApiClient.connect();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lat = location.getLatitude();
                lng = location.getLongitude();

                Log.d(TAG, "Location update: " + lat + ", " + lng);
            }
        };

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
                Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
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
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationListener); //Import should not be **android.Location.LocationListener**
        mGoogleApiClient.disconnect();
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

        mRemoteCallbacks = callback;

        Message message = mHandler.obtainMessage();
//        message.arg1 = mRemoteCallbacks.size() - 1;

        message.what = flag;
        mHandler.sendMessage(message);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "GoogleApiClient connected!");
        createLocationRequest();
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
//        lat = location.getLatitude();
//        lng = location.getLongitude();
//
//        Log.i(TAG, " Location: " + location);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void createLocationRequest() {
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        //remove location updates so that it resets
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, locationListener); //Import should not be **android.Location.LocationListener**
        //import should be **import com.google.android.gms.location.LocationListener**;

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //restart location updates with the new interval
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, locationListener);
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

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        Log.d(TAG, "Location permission denied");
                        return;
                    }

                    JsonMessage message = new JsonMessage();
//                    message.put(KEY_LAT, String.valueOf(lat));
//                    message.put(KEY_LNG, String.valueOf(lng));

                    Location location;
                    try {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location == null) {
                            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        }

                        if (location != null) {
                            stringLocation = location.getLatitude() + "," + location.getLongitude();
                            locationJson = new JSONObject();
                            message.put("org.ambientdynamix.contextplugins.Latitude", String.valueOf(location.getLatitude()));
                            message.put("org.ambientdynamix.contextplugins.Longitude", String.valueOf(location.getLongitude()));
                            status = "valid";
                        } else {
                            locationJson = null;
                            stringLocation = "";
                            status = "invalid";
                        }
                    } catch (Exception e) {
                        Log.w("GPS Plugin Error", e.toString());
                        stringLocation = "";
                        status = "invalid";
                    }

                    Log.w(TAG, "GPS Plugin:" + stringLocation);
//                        PluginInfo info = new PluginInfo();
//                        info.setState(status);
//                        List<Reading> r = new ArrayList<Reading>();
//                        r.add(new Reading(Reading.Datatype.String, this.locationJson.toString(), PluginInfo.CONTEXT_TYPE));
//                        info.setPayload(r);
//                        Log.w(TAG, "GPS Plugin:" + info.getPayload());
                    Log.w(TAG, "GPS Plugin: " + locationJson.toString());
//                        if (requestId != null) {
//                            sendContextEvent(requestId, new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
//                            Log.w(TAG, "GPS Plugin from Request:" + info.getPayload());
//                        } else {
//                            sendBroadcastContextEvent(new SecuredContextInfo(info, PrivacyRiskLevel.LOW), 60000);
//                            Log.w(TAG, "GPS Plugin Broadcast:" + info.getPayload());
//                        }


                    try {
                        mRemoteCallbacks.handlePluginInfo(message);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
//                            mRemoteCallbacks.remove(0);
                    mRemoteCallbacks = null;

                    break;
            }
        }
    }
}