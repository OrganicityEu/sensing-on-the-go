package eu.organicity.set.app.operations;

import android.os.AsyncTask;
import android.util.Log;

import org.springframework.web.client.HttpClientErrorException;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.sdk.Report;

public class AsyncReportNowTask extends AsyncTask<Report, Void, String> {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    protected String doInBackground(Report... params) {
        Report report = params[0];
        try {
            //try to send to server, on fail save it in SQLite
            Communication.getInstance().setLastMessage(report.getJobResults());
            Log.i(TAG, report.toString());
            final String r = Communication.getInstance().sendReportResults(report);
            Log.i(TAG, r.toString());
        } catch (HttpClientErrorException e) {
            //ignore
            Log.e(TAG, e.getMessage(), e);
        } catch (Exception e) {
            // TODO Change this
            AppModel.addExperimentalMessage(report.getJobResults());
            Log.e(TAG, e.getMessage(), e);
        }
        return "AndroidExperimentation Async Experiment Task Executed";
    }

    @Override
    protected void onPostExecute(String result) {
    }

    @Override
    protected void onPreExecute() {
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }

    @Override
    protected void onCancelled() {
    }

}
