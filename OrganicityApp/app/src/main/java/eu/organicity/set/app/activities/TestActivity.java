package eu.organicity.set.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import eu.organicity.set.app.R;
import eu.organicity.set.app.SimpleAdapter;
import eu.organicity.set.app.connections.ExperimentConnection;
import eu.organicity.set.app.connections.SensorConnection;
import eu.organicity.set.app.operations.Communication;
import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;
import eu.organicity.set.app.views.LogView;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.ReadingStorage;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;
import eu.smartsantander.androidExperimentation.util.Discoverable;

public class TestActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, ExperimentConnection.ExperimentCallbacks, SensorConnection.SensorCallback {

    public static final String ACTION_PICK_DEVELOPER_PLUGIN = "organicity.intent.action.PICK_DEVELOPER_PLUGIN";
    public static final String ACTION_PICK_DEVELOPER_EXPERIMENT = "organicity.intent.action.PICK_DEVELOPER_EXPERIMENT";

    static final String KEY_PKG = "pkg";
    static final String KEY_SERVICENAME = "servicename";
    static final String KEY_ACTIONS = "actions";
    static final String KEY_CATEGORIES = "categories";

    static final String TAG = "TestActivity";
    private static final long DELAY = 5000;
    private List<String> started;
    private Handler handler;
    private Runnable runnable;

    private LogView logView;

    private HashMap<String, SensorConnection> connections = new HashMap<>();
    private ExperimentConnection experimentConnection;

    private HashSet<String> connected;
    private HashMap<String, List<JSONObject>> sensorsResults;
    private ReadingStorage readingStorage;

    private ListView list;
    private Button run, stop, clear;

