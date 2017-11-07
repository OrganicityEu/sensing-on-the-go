package eu.organicity.set.app.connections;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import eu.organicity.set.app.sdk.IExperimentService;

/**
 * Created by chris on 26/10/2017.
 */

public class ExperimentConnection implements ServiceConnection {
    private static final String TAG = "ExperimentConnection";
    public IExperimentService service;
    private ExperimentCallbacks listener;

    public ExperimentConnection(ExperimentCallbacks listener) {
        this.listener = listener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "Experiment connected!");
        service = IExperimentService.Stub.asInterface(iBinder);

        if (listener != null) {
            listener.experimentStarted();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.e(TAG, "Service has unexpectedly disconnected");
        service = null;

        if (listener != null) {
            listener.experimentStopped();
        }
    }

    public interface ExperimentCallbacks {
        void experimentStarted();
        void experimentStopped();
    }
}