package eu.organicity.set.experiments.wifiscannerexperiment;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import eu.organicity.set.sensors.sdk.IAidlCallback;
import eu.organicity.set.sensors.sdk.IExperimentService;
import eu.organicity.set.sensors.sdk.JsonMessage;

public class WifiScannerExperiment extends Service {

    private static final String TAG = "WiFiSensorService";
    private static final int MSG_PLUGIN_INFO = 53;

    private IAidlCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");

    public WifiScannerExperiment() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Handler Thread handling all call back methods
        mHandlerThread.start();
        mHandler = new ServiceHandler(mHandlerThread.getLooper());

        return mBinder;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed!");
        super.onDestroy();
    }

    /**
     * Stub implementation for Remote service
     */
    IExperimentService.Stub mBinder = new IExperimentService.Stub() {

        @Override
        public void getExperimentResult(JsonMessage jsonMessage, JsonMessage jsonMessage1) throws RemoteException {
            Log.d(TAG, jsonMessage.toString());
            jsonMessage1.setJSON(jsonMessage.getJSON());

            Log.d(TAG, "Result: " + jsonMessage1.getJSON().toString());
        }
    };

    /**
     * Create handler message to be sent
     *
     * @param callback
     * @param flag
     */
    void sendMsgToHandler(IAidlCallback callback, int flag) {

//        mRemoteCallbacks.add(callback);
        mRemoteCallbacks = callback;

        Message message = mHandler.obtainMessage();
//        message.arg1 = mRemoteCallbacks.size() - 1;

        message.what = flag;
        mHandler.sendMessage(message);
    }

    /**
     * Handler class sending result in callback to respective
     * application
     */
    private class ServiceHandler extends Handler {
        int callbackIndex = 0;

        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            callbackIndex = msg.arg1;

            switch (msg.what) {

                case MSG_PLUGIN_INFO:

                    break;
            }
        }
    }
}
