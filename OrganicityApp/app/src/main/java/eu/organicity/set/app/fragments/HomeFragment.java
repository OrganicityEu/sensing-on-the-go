package eu.organicity.set.app.fragments;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.mixpanel.android.mpmetrics.MixpanelAPI;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.BuildConfig;
import eu.organicity.set.app.R;
import eu.organicity.set.app.operations.AsyncReportOnServerTask;
import eu.organicity.set.app.operations.AsyncStatusRefreshTask;
import eu.organicity.set.app.operations.Communication;
import eu.organicity.set.app.services.SchedulerService;
import eu.organicity.set.app.utils.PermissionsUtil;
import eu.smartsantander.androidExperimentation.ActivityRecognitionService;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;
import eu.smartsantander.androidExperimentation.jsonEntities.OrganicityProfile;
import gr.cti.android.experimentation.model.RankingEntry;
import gr.cti.android.experimentation.model.RegionDTO;
import gr.cti.android.experimentation.model.RegionListDTO;
import gr.cti.android.experimentation.model.SmartphoneStatisticsDTO;
import gr.cti.android.experimentation.model.UsageEntry;

import static eu.organicity.set.app.services.SchedulerService.STOP;
import static eu.smartsantander.androidExperimentation.util.Constants.EXPERIMENT;
import static eu.smartsantander.androidExperimentation.util.Constants.EXPERIMENT_STATE_INTENT;
import static eu.smartsantander.androidExperimentation.util.Constants.RESULT_ACTION;
import static eu.smartsantander.androidExperimentation.util.Constants.STATE;

/**
 * Created by chris on 06/07/2017.
 */

public class HomeFragment extends Fragment implements OnMapReadyCallback, LocationListener, AsyncStatusRefreshTask.RefreshTaskInterface {

    private static final String TAG = "HomeFragment";

    private final BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive " + action);
            switch (action) {
                case RESULT_ACTION:
                    updatePending();
                    double lat = intent.getDoubleExtra("lat", 0);
                    double lon = intent.getDoubleExtra("lon", 0);
                    if (lat != 0 && lon != 0) {
                        updateMapLocation(lat, lon);
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver experimentStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra(EXPERIMENT) && intent.hasExtra(STATE)) {
                Experiment experiment = (Experiment) intent.getSerializableExtra(EXPERIMENT);
                Experiment.State state = (Experiment.State) intent.getSerializableExtra(STATE);

                if (lastExperimentButton.getVisibility() == View.VISIBLE) {
                    if (state == Experiment.State.RUNNING) {
                        lastExperimentButton.setText("Stop");
                    }
                    else {
                        lastExperimentButton.setText("Start");
                    }
                }
            }
        }
    };

    private final static int DURATION = 2000;

    //Private data
    private static boolean experimentationStatus = true;
    private static boolean registered = false;
    public static Location location = null;

    private Button downloadButton;

    //Organicity
    public TextView expDescriptionTv;
    public TextView pendingMeasurements;
    private MixpanelAPI mMixpanel;

    private View pendingLayout;
    private Button sendPendingNow;

    private Button lastExperimentButton;

    //    public ArrayList<SensorMeasurement> sensorMeasurements;
    //    public SensorMeasurementAdapter sensorMeasurementAdapter;
    Set<LatLng> points;
    Deque<LatLng> measurementPoints;

    private PolylineOptions line = null;
    private PendingIntent activityRecognitionPendingIntent;
    private RegionListDTO regions;

    private LocationManager locationManager;
    private MapView mapView;

    private Experiment lastExperiment;

    private BarChart mBarChart;
    private HeatmapTileProvider mProvider;
    private List<LatLng> heatMapItems;

    private OrganicityProfile profile;
    private int phoneId = -1;
    private long lastUpdate = 0;
    private boolean hadLastExp = false;

    private TextView experimentsTodayTextView;
    private TextView readingsTodayTextView;
    private TextView experimentsAllTextView;
    private TextView readingsAllTextView;

    private Handler statsHandler;
    private Runnable statsRunnable;

    public static Context context;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.home_fragment, container, false);

        context = getContext();
        points = new HashSet<>();
        measurementPoints = new ArrayDeque<>();
        mMixpanel = MixpanelAPI.getInstance(getContext(), BuildConfig.SET_MIXPANEL_TOKEN);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

        IntentFilter filter = new IntentFilter();
        filter.addAction(RESULT_ACTION);
        if (getActivity() != null) {
            getActivity().registerReceiver(actionReceiver, filter);
        }

        mapView = (MapView) v.findViewById(R.id.map_main);
        mapView.onCreate(savedInstanceState);
