package eu.organicity.set.app.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mixpanel.android.mpmetrics.MixpanelAPI;

import java.util.ArrayList;

import eu.organicity.set.app.R;
import eu.organicity.set.app.adapters.SensorsAdapter;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.Sensor;

/**
 * Created by chris on 27/06/2017.
 */

@SuppressLint("ValidFragment")
public class InstallExperimentDialog extends DialogFragment {

    private static final String EXPERIMENT = "EXPERIMENT";

    private MixpanelAPI mMixpanel;
    private Button installButton;
    private Experiment experiment;
    private AlertDialog dialog;
    private InstallExperimentInterface listener;
    private SensorsAdapter adapter;
    private int accentColor;


    public static DialogFragment newInstance(Experiment experiment) {
        InstallExperimentDialog d = new InstallExperimentDialog();

        Bundle bundle = new Bundle();
        bundle.putSerializable(EXPERIMENT, experiment);
        d.setArguments(bundle);

        return d;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final View view = inflater.inflate(R.layout.install_expirement_dialog, null);

        if (getArguments() != null) {
            experiment = (Experiment) getArguments().getSerializable(EXPERIMENT);
        }

        accentColor = ResourcesCompat.getColor(getResources(), R.color.accent, null);

        adapter = new SensorsAdapter(getContext());
        adapter.setItems(experiment.getSensors());
        RecyclerView list = (RecyclerView) view.findViewById(android.R.id.list);
        list.addItemDecoration(new DividerItemDecoration(getContext(), DividerItemDecoration.VERTICAL));
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.setAdapter(adapter);

        TextView experiment_name = (TextView) view.findViewById(R.id.experiment);
        experiment_name.setText(experiment.getName());

        installButton = (Button) view.findViewById(R.id.install);
        checkIfSensorsInstalled();

        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + experiment.getPkg()));
                getActivity().startActivity(intent);
            }
        });

        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
            }
        });

        builder.setView(view);

        dialog = builder.create();
        dialog.setCancelable(false);

        return dialog;
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        checkIfSensorsInstalled();
    }

    public void checkIfSensorsInstalled() {
        ArrayList<Sensor> installedSensors = new ArrayList<>();
        PackageManager packageManager = getContext().getPackageManager();
        for (Sensor s : experiment.getSensors()) {
            try {
                packageManager.getPackageInfo(s.getPkg(), 0);
                installedSensors.add(s);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (installedSensors.size() != experiment.getSensors().size()) {
            installButton.setEnabled(false);
            installButton.setTextColor(Color.GRAY);
        }
        else {
            boolean installed = false;
            try {
                packageManager.getPackageInfo(experiment.getPkg(), 0);
                installed = true;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (!installed) {
                installButton.setEnabled(true);
                installButton.setTextColor(accentColor);
            }
            else {
                if (listener != null) {
                    listener.finished();
                }

                dialog.dismiss();
            }
        }
    }

    public void addListener(InstallExperimentInterface listener) {
        this.listener = listener;
    }

    public interface InstallExperimentInterface {
        void finished();
    }
}
