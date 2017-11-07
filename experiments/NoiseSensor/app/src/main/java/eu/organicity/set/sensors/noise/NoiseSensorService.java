package eu.organicity.set.sensors.noise;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.intentfilter.androidpermissions.PermissionManager;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import eu.organicity.set.app.sdk.ISensorCallback;
import eu.organicity.set.app.sdk.ISensorService;
import eu.organicity.set.app.sdk.JsonMessage;
import eu.organicity.set.app.sdk.Reading;

import static java.util.Collections.singleton;

public class NoiseSensorService extends Service {

    private static final String TAG = "NoiseSensorService";
    private static final int MSG_PLUGIN_INFO = 53;
    public static String CONTEXT_TYPE;

    private ISensorCallback mRemoteCallbacks;

    private ServiceHandler mHandler = null;

    HandlerThread mHandlerThread = new HandlerThread("AidlServiceThread");

    private long SENSOR_POLL_INTERVAL = 800;
    public static double REFERENCE = 0.00002;
    private MediaRecorder mRecorder = null;
    private boolean enabled = false;
    private Handler handler;
    private LinkedList<Double> queue = new LinkedList<Double>();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            captureNoiseLevel();
            if (enabled) {
                handler.postDelayed(this, SENSOR_POLL_INTERVAL);
            }
        }
    };

    public NoiseSensorService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CONTEXT_TYPE = getApplicationContext().getPackageName();

        //TODO setup here

        PermissionManager permissionManager = PermissionManager.getInstance(getApplicationContext());
        permissionManager.checkPermissions(singleton(Manifest.permission.RECORD_AUDIO), new PermissionManager.PermissionRequestListener() {
            @Override
            public void onPermissionGranted() {
                Toast.makeText(getApplicationContext(), "Permissions Granted", Toast.LENGTH_SHORT).show();
                mRecorder = new MediaRecorder();
                handler = new Handler();
                enabled = true;
                runnable.run();
            }

            @Override
            public void onPermissionDenied() {
                Toast.makeText(getApplicationContext(), "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "on start command called");
        return START_STICKY;
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
        super.onDestroy();

        Log.i(TAG, "Service destroyed!");
        //TODO unregister listeners, unbind services and clean up here
        mHandlerThread.quit();
        mHandler.removeCallbacksAndMessages(null);
        mHandler = null;

        try {
            mRecorder.stop();     // stop recording
            mRecorder.reset();    // set state to idle
            mRecorder.release();  // release resources back to the system
        }
        catch (IllegalStateException e) {
            e.printStackTrace();
        }

        mRecorder = null;

        stopSelf();
    }

    /**
     * Stub implementation for Remote service
     */
    ISensorService.Stub mBinder = new ISensorService.Stub() {

        @Override
        public void getPluginInfo(ISensorCallback callback) throws RemoteException {

            Log.d(TAG, "getPluginInfo called!");
            sendMsgToHandler(callback, MSG_PLUGIN_INFO);
        }
    };

    /**
     * Create handler message to be sent
     *
     * @param callback
     * @param flag
     */
    void sendMsgToHandler(ISensorCallback callback, int flag) {

        mRemoteCallbacks = callback;

        Message message = mHandler.obtainMessage();

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
                    //TODO sensor logic here

                    broadcastNoiseLevel();
                    break;
            }
        }
    }

    public void broadcastNoiseLevel() {
        List<Reading> r = new ArrayList<>();
        JsonMessage info = new JsonMessage();

        if (this.queue.size() == 0) {
            double db = captureNoiseLevel();
            Log.w(TAG, "NoiseLevel Plugin:" + db);
        } else {
            for (Double dbM : this.queue) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(CONTEXT_TYPE + ".NoiseLevel", dbM);
                    r.add(new Reading(obj.toString(), CONTEXT_TYPE + ".NoiseSensorService"));
                } catch (Exception ignored) {
                }
            }
        }

        info.setPayload(r);
        info.setState("OK");

        try {
            if (mRemoteCallbacks != null) {
                mRemoteCallbacks.handlePluginInfo(info);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    double captureNoiseLevel() {
        Log.d(TAG, "capture!");
        try {
            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mRecorder.setOutputFile("/dev/null");
            mRecorder.prepare();
            mRecorder.start();

            double sum = 0;
            double ma = mRecorder.getMaxAmplitude();
            double value;
            for (int i = 1; i <= 10; i++) {
                Thread.sleep(100);
                sum += mRecorder.getMaxAmplitude();
                ma = sum / i;

            }
            Log.w(TAG, "NoiseLevel Max Anplitute AVG:" + ma);
            value = (ma / 51805.5336);
            double db = 20 * Math.log10(value / REFERENCE);
            Log.e(TAG, "NoiseLevel db:" + db);
            this.queue.addLast(db);

            if (this.queue.size() > 10)
                this.queue.removeFirst();

            mRecorder.stop();
            mRecorder.reset();
            return db;
        } catch (Exception e) {
            Log.e("NoiseLevel Plugin Error", e.toString());
            return -1;
        }
    }
}
