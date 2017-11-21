package eu.organicity.set.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import eu.organicity.set.app.R;
import eu.organicity.set.app.adapters.ExperimentsCardAdapter;
import eu.organicity.set.app.adapters.SensorsDetailsAdapter;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

public class ExperimentDetailsActivity extends AppCompatActivity {

    private Toolbar toolbar;

    BroadcastReceiver killReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finishAffinity();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_experiment_details);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Details");
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        Experiment experiment = (Experiment) bundle.getSerializable(ExperimentsCardAdapter.EXPERIMENT);

        TextView title = (TextView) findViewById(R.id.title);
        title.setText(experiment.getName());

        TextView description = (TextView) findViewById(R.id.description);
        description.setText(experiment.getDescription());

        Date date = new Date(experiment.getTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

        TextView startDate = (TextView) findViewById(R.id.start_date);
        startDate.setText("Added: " + sdf.format(date));

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        SensorsDetailsAdapter adapter = new SensorsDetailsAdapter(experiment.getSensors());
        list.setAdapter(adapter);


        IntentFilter intentFilter = new IntentFilter("kill-organicity");
        LocalBroadcastManager.getInstance(this).registerReceiver(killReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(killReceiver);
    }

}
