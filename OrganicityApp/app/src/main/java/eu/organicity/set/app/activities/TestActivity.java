package eu.organicity.set.app.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.R;
import eu.organicity.set.app.SimpleAdapter;
import eu.organicity.set.app.sdk.IExperimentService;
import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

public class TestActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    public static final String ACTION_PICK_PLUGIN = "organicity.intent.action.PICK_PLUGIN";
    static final String KEY_PKG = "pkg";
    static final String KEY_SERVICENAME = "servicename";
    static final String KEY_ACTIONS = "actions";
    static final String KEY_CATEGORIES = "categories";

    static final String TAG = "MainActivity";
    private static final long DELAY = 5000;
    private List<String> started;
    private Handler handler;
    private Runnable runnable;

    private HashMap<String, AidlConnectionService> connections;
    private Sensor[] sensors = {
            new Sensor("Location sensor", "eu.organicity.set.sensors.location", "LocationSensorService"),
            new Sensor("WiFi sensor", "eu.organicity.set.sensors.wifi", "WifiSensorService"),
            new Sensor("Ble Reader sensor", "eu.organicity.set.sensors.ble", "BLESensorService"),
            new Sensor("Temperature sensor", "eu.organicity.set.sensors.temperature", "TemperatureSensorService"),
            new Sensor("Noise sensor", "eu.organicity.set.sensors.noise", "NoiseSensorService"),

    };

    private HashMap<String, List<JSONObject>> sensorsResults;

    private ExperimentService experimentConnection;
    private boolean experimentConnected;

    Experiment experiment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_old);

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                for (String key : started) {
                    ISensorService service = connections.get(key).aidlService;
                    ISensorCallback callback = connections.get(key).callback;

                    if (service != null && callback != null) {
                        try {
                            Log.d(TAG, "Calling getPluginInfo for " + key);
                            service.getPluginInfo(callback);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }

                    }
                }

                if (experimentConnection != null && experimentConnection.service != null && sensorsResults != null) {
                    boolean run = true;

                    if (AppModel.instance.readingStorage.getReadingsCount() == 0) {
                        run = false;
                    }

                    if (run) {
                        JsonMessage message = new JsonMessage();
                        JSONObject object = new JSONObject();
                        for (String key : sensorsResults.keySet()) {
                            try {
                                object.put(key, sensorsResults.get(key).get(0));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        message.setJSON(object);

                        JsonMessage result = new JsonMessage();

                        try {
                            experimentConnection.service.getExperimentResult(AppModel.instance.readingStorage.getBundle(), result);

                            try {
                                JSONArray array = new JSONArray(result.getPayload());
                                JSONObject res = new JSONObject((String) array.getJSONObject(0).get("value"));

                                AppModel.publishMessage(getApplicationContext(), res.toString());
                                Log.d(TAG, "Result: " + res.toString());

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }

                handler.postDelayed(runnable, DELAY);
            }
        };

        experiment = new Experiment();
        experiment.setName("Noise Level Experiment");
        experiment.setPkg("eu.organicity.set.experiments.noiselevelexperiment");
        experiment.setService("NoiseLevelExperiment");
        experiment.setKey(experiment.getPkg() + "." + experiment.getService());

        List<Sensor> sen = new ArrayList<>();
        sen.add(sensors[0]);
        sen.add(sensors[4]);
        experiment.setSensors(sen);

        experimentConnected = false;
        sensorsResults = new HashMap<>();
        started = new ArrayList<>();
        connections = new HashMap<>();

        fillPluginList();
        itemAdapter = new SimpleAdapter(this, R.layout.plugin_row, sensors);
//        itemAdapter =
//                new SimpleAdapter(this,
//                        services,
//                        R.layout.plugin_row,
//                        new String[]{KEY_PKG, KEY_SERVICENAME, KEY_ACTIONS, KEY_CATEGORIES},
//                        new int[]{R.id.pkg, R.id.servicename, R.id.actions, R.id.categories}
//                );

        ListView list = (ListView) findViewById(android.R.id.list);
        list.setAdapter(itemAdapter);
        list.setOnItemClickListener(this);

        packageBroadcastReceiver = new PackageBroadcastReceiver();
        packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");

        handler.post(runnable);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PackageManager packageManager = getPackageManager();
                try {
                    PackageInfo pkInfo = packageManager.getPackageInfo("eu.organicity.set.experiments.noiselevelexperiment", 0);

                    startExperiment(experiment);

                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startExperiment(Experiment experiment) {
        for (Sensor sensor : experiment.getSensors()) {
            startSensor(sensor);
        }

        String pkg = experiment.getPkg();
        String key = pkg + "." + experiment.getService();

        experimentConnection = new ExperimentService();
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(pkg, key);

        if (experimentConnection.service != null) {
            unbindService(experimentConnection);
            stopService(serviceIntent);

            AppModel.instance.experiment = null;
        }
        else {
            AppModel.instance.experiment = experiment;

            bindService(serviceIntent, experimentConnection, Context.BIND_AUTO_CREATE);
            startService(serviceIntent);
        }
    }

    private void startSensor(Sensor sensor) {
        String name = sensor.getName();
        String serviceName = sensor.getService();
        String pkg = sensor.getPkg();

        String key = pkg + "." + serviceName;

        boolean installed = false;
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo pkInfo = packageManager.getPackageInfo(pkg, 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!installed) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + pkg));
            startActivity(intent);
        }
        else {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            if (started.contains(key)) {
                unbindService(connections.get(key));
                stopService(serviceIntent);
                started.remove(key);
            }
            else {
                bindService(serviceIntent, connections.get(key), Context.BIND_AUTO_CREATE);
                startService(serviceIntent);
                started.add(key);
            }
        }
    }

    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        registerReceiver(packageBroadcastReceiver, packageFilter);
    }

    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        unregisterReceiver(packageBroadcastReceiver);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
        Log.d(TAG, "onListItemClick: " + position);