//        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)) {
        mapView.getMapAsync(this);
//        }

        //Disable for now
        final Intent activityRecognitionIntent = new Intent(getContext(), ActivityRecognitionService.class);
        activityRecognitionPendingIntent = PendingIntent.getService(getActivity().getApplicationContext(), 0, activityRecognitionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        pendingLayout = v.findViewById(R.id.pendingLayout);
        sendPendingNow = (Button) v.findViewById(R.id.sendPendingNow);
        sendPendingNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncReportOnServerTask().execute();

                try {
                    final JSONObject props = new JSONObject();
                    props.put("count", AppModel.storage.size());
                    //mMixpanel.track("send-stored-readings", props);
                } catch (JSONException ignore) {}
            }
        });

        expDescriptionTv = (TextView) v.findViewById(R.id.experiment_description);
        lastExperimentButton = (Button) v.findViewById(R.id.last_experiment_button);
        lastExperimentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getContext(), SchedulerService.class);
                intent.putExtra(EXPERIMENT, AppModel.instance.experiment);

                if (lastExperimentButton.getText().toString().toLowerCase().equals("stop")) {
                    intent.putExtra(STOP, true);
                    getActivity().startService(intent);
                    lastExperimentButton.setText("Start");
                }
                else {
                    intent.putExtra(EXPERIMENT, AppModel.instance.experiment);
                    getActivity().startService(intent);
                    lastExperimentButton.setText("Stop");
                }
            }
        });

        pendingMeasurements = (TextView) v.findViewById(R.id.experiment_pending);


        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            refresh();
        }

        mBarChart = (BarChart) v.findViewById(R.id.barchart);
        heatMapItems = new ArrayList<>();
        heatMapItems.add(new LatLng(0, 0));

        downloadButton = (Button) v.findViewById(R.id.downloadDataExperiment);
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (AppModel.instance.experiment != null) {
                                downloadFile(downloadButton, AppModel.instance.experiment.getId(), AppModel.instance.phoneProfiler.getPhoneId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.getMessage(), e);
                        }
                    }
                }).start();
            }
        });

        final TextView title = (TextView) v.findViewById(R.id.heatmaptitle);
        final View map = v.findViewById(R.id.map_main);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (heatMapItems.size() == 1) {
                    title.setVisibility(View.GONE);
//                    map.setVisibility(View.GONE);
                } else {
                    title.setVisibility(View.VISIBLE);
//                    map.setVisibility(View.VISIBLE);
                }
            }
        });
        mProvider = new HeatmapTileProvider.Builder().data(heatMapItems).build();

        experimentsTodayTextView = (TextView) v.findViewById(R.id.stats_number_exp_value);
        readingsTodayTextView = (TextView) v.findViewById(R.id.stats_number_readings_value);
        experimentsAllTextView = (TextView) v.findViewById(R.id.stats_number_exp_value_a);
        readingsAllTextView = (TextView) v.findViewById(R.id.stats_number_readings_value_a);

        statsHandler = new Handler();
        statsRunnable = new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        fillStatsFields();
                    }
                }).start();

                statsHandler.postDelayed(this, DURATION);
            }
        };

        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(experimentStateReceiver, new IntentFilter(EXPERIMENT_STATE_INTENT));

        if (!AppModel.instance.isDeviceRegistered()) {
            AppModel.instance.phoneProfiler.register();
        }

        if (AppModel.instance.experiment != null) {
            statsHandler.removeCallbacks(statsRunnable);
            statsHandler.post(statsRunnable);
        }

        updateLastExperiment();
    }

    private void updateLastExperiment() {
        if (AppModel.instance.experiment != null) {
            expDescriptionTv.setText("Last experiment: " + AppModel.instance.experiment.getName());

            if (AppModel.instance.experimentConnection != null && AppModel.instance.experimentConnection.service != null) {
                lastExperimentButton.setText("Stop");
            }
            else {
                lastExperimentButton.setText("Start");
            }

            lastExperimentButton.setVisibility(View.VISIBLE);
        }
        else {
            expDescriptionTv.setText(R.string.home_hint);
            lastExperimentButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();

        statsHandler.removeCallbacks(statsRunnable);

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(experimentStateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (getActivity() != null) {
            getActivity().unregisterReceiver(actionReceiver);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (PermissionsUtil.checkLocationPermissions(getActivity())) {
            //noinspection MissingPermission
            googleMap.setMyLocationEnabled(true);

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;

            }
            Location location;
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location == null) {
                    return;
                }
            }

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

            CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(latLng, 13);
            googleMap.animateCamera(yourLocation);
        }
        googleMap.getUiSettings().setAllGesturesEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
    }

    private void updatePending() {
        final Long storageSize = AppModel.storage.size();
        if (storageSize > 0) {
            pendingLayout.setVisibility(View.VISIBLE);
            sendPendingNow.setText(getString(R.string.pending_messages_template, storageSize));
            pendingMeasurements.setText(String.format("%d pending measurements", storageSize));
        } else {
            pendingMeasurements.setText("no pending measurements");
            pendingLayout.setVisibility(View.GONE);
        }
    }

    private void refresh() {
        final Experiment experiment = AppModel.instance.experiment;

        try {
            if (regions == null && experiment != null) {

                Log.i(TAG, "Loading Regions....");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        regions = new Communication().getExperimentRegions(experiment.getId());
                        if (regions != null) {
                            for (final RegionDTO region : regions.getRegions()) {

                                try {
                                    Log.i(TAG, "Regions[" + region.getId() + "]  ....");
                                    final JSONArray array = new JSONArray(region.getCoordinates());
                                    final PolygonOptions rectOptions = new PolygonOptions();
                                    final List<LatLng> points = new ArrayList<>();

                                    for (int i = 0; i < array.length(); i++) {
                                        final JSONArray elem = (JSONArray) array.get(i);
                                        points.add(new LatLng(elem.getDouble(1), elem.getDouble(0)));
                                    }

                                    rectOptions.strokeColor(R.color.organicityPink);
                                    rectOptions.fillColor(R.color.organicityPink);
                                    rectOptions.addAll(points);

                                    getActivity().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            // Get back the mutable Polygon
                                            mapView.getMapAsync(new OnMapReadyCallback() {
                                                @Override
                                                public void onMapReady(GoogleMap googleMap) {
                                                    Polygon polygon = googleMap.addPolygon(rectOptions);
                                                }
                                            });

                                        }
                                    });
                                } catch (Exception e) {
                                    Log.e(TAG, e.getMessage(), e);
                                }
                            }
                        }
                    }
                }).start();
            }
        } catch (Exception ignore) {
        }

        final AsyncStatusRefreshTask task = new AsyncStatusRefreshTask(this);
        task.execute();
    }

    private void updateMapLocation(final double latitude, final double longitude) {
        // Creating a LatLng object for the current location
        final LatLng latLng = new LatLng(latitude, longitude);
        try {
            // Showing the current location in Google Map
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if (measurementPoints.size() > 30) {
                        measurementPoints.poll();
                    }
                    measurementPoints.push(latLng);
                    googleMap.clear();
                    for (LatLng measurementPoint : measurementPoints) {
                        googleMap.addMarker(new MarkerOptions().position(measurementPoint));
                    }
                }
            });
        } catch (NullPointerException ignore) {
        }
    }

    public static void changeStatus(boolean status) {
        experimentationStatus = status;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, String.format("onLocationChanged: [%f,%f]", location.getLatitude(), location.getLongitude()));
        this.location = location;
        final LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
