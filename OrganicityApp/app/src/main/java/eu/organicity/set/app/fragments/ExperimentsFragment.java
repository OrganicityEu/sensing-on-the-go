package eu.organicity.set.app.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.app.R;
import eu.organicity.set.app.adapters.ExperimentsCardAdapter;
import eu.organicity.set.app.operations.Communication;
import eu.organicity.set.app.utils.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

import static eu.smartsantander.androidExperimentation.util.Constants.EXPERIMENT;
import static eu.smartsantander.androidExperimentation.util.Constants.EXPERIMENT_STATE_INTENT;
import static eu.smartsantander.androidExperimentation.util.Constants.STATE;

/**
 * Created by chris on 06/07/2017.
 */

public class ExperimentsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "ExperimentsFragment";

    private RecyclerView experementsList;
    private ExperimentsCardAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    public boolean selfEvent;
    private static final String experimentsURL = "https://api.myjson.com/bins/102ve3";

    private Sensor[] sensors = {
            new Sensor("Location sensor", "eu.organicity.set.sensors.location", "LocationSensorService"),
            new Sensor("WiFi sensor", "eu.organicity.set.sensors.wifi", "WifiSensorService"),
            new Sensor("Ble Reader sensor", "eu.organicity.set.sensors.ble", "BLESensorService"),
            new Sensor("Temperature sensor", "eu.organicity.set.sensors.temperature", "TemperatureSensorService"),
            new Sensor("Noise sensor", "eu.organicity.set.sensors.noise", "NoiseSensorService"),

    };

    private BroadcastReceiver experimentStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(EXPERIMENT) && intent.hasExtra(STATE)) {
                Experiment experiment = (Experiment) intent.getSerializableExtra(EXPERIMENT);
                Experiment.State state = (Experiment.State) intent.getSerializableExtra(STATE);

                adapter.updateExperimentState(experiment, state);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.expirements_fragment, container, false);

//        adapter = new ExperimentsAdapter(getContext());
        adapter = new ExperimentsCardAdapter(this);

        experementsList = (RecyclerView) v.findViewById(android.R.id.list);
        experementsList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        experementsList.setAdapter(adapter);

        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this);

        onRefresh();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        onRefresh();
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(experimentStateReceiver, new IntentFilter(EXPERIMENT_STATE_INTENT));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(experimentStateReceiver);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.UNINSTALL_CODE) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onRefresh() {
        new AsyncTask<Object, Object, List<Experiment>>() {

            @Override
            protected List<Experiment> doInBackground(Object... voids) {
                try {
                    return Communication.getInstance().getExperiments();
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<Experiment> experiments) {
                super.onPostExecute(experiments);

//                //TODO REMOVE THIS
//                experiments.add(dummyExperiment());

                if (experiments != null) {
                    adapter.setItems(experiments);
                    adapter.notifyDataSetChanged();
                }

                //stop refreshing
                swipeRefresh.setRefreshing(false);
            }
        }.execute();

    }

    private Experiment dummyExperiment() {
        Experiment experiment = new Experiment();
        experiment.setName("Noise Level Experiment");
        experiment.setPkg("eu.organicity.set.experiments.noiselevelexperiment");
        experiment.setService("NoiseLevelExperiment");
        experiment.setKey(experiment.getPkg() + "." + experiment.getService());

        List<Sensor> sen = new ArrayList<>();
        sen.add(sensors[0]);
        sen.add(sensors[4]);
        experiment.setSensors(sen);

        return experiment;
    }

}