//        String serviceName = services.get(position).get(KEY_SERVICENAME);
//        String pkg = services.get(position).get(KEY_PKG);

        String name = sensors[position].getName();
        String serviceName = sensors[position].getService();
        String pkg = sensors[position].getPkg();

        String key = pkg + "." + serviceName;

        boolean installed = false;
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo pkInfo = packageManager.getPackageInfo(pkg, 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!installed) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + pkg));
            startActivity(intent);
        }
        else {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            if (started.contains(key)) {
                unbindService(connections.get(key));
                stopService(serviceIntent);
                started.remove(key);
            }
            else {
                bindService(serviceIntent, connections.get(key), Context.BIND_AUTO_CREATE);
                startService(serviceIntent);
                started.add(key);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fillPluginList();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacks(runnable);

        for (String key : connections.keySet()) {
            if (started.contains(key)) {
                unbindService(connections.get(key));

                String pkg = "";
                for (int pos = 0; pos < services.size(); pos++) {
                    if (services.get(pos).get(KEY_SERVICENAME).equals(key)) {
                        pkg = services.get(pos).get(KEY_PKG);
                        break;
                    }
                }

                Intent serviceIntent = new Intent();
                serviceIntent.setClassName(pkg, key);
                stopService(serviceIntent);
            }
        }
    }

    private void fillPluginList() {
        services = new ArrayList<HashMap<String, String>>();
        categories = new ArrayList<String>();

        PackageManager packageManager = getPackageManager();

        Intent baseIntent = new Intent(ACTION_PICK_PLUGIN);
        baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);

        List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent,
                PackageManager.GET_RESOLVED_FILTER);

        Log.d(TAG, "fillPluginList: " + list);

        connections.clear();

        if (list.size() > 0) {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
        else {
            findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < list.size(); ++i) {
            ResolveInfo info = list.get(i);
            ServiceInfo sinfo = info.serviceInfo;
            IntentFilter filter = info.filter;
            Log.d(TAG, "fillPluginList: i: " + i + "; sinfo: " + sinfo + ";filter: " + filter);
            if (sinfo != null) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(KEY_PKG, sinfo.packageName);

                item.put(KEY_SERVICENAME, sinfo.name);

                String firstCategory = null;
                if (filter != null) {
                    StringBuilder actions = new StringBuilder();
                    for (Iterator<String> actionIterator = filter.actionsIterator(); actionIterator.hasNext(); ) {
                        String action = actionIterator.next();
                        if (actions.length() > 0)
                            actions.append(",");
                        actions.append(action);
                    }
                    StringBuilder categories = new StringBuilder();
                    for (Iterator<String> categoryIterator = filter.categoriesIterator();
                         categoryIterator.hasNext(); ) {
                        String category = categoryIterator.next();
                        if (firstCategory == null)
                            firstCategory = category;
                        if (categories.length() > 0)
                            categories.append(",");
                        categories.append(category);
                    }
                    item.put(KEY_ACTIONS, new String(actions));
                    item.put(KEY_CATEGORIES, new String(categories));
                } else {
                    item.put(KEY_ACTIONS, "<null>");
                    item.put(KEY_CATEGORIES, "<null>");
                }
                if (firstCategory == null)
                    firstCategory = "";
                categories.add(firstCategory);
                services.add(item);

                AidlConnectionService connection = new AidlConnectionService(item.get(KEY_SERVICENAME), item.get(KEY_PKG));

                connections.put(item.get(KEY_SERVICENAME), connection);
//                callbacks.put(item.get(KEY_SERVICENAME), callback);
            }
        }
        Log.d(TAG, "services: " + services);
        Log.d(TAG, "categories: " + categories);
    }

    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;
    private ArrayList<HashMap<String, String>> services;
    private ArrayList<String> categories;
    private SimpleAdapter itemAdapter;

    class PackageBroadcastReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "PackageBroadcastReceive";

        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive: " + intent);
            services.clear();
            fillPluginList();
            itemAdapter.notifyDataSetChanged();
        }
    }

    class ExperimentService implements ServiceConnection {
        IExperimentService service;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            service = IExperimentService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Service has unexpectedly disconnected");
            service = null;
        }
    }

    class AidlConnectionService implements ServiceConnection {
        ISensorService aidlService;
        ISensorCallback callback;
        String serviceName;
        String servicePackage;

        AidlConnectionService(String name, String pkg) {
            this.serviceName = name;
            this.servicePackage = pkg;

            this.callback = new ISensorCallback.Stub() {
                @Override
                public void handlePluginInfo(JsonMessage message) throws RemoteException {
                    if (message == null || message.getPayload().length() == 0) {
                        return;
                    }

                    try {
                        JSONArray arr = new JSONArray(message.getPayload());
                        Reading reading = Reading.fromJson(arr.get(0).toString());
                        AppModel.instance.readingStorage.pushReading(reading);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            aidlService = ISensorService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.e(TAG, "Service has unexpectedly disconnected");
            callback = null;
        }
    }
}
