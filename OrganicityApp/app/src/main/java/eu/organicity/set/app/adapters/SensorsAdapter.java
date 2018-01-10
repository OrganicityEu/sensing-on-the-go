package eu.organicity.set.app.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import eu.organicity.set.app.R;
import eu.organicity.set.app.utils.Constants;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

/**
 * Created by chris on 06/07/2017.
 */

public class SensorsAdapter extends RecyclerView.Adapter<SensorsAdapter.SensorViewHolder> {

    public static final String TAG = "SensorsAdapter";
    public static final String LAST_EXPERIMENT = "last_experiment";
    private boolean shouldShowUninstall;
    private List<Sensor> items;
    private int sensorColor;
    private Context fragment;

    private PackageManager packageManager;

    public SensorsAdapter(Activity context, boolean shouldShowUninstall) {
        this.shouldShowUninstall = shouldShowUninstall;
        setup(context);
    }

    public SensorsAdapter(Context context) {
        this.shouldShowUninstall = false;
        setup(context);
    }

    private void setup(Context context) {
        items = new ArrayList<>();
        sensorColor = ResourcesCompat.getColor(context.getResources(), R.color.accent, null);
        fragment = context;

        packageManager = context.getPackageManager();
    }

    @Override
    public SensorViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (this.shouldShowUninstall) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_fragment_row, parent, false);
        }
        else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.sensor_row, parent, false);
        }

        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SensorViewHolder holder, final int position) {
        final Sensor sensor = items.get(position);

        holder.title.setText(sensor.getName());

        boolean installed = false;

        try {
            PackageInfo pkInfo = packageManager.getPackageInfo(sensor.getPkg(), 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Sensor: " + sensor.getPkg() + " not installed");
        }

        if (installed) {
            if (shouldShowUninstall) {
                holder.install.setVisibility(View.VISIBLE);
                holder.install.setText("Uninstall");
            }
            else {
                holder.install.setVisibility(View.GONE);
            }
        }
        else {
            holder.install.setText("Install");
            holder.install.setVisibility(View.VISIBLE);
        }

        final String tag = holder.install.getText().toString().toLowerCase();
        holder.install.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (tag.equals("install")) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse("market://details?id=" + sensor.getPkg()));
                    fragment.startActivity(intent);
                }
                else {
                    Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + sensor.getPkg()));
                    ((Activity) fragment).startActivityForResult(intent, Constants.UNINSTALL_CODE);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setItems(List<Sensor> items) {
        this.items = items;
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public Button install;

        public SensorViewHolder(View itemView) {
            super(itemView);

            title = (TextView) itemView.findViewById(R.id.title);
            install = (Button) itemView.findViewById(R.id.install);
        }
    }

}
