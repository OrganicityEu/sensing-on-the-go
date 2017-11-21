package eu.organicity.set.app.adapters;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.R;
import eu.organicity.set.app.activities.ExperimentDetailsActivity;
import eu.organicity.set.app.fragments.ExperimentsFragment;
import eu.organicity.set.app.fragments.InstallExperimentDialog;
import eu.organicity.set.app.services.SchedulerService;
import eu.organicity.set.app.utils.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

import static eu.organicity.set.app.services.SchedulerService.STOP;

/**
 * Created by chris on 06/07/2017.
 */

public class ExperimentsCardAdapter extends RecyclerView.Adapter<ExperimentsCardAdapter.ExperimentViewHolder> {

    public static final String LAST_EXPERIMENT = "last_experiment";
    public static final String EXPERIMENT = "EXPERIMENT";

    private List<Experiment> items;
    private int sensorColor;
    private SimpleDateFormat sdf;
    private ExperimentsFragment fragment;
    private PackageManager packageManager;
    private Experiment runningExperiment;

    InstallExperimentDialog.InstallExperimentInterface listener;

    public ExperimentsCardAdapter(ExperimentsFragment context) {
        items = new ArrayList<>();

        sensorColor = ResourcesCompat.getColor(context.getResources(), R.color.accent, null);
        sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

        fragment = context;
        packageManager = context.getActivity().getPackageManager();
    }

    @Override
    public ExperimentViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.experiment_card_row, parent, false);

        return new ExperimentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ExperimentViewHolder holder, final int position) {
        final Experiment experiment = items.get(position);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("Sensors: ");

        int start;
        int end;
        String s;

        for (Sensor sensor : experiment.getSensors()) {
            s = sensor.getName();

            start = builder.length();
            end = start + s.length();
            builder.append(s);
            builder.setSpan(new ForegroundColorSpan(sensorColor), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);

            start = builder.length();
            builder.append(", ");
            end = builder.length();
            builder.setSpan(new ForegroundColorSpan(Color.BLACK), start, end, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        // remove last " ,"
        builder.delete(builder.length() - 2, builder.length());

        holder.title.setText(experiment.getName());
        holder.sensors.setText(builder);

        Date date = new Date(experiment.getTimestamp());
        holder.added.setText("Added: " + sdf.format(date));

        boolean sensorsInstalled = true;
        for (Sensor sensor : experiment.getSensors()) {
            try {
                packageManager.getPackageInfo(sensor.getPkg(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                sensorsInstalled = false;
            }
        }

        boolean installed = false;
        // check if experiment is installed and update buttons state and text
        try {
            packageManager.getPackageInfo(experiment.getPkg(), 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
        }

        boolean running = false;
        if (AppModel.instance.experiment != null && AppModel.instance.experiment.getId().equals(experiment.getId())) {
            runningExperiment = AppModel.instance.experiment;
            runningExperiment.setState(experiment.getState());
            running = true;
        }

        if (running) {
            holder.start.setEnabled(true);
            holder.start.setTextColor(sensorColor);

            if (runningExperiment.getState() == Experiment.State.RUNNING) {
                holder.start.setText("Stop");
            }
            else {
                holder.start.setText("Start");
            }
        }
        else if (installed) {
            holder.start.setEnabled(true);
            holder.start.setTextColor(sensorColor);
        }

        if (sensorsInstalled && installed) {
            holder.install.setText("Uninstall");
            holder.install.setTag("uninstall");

            holder.start.setEnabled(true);
            holder.start.setTextColor(sensorColor);
        }
        else {
            holder.install.setText("Install");
            holder.install.setTag("install");

            holder.start.setEnabled(false);
            holder.start.setTextColor(Color.GRAY);
        }

        final String tag = (String) holder.install.getTag();
        holder.install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tag.equals("install")) {
                    InstallExperimentDialog dialog = (InstallExperimentDialog) InstallExperimentDialog.newInstance(experiment);
                    dialog.addListener(listener);

                    if (fragment != null) {
                        dialog.show(fragment.getActivity().getSupportFragmentManager(), "installSensorsDialog");
                    }
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + experiment.getPkg()));
                    fragment.startActivityForResult(intent, Constants.UNINSTALL_CODE);
                }
            }
        });

        holder.details.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(fragment.getActivity(), ExperimentDetailsActivity.class);
                intent.putExtra(EXPERIMENT, experiment);
                fragment.startActivity(intent);
            }
        });

        final String text = holder.start.getText().toString().toLowerCase();
        holder.start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(fragment.getContext(), SchedulerService.class);
                intent.putExtra(EXPERIMENT, experiment);

                if (text.equals("start")) {
                    fragment.getActivity().startService(intent);
                }
                else {
                    intent.putExtra(STOP, true);
                    fragment.getActivity().startService(intent);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Experiment> items) {
        this.items = items;
    }

    public void updateExperimentState(Experiment experiment, Experiment.State state) {
        for (Experiment exp : items) {
            if (exp.getKey().equals(experiment.getKey())) {
                exp.setState(state);
                runningExperiment = exp;
                notifyDataSetChanged();
                break;
            }
        }
    }

    static class ExperimentViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public TextView sensors;
        public TextView added;
        public TextView install;
        public Button details;
        public Button start;

        public ExperimentViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.ex1_title);
            sensors = (TextView) itemView.findViewById(R.id.ex1_plugins);
            added = (TextView) itemView.findViewById(R.id.ex1_start_date);
            install = (TextView) itemView.findViewById(R.id.download);
            details = (Button) itemView.findViewById(R.id.details);
            start = (Button) itemView.findViewById(R.id.record);
        }
    }

}
