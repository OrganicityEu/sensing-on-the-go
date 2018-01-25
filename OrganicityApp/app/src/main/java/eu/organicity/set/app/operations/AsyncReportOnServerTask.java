package eu.organicity.set.app.operations;

import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import eu.organicity.set.app.AppModel;
import eu.organicity.set.app.fragments.HomeFragment;

public class AsyncReportOnServerTask extends AsyncTask<String, Void, String> {
    private final String TAG = this.getClass().getSimpleName();
    private boolean finished = false;

    public AsyncReportOnServerTask() {
        finished = false;
    }

    @Override
    protected String doInBackground(String... params) {
        finished = false;

        while (AppModel.storage.size() > 0) {
            Pair<Long, String> value = AppModel.storage.getMessage();
            try {
                AppModel.deleteExperimentalMessage(value.first);
                if (value.first != 0 && value.second != null && value.second.length() > 0) {
                    Log.i(TAG, "Parsing:" + value.second);
                    AppModel.publishMessage(HomeFragment.context, value.second);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getLocalizedMessage(), e);
            }
        }
        finished = true;
        return "AsyncReportOnServerTask Executed";
    }

    @Override
    protected void onPostExecute(String result) {
        finished = true;
    }

    @Override
    protected void onPreExecute() {
        finished = false;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        finished = false;
    }

    @Override
    protected void onCancelled() {
        finished = true;
    }

    public boolean isFinished() {
        return this.finished;
    }

}
