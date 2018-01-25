package eu.organicity.set.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import eu.organicity.set.app.connections.ExperimentConnection;
import eu.organicity.set.app.connections.SensorConnection;
import eu.organicity.set.app.fragments.HomeFragment;
import eu.organicity.set.app.operations.AsyncReportNowTask;
import eu.organicity.set.app.operations.PhoneProfiler;
import eu.organicity.set.app.sdk.Report;
import eu.smartsantander.androidExperimentation.DataStorage;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.ReadingStorage;
import eu.smartsantander.androidExperimentation.util.Constants;

import static eu.organicity.set.app.utils.Constants.RESULT_ACTION;

/**
 * Created by chris on 31/10/2017.
 */

public class AppModel {

    private static final String TAG = "AppModel";
    private static final String GPS_PLUGIN = "org.ambientdynamix.contextplugins.GpsPlugin";

    public static AppModel instance;

    static void create(Context context) {
        if (instance == null) {
            instance = new AppModel(context);
        }
    }

    private AppModel(Context context) {
        readingStorage = new ReadingStorage();
        storage = DataStorage.getInstance(context);
        phoneProfiler = new PhoneProfiler();
        this.context = context;
    }

    public List<String> started = new ArrayList<>();
    public HashMap<String, SensorConnection> connections = new HashMap<>();
    public ExperimentConnection experimentConnection;
    public Experiment experiment = null;
    public ReadingStorage readingStorage;
    public PhoneProfiler phoneProfiler;

    public Context context;
    public static DataStorage storage;

    private static final LinkedList<String> experimentMessageQueue = new LinkedList<>();
    private static MixpanelAPI mMixpanel = null;

    public static void publishMessage(Context context, String message) {
        if (AppModel.instance.experiment == null) {
            Log.e(TAG, "Cannot publish message for experiment: null");
            return;
        }

        Log.d(TAG, "publishMessage");
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        //            final Report aa = new ObjectMapper().readValue(message, Report.class);
        final Report aa = new Report();
        aa.setDeviceId(AppModel.instance.phoneProfiler.getPhoneId());
        aa.setExperimentId(AppModel.instance.experiment.getId());
        aa.setJobResults(message);

        final Intent i = new Intent();
        i.setAction(RESULT_ACTION);
        i.putExtra("result", aa.getJobResults());
        LatLng latlng = parseExperimentMessage(aa);


        if (latlng != null) {
            if (HomeFragment.location != null) {
                Log.i(TAG, "message:" + message);
                String nMessage = message;
                nMessage = nMessage.replace("" + latlng.latitude, "" + HomeFragment.location.getLatitude());
                nMessage = nMessage.replace("" + latlng.longitude, "" + HomeFragment.location.getLongitude());
                message = nMessage;
                Log.i(TAG, "nMessage" + nMessage);
            } else {
                Log.i(TAG, "HomeActivity.location=null");
            }

            Log.i(TAG, "lat:" + latlng.latitude);
            i.putExtra("lat", latlng.latitude);
            i.putExtra("lon", latlng.longitude);
        }
        context.sendBroadcast(i);

        if (mWifi.isConnected()) {
            Log.i(TAG, "will use AsyncReportNowTask");
            new AsyncReportNowTask().execute(aa);
        } else {
            Log.i(TAG, "will use addExperimentalMessage");
            addExperimentalMessage(message);
        }
    }

    private static LatLng parseExperimentMessage(final Report report) {
        if (report.getExperimentId().equals("0")) {
            return null;
        }

        Double longitude = null;
        Double latitude = null;
        for (final String result : report.getJobResults().split(",")) {
            if (result.contains("eu.organicity.set.sensors.location.Latitude")) {
                latitude = Double.valueOf(result.split(":")[1].trim());
            }
            else if (result.contains("eu.organicity.set.sensors.location.Longitude")) {
                longitude = Double.valueOf(result.split(":")[1].trim());
            }
        }

        if (longitude != null && latitude != null) {
            return new LatLng(latitude, longitude);
        }

        return null;
    }

    public static synchronized void addExperimentalMessage(String message) {
        try {
            storage.addMessage(message);
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        }
    }

    public static synchronized void deleteExperimentalMessage(Long id) {
        try {
            storage.deleteMessage(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cacheExperimentalMessage(String message) {
        experimentMessageQueue.addLast(message);
        if (experimentMessageQueue.size() > 10) {
            experimentMessageQueue.poll();
        }
    }

    public void mixpanelIdentify() {
        mMixpanel = MixpanelAPI.getInstance(this.context, BuildConfig.SET_MIXPANEL_TOKEN);
        mMixpanel.identify(String.valueOf(this.phoneProfiler.getPhoneId()));
        mMixpanel.getPeople().identify(String.valueOf(this.phoneProfiler.getPhoneId()));
    }

    public void mixpanelRecord(String key, long value) {
        mMixpanel = MixpanelAPI.getInstance(this.context, BuildConfig.SET_MIXPANEL_TOKEN);
        mMixpanel.getPeople().identify(String.valueOf(this.phoneProfiler.getPhoneId()));
        mMixpanel.getPeople().set(key, value);
        mMixpanel.flush();
    }

    public Boolean isDeviceRegistered() {
        return this.phoneProfiler.getPhoneId() != Constants.PHONE_ID_UNITIALIZED;
    }
}
