package eu.organicity.set.app.operations;

import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.sdk.Reading;
import eu.smartsantander.androidExperimentation.jsonEntities.Experiment;

public class AsyncStatusRefreshTask extends AsyncTask<Void, String, Integer> {
    RefreshTaskInterface listener;

    public AsyncStatusRefreshTask(RefreshTaskInterface listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        //Organicity
        PhoneProfiler phoneProfiler = AppModel.instance.phoneProfiler;
        Experiment experiment = AppModel.instance.experiment;

        if (experiment != null) {
            if (!AppModel.instance.isDeviceRegistered()) {
               phoneProfiler.register();
            } else {
                publishProgress("phone", String.valueOf(phoneProfiler.getPhoneId()));
                phoneProfiler.savePrefs();
            }
        } else {
            publishProgress("phone", String.valueOf("Not Connected"));
        }

        //set the phone id and experiment description fields
        if (experiment!= null) {
            publishProgress("phone", String.valueOf(phoneProfiler.getPhoneId()));
            publishProgress("experimentDescription", String.valueOf(experiment.getName()));
        }

        //parse the message and update the data sparklines
//        if (DynamixService.isFrameworkInitialized()) {
//            //get the last experiment message and show the changes in the home activity
//        }
        return 0;
    }

    private void showMeasurementSparkLines(final Reading reading) throws JSONException {

        final JSONObject obj = new JSONObject(reading.getValue());
        final Iterator keyIterator = obj.keys();
        while (keyIterator.hasNext()) {
            final String next = (String) keyIterator.next();
            // Add item to adapter
            Double value = null;
            try {
                value = (Double) obj.get(next);
            } catch (ClassCastException e) {
                try {
                    value = Double.valueOf((Integer) obj.get(next));
                } catch (Exception ignore) {
                }
            }
            if (value != null) {
                publishProgress("spark-line", next, String.valueOf(value));
            }
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if ("phone".equals(values[0])) {
            listener.setDeviceId("Device ID: " + String.valueOf(values[1]));

        } else if ("experimentDescription".equals(values[0])) {
            listener.setDescription(values[1]);
        } else if ("spark-line".equals(values[0])) {
            //            final String next = values[1];
            //            final double value = Double.parseDouble(values[2]);
            //            boolean found = false;
            //            for (final SensorMeasurement sensorMeasurement : activity.sensorMeasurements) {
            //                if (sensorMeasurement.getType().equals(next)) {
            //                    found = true;
            //                    sensorMeasurement.add(value);
            //                    activity.sensorMeasurementAdapter.notifyDataSetChanged();
            //                    break;
            //                }
            //            }
            //            if (!found) {
            //                final SensorMeasurement measurement = new SensorMeasurement(next, value);
            //                activity.sensorMeasurements.add(measurement);
            //            }
        }
    }

    @Override
    protected void onCancelled() {
    }

    public interface RefreshTaskInterface {
        void setDeviceId(String id);
        void setDescription(String description);
    }

}
