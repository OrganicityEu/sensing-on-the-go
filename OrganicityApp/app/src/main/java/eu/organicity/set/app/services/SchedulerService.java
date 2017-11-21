package eu.organicity.set.app.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.R;
import eu.organicity.set.app.activities.MainActivity;
import eu.organicity.set.app.connections.ExperimentConnection;
import eu.organicity.set.app.connections.SensorConnection;
import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

import static eu.smartsantander.androidExperimentation.util.Constants.EXPERIMENT_STATE_INTENT;
import static eu.smartsantander.androidExperimentation.util.Constants.STATE;

/**
 * Created by chris on 26/10/2017.
 */

public class SchedulerService extends Service implements SensorConnection.SensorCallback, ExperimentConnection.ExperimentCallbacks {

    public static final String STOP = "STOP";

    private static final int id = 1020;
    private static final String EXPERIMENT = "EXPERIMENT";
    private static final String TAG = "SchedulerService";
    private static final int PLAY_CODE = 210;
    private static final int STOP_CODE = 211;
    private static final long DELAY = 60000;

    private HashSet<String> connected;

    private BroadcastReceiver playReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);

            Experiment exp = AppModel.instance.experiment;

            if (exp != null) {
                if (exp.getState() == Experiment.State.RUNNING) {
                    stopExperiment(exp);
                }
                else {
                    startExperiment(exp);
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "No last experiment found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private BroadcastReceiver stopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(it);

            if (AppModel.instance.experiment != null) {
                stopExperiment(AppModel.instance.experiment);
            }

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.cancel(id);

            Intent kill = new Intent("kill-organicity");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(kill);

            stopSelf();
        }
    };

    private Handler handler;
    private Runnable runnable;
    private RemoteViews notificationView;
    private Notification.Builder builder;

    public SchedulerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Scheduler created!");

        AppModel.instance.started = new ArrayList<>();
        AppModel.instance.connections = new HashMap<>();
        connected = new HashSet<>();

        if (!AppModel.instance.isDeviceRegistered()) {
            AppModel.instance.phoneProfiler.register();
        }

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                for (String key : AppModel.instance.started) {
                    ISensorService service = AppModel.instance.connections.get(key).service;
                    ISensorCallback callback = AppModel.instance.connections.get(key).callback;

                    if (service != null && callback != null) {
                        try {
                            Log.d(TAG, "Calling getPluginInfo for " + key);
                            service.getPluginInfo(callback);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
                }

                if (AppModel.instance.experimentConnection != null && AppModel.instance.experimentConnection.service != null) {
                    boolean run = true;

                    if (AppModel.instance.readingStorage.getReadingsCount() == 0) {
                        run = false;
                    }

                    if (run) {
                        JsonMessage result = new JsonMessage();

                        try {
                            AppModel.instance.experimentConnection.service.getExperimentResult(AppModel.instance.readingStorage.getBundle(), result);
                            Log.d(TAG, "Result: " + result.getPayload());

                            try {
                                JSONArray array = new JSONArray(result.getPayload());
                                JSONObject res = new JSONObject((String) array.getJSONObject(0).get("value"));

                                AppModel.publishMessage(getApplicationContext(), res.toString());
                                Log.d(TAG, "Result: " + res.toString());

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                handler.postDelayed(runnable, DELAY);
            }
        };


        registerReceiver(playReceiver, new IntentFilter("organicity-play-intent"));
        registerReceiver(stopReceiver, new IntentFilter("organicity-stop-intent"));

        //Intent intent = new Intent(this, NotificationReceiver.class);
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), intent, 0);

        Intent playIntent = new Intent("organicity-play-intent");
        PendingIntent playpIntent = PendingIntent.getBroadcast(getApplicationContext(), PLAY_CODE, playIntent, 0);

        Intent stopIntent = new Intent("organicity-stop-intent");
        PendingIntent stoppIntent = PendingIntent.getBroadcast(getApplicationContext(), STOP_CODE, stopIntent, 0);

        notificationView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        notificationView.setOnClickPendingIntent(R.id.play, playpIntent);
        notificationView.setOnClickPendingIntent(R.id.stop, stoppIntent);

        notificationView.setTextViewText(R.id.play, "Play");

        // build notification
        // the addAction re-use the same intent to keep the example short
        builder = new Notification.Builder(getApplicationContext())
                .setContentTitle("Organicity")
                .setContentText("")
                .setContent(notificationView)
                .setSmallIcon(R.drawable.status_bar_icon)
                .setContentIntent(pIntent)
                .setOngoing(true)
                .setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());

        handler.post(runnable);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");

        handler.removeCallbacks(runnable);
        unregisterReceiver(playReceiver);
        unregisterReceiver(stopReceiver);

        if (AppModel.instance.experiment != null) {
            stopExperiment(AppModel.instance.experiment);
        }
        experimentStopped();

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(id);

        AppModel.instance.started.clear();

        if (AppModel.instance.experiment != null) {
            experimentStopped();
        }

        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Scheduler onStartCommand!");

        if (intent != null) {
            if (intent.hasExtra(EXPERIMENT)) {
                Experiment experiment = (Experiment) intent.getSerializableExtra(EXPERIMENT);

                if (experiment != null) {

                    if (intent.hasExtra(STOP)) {
                        stopExperiment(experiment);
                    }
                    else {
                        handleExperiment(experiment);
                    }
                }
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void handleExperiment(Experiment experiment) {
        AppModel.instance.experiment = experiment;
        Log.d(TAG, "Starting experiment: " + experiment.getName() + " in package: " + experiment.getPkg());

        setupSensorServices(experiment.getSensors());
    }

    private void setupSensorServices(List<Sensor> sensors) {
        for (Sensor s : sensors) {
            SensorConnection connection = new SensorConnection(s.getService(), s.getPkg(), this);

            AppModel.instance.connections.put(s.getKey(), connection);
        }

        startSensors(AppModel.instance.experiment);
    }

    private void stopSensors(Experiment experiment) {
        for (Sensor sensor : experiment.getSensors()) {
            Log.d(TAG, "Stopping sensor: " + sensor.getName() + " in package: " + sensor.getPkg());
            stopSensor(sensor);
        }
    }

    private void stopSensor(Sensor sensor) {
        String pkg = sensor.getPkg();
        String key = sensor.getKey();

        boolean installed = false;
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(pkg, 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Cannot stop uninstalled sensor!");
            e.printStackTrace();
        }

        if (installed && AppModel.instance.started.contains(key)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            unbindService(AppModel.instance.connections.get(key));
            AppModel.instance.connections.get(key).service =  null;
            boolean result = stopService(serviceIntent);
            Log.d(TAG, "Stopping service " + key + " with result " + result);
            AppModel.instance.started.remove(key);
        }
    }

    private void startSensors(Experiment experiment) {
        for (Sensor sensor : experiment.getSensors()) {
            Log.d(TAG, "Starting sensor: " + sensor.getName() + " in package: " + sensor.getPkg());
            startSensor(sensor);
        }
    }

    private void startSensor(Sensor sensor) {
        String pkg = sensor.getPkg();
        String key = sensor.getKey();

        boolean installed = false;
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(pkg, 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }


        if (installed && !AppModel.instance.started.contains(key)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            bindService(serviceIntent, AppModel.instance.connections.get(key), Context.BIND_AUTO_CREATE);
            startService(serviceIntent);
            AppModel.instance.started.add(key);
        }
    }

    private void startExperiment(Experiment experiment) {
        String pkg = experiment.getPkg();
        String key = pkg + "." + experiment.getService();

        AppModel.instance.experimentConnection = new ExperimentConnection(this);
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(pkg, key);

        Log.d(TAG, "Starting experiment service: " + experiment.getName() + " in package: " + experiment.getPkg());
        bindService(serviceIntent, AppModel.instance.experimentConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);

        notificationView.setTextViewText(R.id.play, "Stop");
        updateNotification();
    }

    private void stopExperiment(Experiment experiment) {
        stopSensors(experiment);

        if (AppModel.instance.experimentConnection != null && AppModel.instance.experimentConnection.service != null) {
            Log.d(TAG, "Stopping experiment service: " + experiment.getName() + " in package: " + experiment.getPkg());

            String pkg = experiment.getPkg();
            String key = pkg + "." + experiment.getService();

            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            unbindService(AppModel.instance.experimentConnection);
            stopService(serviceIntent);

            handler.removeCallbacks(runnable);

            experimentStopped();

            notificationView.setTextViewText(R.id.play, "Start");
            updateNotification();

            Toast.makeText(getApplicationContext(), "Experiment stopped", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Cannot stop null experiment!");
        }
    }

    private void updateNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());
    }

    @Override
    public void sensorConnected(String service) {
        Log.d(TAG, "Sensor " + service + " connected");
        connected.add(service);

        if (AppModel.instance.experiment == null) {
            Log.d(TAG, "Experiment is null");
            return;
        }

        boolean start = true;
        for (Sensor s : AppModel.instance.experiment.getSensors()) {
            if (!connected.contains(s.getService())) {
                start = false;
            }
        }

        if (start) {
            startExperiment(AppModel.instance.experiment);
        }
    }

    @Override
    public void sensorDisconnected(String service) {
        Log.d(TAG, "Sensor " + service + " disconnected");

        connected.remove(service);
    }

    @Override
    public void updateSensorResults(String service, JsonMessage result) {
        try {
            JSONArray arr = new JSONArray(result.getPayload());
            Reading reading = Reading.fromJson(arr.get(0).toString());
            AppModel.instance.readingStorage.pushReading(reading);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void experimentStarted() {
        Intent intent = new Intent(EXPERIMENT_STATE_INTENT);
        intent.putExtra(EXPERIMENT, AppModel.instance.experiment);
        intent.putExtra(STATE, Experiment.State.RUNNING);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void experimentStopped() {
        Intent intent = new Intent(EXPERIMENT_STATE_INTENT);
        intent.putExtra(EXPERIMENT, AppModel.instance.experiment);
        intent.putExtra(STATE, Experiment.State.STOPPED);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