//                googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                CameraUpdate yourLocation = CameraUpdateFactory.newLatLngZoom(latLng, 5);
                googleMap.animateCamera(yourLocation);
            }
        });
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    private void updateBarChart(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics == null) {
            return;
        }

        final SortedMap<Long, Long> sortedMap = new TreeMap<>(new Comparator<Long>() {
            @Override
            public int compare(Long lhs, Long rhs) {
                return (int) (rhs - lhs);
            }
        });
        if (smartphoneStatistics.getLast7Days() != null) {
            sortedMap.putAll(smartphoneStatistics.getLast7Days());
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBarChart.setVisibility(View.VISIBLE);
                    mBarChart.clearChart();
                    for (Long integer : sortedMap.keySet()) {
                        mBarChart.addBar(new BarModel(integer.toString(), sortedMap.get(integer).floatValue(), 0xFF1FF4AC));
                    }
//                    mBarChart.startAnimation();
                }
            });
        } else {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mBarChart.setVisibility(View.GONE);
                }
            });
        }
    }

    public void updateFields(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics == null) {
            return;
        }

        try {
            updateBarChart(smartphoneStatistics);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        try {
            //updateExperimentDeviceHeatMap();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }

        //TODO change this

//        DynamixService.mixpanelRecord("experiments", smartphoneStatistics.getExperiments());
//        DynamixService.mixpanelRecord("readings", smartphoneStatistics.getReadings());
//        DynamixService.mixpanelRecord("badges", smartphoneStatistics.getBadges().size());

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                experimentsTodayTextView.setText("");
                readingsTodayTextView.setText(Long.toString(smartphoneStatistics.getExperimentReadings()));
                experimentsAllTextView.setText(Long.toString(smartphoneStatistics.getExperiments()));
                readingsAllTextView.setText(Long.toString(smartphoneStatistics.getReadings()));
            }
        });

        //-------------------------------------//

        updateExperimentRankings(smartphoneStatistics);
        updateRankings(smartphoneStatistics);

        updateExperimentBadges(smartphoneStatistics);
        updateBadges(smartphoneStatistics);
        updateExperimentUsage(smartphoneStatistics);
        updateUsage(smartphoneStatistics);
    }

    private void updateUsage(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getUsage() == null) {
            return;
        }
        try {
            long total = 0;
            for (final UsageEntry entry : smartphoneStatistics.getUsage()) {
                total += entry.getTime();
            }
            final long totalF = total;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView badgesTextView = (TextView) getView().findViewById(R.id.stats_time_value_a);
                    badgesTextView.setText(String.format("%d hours", totalF / 60));
                }
            });
        } catch (NullPointerException ignore) {
        }
    }

    private void updateExperimentUsage(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getExperimentUsage() == null) {
            return;
        }
        try {
            long total = 0;
            for (final UsageEntry entry : smartphoneStatistics.getExperimentUsage()) {
                total += entry.getTime();
            }
            final long totalF = total;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView badgesTextView = (TextView) getView().findViewById(R.id.stats_time_value);
                    badgesTextView.setText(String.format("%d hours", totalF / 60));
                }
            });

        } catch (NullPointerException ignore) {

        }
    }

    private void updateBadges(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getBadges() == null) {
            return;
        }
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView badgesTextView = (TextView) getView().findViewById(R.id.badgesMessage);
                    badgesTextView.setText(String.format("%d badges earned", smartphoneStatistics.getBadges().size()));
                }
            });
        } catch (NullPointerException ignore) {
        }
    }

    private void updateExperimentBadges(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getExperimentBadges() == null) {
            return;
        }
        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final TextView experimnetBadgesTextView = (TextView) getView().findViewById(R.id.badgesExperimentMessage);
                    experimnetBadgesTextView.setText(String.format("%d badges earned", smartphoneStatistics.getExperimentBadges().size()));
                }
            });
        } catch (NullPointerException ignore) {

        }
    }

    public void updateRankings(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getRankings() == null) {
            return;
        }

        try {
            final SortedSet<RankingEntry> list = new TreeSet<>(new Comparator<RankingEntry>() {
                @Override
                public int compare(RankingEntry o1, RankingEntry o2) {
                    return (int) (o2.getCount() - o1.getCount());
                }
            });
            list.addAll(smartphoneStatistics.getRankings());
            int ranking = 0;
            for (final RankingEntry entry : list) {
                ranking++;
                if (entry.getPhoneId() == phoneId) {
                    final int finalRanking = ranking;
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final TextView rankingTextView = (TextView) getView().findViewById(R.id.rankingMessage);
                            rankingTextView.setText(String.format("Position %d of %d", finalRanking, list.size()));
                        }
                    });
                }
            }
        } catch (NullPointerException ignore) {
        }
    }

    public void updateExperimentRankings(final SmartphoneStatisticsDTO smartphoneStatistics) {
        if (smartphoneStatistics.getExperimentRankings() == null) {
            final LinearLayout currentExperimentLayout = (LinearLayout) getView().findViewById(R.id.currentExperimentLayout);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentExperimentLayout.setVisibility(View.GONE);
                }
            });
        } else {
            final LinearLayout currentExperimentLayout = (LinearLayout) getView().findViewById(R.id.currentExperimentLayout);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentExperimentLayout.setVisibility(View.VISIBLE);
                }
            });
            try {
                final SortedSet<RankingEntry> list = new TreeSet<>(new Comparator<RankingEntry>() {
                    @Override
                    public int compare(RankingEntry o1, RankingEntry o2) {
                        return (int) (o2.getCount() - o1.getCount());
                    }
                });
                list.addAll(smartphoneStatistics.getExperimentRankings());
                int ranking = 0;
                for (final RankingEntry entry : list) {
                    ranking++;
                    if (entry.getPhoneId() == phoneId) {
                        final int finalRanking = ranking;
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final TextView experimentRankingTextView = (TextView) getView().findViewById(R.id.rankingExperimentMessage);
                                experimentRankingTextView.setText(String.format("Position %d of %d", finalRanking, list.size()));
                            }
                        });
                    }
                }

            } catch (NullPointerException ignore) {

            }
        }
    }

    public void fillStatsFields() {
//        final TextView timeTodayTextView = (TextView) getView().findViewById(R.id.stats_time_value);
//        final TextView timeAllTextView = (TextView) getView().findViewById(R.id.stats_time_value_a);
//
//        getActivity().runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                timeTodayTextView.setText(String.format("%.3g", (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
//                timeAllTextView.setText(String.format("%.3g", (float) TimeUnit.MILLISECONDS.toSeconds(DynamixService.getTotalTimeConnectedOnline()) / 3600));
//            }
//        });

        new Thread(new Runnable() {
            @Override
            public void run() {
//                if (profile == null) {
//                    final String accessToken = AccountUtils.getOfflineToken();
//                    if (accessToken != null) {
//                        final TextView statsName = (TextView) getView().findViewById(R.id.stats_name);
//                        final TextView statsUsername = (TextView) getView().findViewById(R.id.stats_email);
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                try {
//                                    OrganicityAndroidClient client = new OrganicityAndroidClient(accessToken);
//                                    final OrganicityProfileDTO profile = client.getProfle();
//                                    Log.i(TAG, "profile: " + profile.toString());
//                                    if (profile != null) {
//                                        runOnUiThread(new Runnable() {
//                                            @Override
//                                            public void run() {
//                                                Log.i(TAG, "name: " + profile.getName());
//                                                statsName.setText(profile.getName());
//                                                statsUsername.setText(profile.getPreferred_username());
//                                            }
//                                        });
//                                    }
//                                } catch (Exception e) {
//                                    Log.e(TAG, e.getLocalizedMessage(), e);
//                                }
//                            }
//                        }).start();
//                    }
//                }

                phoneId = AppModel.instance.phoneProfiler.getPhoneId();
                final Experiment exp = AppModel.instance.experiment;
                if (!isNetworkConnected()) {
                    return;
                }
                if (System.currentTimeMillis() - lastUpdate > 10 * 60 * 1000 || hadLastExp == (exp != null)) {
                    final SmartphoneStatisticsDTO smartphoneStatistics;
                    if (exp == null) {
                        smartphoneStatistics = Communication.getInstance().getSmartphoneStatistics(phoneId);
                    } else {
                        smartphoneStatistics = Communication.getInstance().getSmartphoneStatistics(phoneId, exp.getId());
                    }
                    if (smartphoneStatistics != null && getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateFields(smartphoneStatistics);
                            }
                        });
                        lastUpdate = System.currentTimeMillis();
                        hadLastExp = exp != null;
                    } else {
                        Log.w(TAG, "smartphoneStatistics:" + smartphoneStatistics);
                    }
                }

            }
        }).start();
    }

    public void downloadFile(final Button downloadDataButton, final String expId, final long deviceId) {

        try {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    downloadDataButton.setText("Downloading...");
                }
            });
            URL u = new URL("http://api.smartphone-experimentation.eu/api/v1/data/?deviceId=" + deviceId);
            String filename = deviceId + ".json";
            if (expId != null) {
                u = new URL("http://api.smartphone-experimentation.eu/api/v1/experiment/data/" + expId + "/list?deviceId=" + deviceId);
                filename = expId + "-" + deviceId + ".json";
            }
            InputStream is = u.openStream();

            DataInputStream dis = new DataInputStream(is);

            byte[] buffer = new byte[1024];
            int length;
            Log.i(TAG, "Download for " + deviceId);
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);
            FileOutputStream fos = new FileOutputStream(file);
            Log.i(TAG, "File " + file.getAbsolutePath());
            while ((length = dis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            sendFile(file);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    downloadDataButton.setText("Download collected Data");
                }
            });

        } catch (MalformedURLException mue) {
            Log.e("SYNC getUpdate", "malformed url error", mue);
        } catch (IOException ioe) {
            Log.e("SYNC getUpdate", "io error", ioe);
        } catch (SecurityException se) {
            Log.e("SYNC getUpdate", "security error", se);
        }
    }

    private void sendFile(File file) {
        Uri path = Uri.fromFile(file);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // set the type to 'email'
        emailIntent.setType("vnd.android.cursor.dir/email");

        // the attachment
        emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        // the mail subject
        startActivity(Intent.createChooser(emailIntent, "Send email..."));
    }

    @Override
    public void setDeviceId(String id) {

    }

    @Override
    public void setDescription(String description) {

    }

    boolean isNetworkConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnected();

    }

    public MapView getMap() {
        return mapView;
    }
}
