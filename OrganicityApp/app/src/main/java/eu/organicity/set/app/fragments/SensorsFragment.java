package eu.organicity.set.app.fragments;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import eu.organicity.set.app.R;
import eu.organicity.set.app.adapters.SensorsAdapter;
import eu.organicity.set.app.operations.Communication;
import eu.organicity.set.app.utils.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

/**
 * Created by chris on 06/07/2017.
 */

public class SensorsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private static final String TAG = "SensorsFragment";
    private static final String SENSORS = "sensors";
    private RecyclerView sensorsList;
    private SensorsAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private static final String sensorsUrl = "https://api.myjson.com/bins/e7627";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.expirements_fragment, container, false);

        adapter = new SensorsAdapter(getActivity(), true);

        sensorsList = (RecyclerView) v.findViewById(android.R.id.list);
        sensorsList.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        sensorsList.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        sensorsList.setAdapter(adapter);

        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this);

        onRefresh();

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        onRefresh();
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
            protected void onPostExecute(List<Sensor> sensors) {
                super.onPostExecute(sensors);
                adapter.setItems(sensors);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }
}