    private PackageBroadcastReceiver packageBroadcastReceiver;
    private IntentFilter packageFilter;
    private ArrayList<HashMap<String, String>> services;
    private ArrayList<String> categories;
    private SimpleAdapter itemAdapter;
    private Experiment experiment;
    private ArrayList<Sensor> runningSensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer);

        readingStorage = new ReadingStorage();
        connected = new HashSet<>();

        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                for (String key : started) {
                    ISensorService service = connections.get(key).service;
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

                    if (readingStorage.getReadingsCount() == 0) {
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
                            experimentConnection.service.getExperimentResult(readingStorage.getBundle(), result);

                            try {
                                JSONArray array = new JSONArray(result.getPayload());
                                final JSONObject res = new JSONObject((String) array.getJSONObject(0).get("value"));

                                Log.d(TAG, "Result: " + res.toString());
                                TestActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logView.appendToLog(res.toString());
                                    }
                                });

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

        sensorsResults = new HashMap<>();
        started = new ArrayList<>();
        connections = new HashMap<>();

        logView = (LogView) findViewById(R.id.logview);
        logView.setMovementMethod(new ScrollingMovementMethod());

        list = (ListView) findViewById(android.R.id.list);
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);
        list.setOnItemClickListener(this);

        itemAdapter = new SimpleAdapter(this, R.layout.plugin_row);
        list.setAdapter(itemAdapter);

        fillPluginList();;

        run = (Button) findViewById(R.id.run);
        run.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<Discoverable> checkedItems = itemAdapter.getCheckedItems();

                runningSensors = new ArrayList<>();

                for (Discoverable plugin : checkedItems) {
                    if (plugin.getType().equals("sensor")) {
                        Sensor s = (Sensor) plugin;
                        runningSensors.add(s);
                    }
                    else if (plugin.getType().equals("experiment")) {
                        experiment = (Experiment) plugin;
                    }
                }

                if (runningSensors.size() > 0) {
                    setupSensorServices(runningSensors);
                }
            }
        });

        stop = (Button) findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (runningSensors != null && runningSensors.size() > 0) {
                    stopSensors(runningSensors);
                }

                if (experiment != null) {
                    stopExperiment(experiment);
                }
            }
        });

        clear = (Button) findViewById(R.id.clear);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TestActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        logView.setText("");
                    }
                });
            }
        });

        packageBroadcastReceiver = new PackageBroadcastReceiver();
        packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addCategory(Intent.CATEGORY_DEFAULT);
        packageFilter.addDataScheme("package");

        handler.post(runnable);
    }

    private void setupSensorServices(List<Sensor> sensors) {
        for (Sensor s : sensors) {
            SensorConnection connection = new SensorConnection(s.getService(), s.getPkg(), this);

            connections.put(s.getKey(), connection);
        }

        startSensors(sensors);
    }

    private void stopSensors(ArrayList<Sensor> sensors) {
        for (Sensor sensor : sensors) {
            if (sensor.getType().equals("sensor")) {
                Log.d(TAG, "Stopping sensor: " + sensor.getName() + " in package: " + sensor.getPkg());
                stopSensor(sensor);
            }
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

        if (installed && started.contains(key)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            unbindService(connections.get(key));
            connections.get(key).service =  null;
            boolean result = stopService(serviceIntent);
            Log.d(TAG, "Stopping service " + key + " with result " + result);
            started.remove(key);
        }
    }

    private void startSensors(List<Sensor> sensors) {
        for (Sensor sensor : sensors) {
            if (sensor instanceof Sensor) {
                Log.d(TAG, "Starting sensor: " + sensor.getName() + " in package: " + sensor.getPkg());
                startSensor(sensor);
            }
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


        if (installed && !started.contains(key)) {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            bindService(serviceIntent, connections.get(key), Context.BIND_AUTO_CREATE);
            startService(serviceIntent);
            started.add(key);
        }
    }

    private void startExperiment(Experiment experiment) {
        String pkg = experiment.getPkg();
        String key = experiment.getKey();

        experimentConnection = new ExperimentConnection(this);
        Intent serviceIntent = new Intent();
        serviceIntent.setClassName(pkg, key);

        Log.d(TAG, "Starting experiment service: " + experiment.getName() + " in package: " + experiment.getPkg());
        bindService(serviceIntent, experimentConnection, Context.BIND_AUTO_CREATE);
        startService(serviceIntent);
    }

    private void stopExperiment(Experiment experiment) {
//        stopSensors(experiment);

        if (experimentConnection != null && experimentConnection.service != null) {
            Log.d(TAG, "Stopping experiment service: " + experiment.getName() + " in package: " + experiment.getPkg());

            String pkg = experiment.getPkg();
            String key = pkg + "." + experiment.getService();

            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(pkg, key);

            unbindService(experimentConnection);
            stopService(serviceIntent);

            handler.removeCallbacks(runnable);

            Toast.makeText(getApplicationContext(), "Experiment stopped", Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "Cannot stop null experiment!");
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

        Discoverable sensor = itemAdapter.getItem(position);

        boolean installed = false;
        PackageManager packageManager = getPackageManager();
        try {
            packageManager.getPackageInfo(sensor.getPkg(), 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!installed) {
            Toast.makeText(getApplicationContext(), "Package not found. Try reinstalling.", Toast.LENGTH_SHORT).show();
        }
        else {
            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(sensor.getPkg(), sensor.getService());

            if (started.contains(sensor.getKey())) {
                unbindService(connections.get(sensor.getKey()));
                stopService(serviceIntent);
                started.remove(sensor.getKey());
            }
            else {
                bindService(serviceIntent, connections.get(sensor.getKey()), Context.BIND_AUTO_CREATE);
                startService(serviceIntent);
                started.add(sensor.getKey());
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
        services = new ArrayList<>();
        categories = new ArrayList<>();

        final ArrayList<Discoverable> discoverables = new ArrayList<>();

        new AsyncTask<Object, Object, List<Sensor>>() {

            @Override
            protected List<Sensor> doInBackground(Object... voids) {
                try {
                    return Communication.getInstance().getSensors();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Sensor> result) {
                super.onPostExecute(result);
                discoverables.addAll(result);
                itemAdapter.setItems(discoverables);
                itemAdapter.notifyDataSetChanged();
            }
        }.execute();

        connections.clear();

        PackageManager packageManager = getPackageManager();

        Intent sensorsIntent = new Intent(ACTION_PICK_DEVELOPER_PLUGIN);
        sensorsIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);

        Intent experimentIntent = new Intent(ACTION_PICK_DEVELOPER_EXPERIMENT);
        experimentIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);

        List<ResolveInfo> resList = packageManager.queryIntentServices(sensorsIntent,
                PackageManager.GET_RESOLVED_FILTER);

        List<ResolveInfo> expResList = packageManager.queryIntentServices(experimentIntent,
                PackageManager.GET_RESOLVED_FILTER);

        for (int i = 0; i < resList.size(); ++i) {
            ResolveInfo info = resList.get(i);
            ServiceInfo sinfo = info.serviceInfo;

            if (sinfo != null) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(KEY_PKG, sinfo.packageName);
                item.put(KEY_SERVICENAME, sinfo.name);

                services.add(item);

                Sensor s = new Sensor(sinfo.name, sinfo.packageName, sinfo.name);
                s.setKey(sinfo.name);

                discoverables.add(s);
            }
        }

        for (int i = 0; i < expResList.size(); ++i) {
            ResolveInfo info = expResList.get(i);
            ServiceInfo sinfo = info.serviceInfo;

            if (sinfo != null) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put(KEY_PKG, sinfo.packageName);
                item.put(KEY_SERVICENAME, sinfo.name);

                services.add(item);

                Experiment exp = new Experiment();
                exp.setName(sinfo.name);
                exp.setPkg(sinfo.packageName);
                exp.setService(sinfo.name);
                exp.setKey(sinfo.name);

                discoverables.add(exp);
            }
        }

        itemAdapter.setItems(discoverables);
        itemAdapter.notifyDataSetChanged();


        Log.d(TAG, "services: " + services);
        Log.d(TAG, "categories: " + categories);
    }

    @Override
    public void experimentStarted() {

    }

    @Override
    public void experimentStopped() {

    }

    @Override
    public void sensorConnected(String service) {
        Log.d(TAG, "Sensor " + service + " connected");
        connected.add(service);

        if (experiment == null) {
            Log.d(TAG, "Experiment is null");
            return;
        }

        boolean start = true;
        for (Sensor s : runningSensors) {
            if (!connected.contains(s.getService())) {
                start = false;
            }
        }

        if (start) {
            startExperiment(experiment);
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
            final Reading reading = Reading.fromJson(arr.get(0).toString());
            readingStorage.pushReading(reading);

            TestActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logView.appendToLog(reading.toJson());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    class PackageBroadcastReceiver extends BroadcastReceiver {
        private static final String LOG_TAG = "PackageBroadcastReceive";

        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "onReceive: " + intent);
            services.clear();
            fillPluginList();
            itemAdapter.notifyDataSetChanged();
        }
    }
}
