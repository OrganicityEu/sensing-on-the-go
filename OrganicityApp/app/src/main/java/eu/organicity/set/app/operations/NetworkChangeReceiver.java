package eu.organicity.set.app.operations;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "NetworkChangeReceiver";
    private static AsyncReportOnServerTask reportT = null;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        process(context);
    }

    public static synchronized void process(Context context) {
        try {

            final ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

            if (wifi.isConnected()) {
                if (reportT == null || reportT.isFinished()) {
                    if (reportT != null) {
                        reportT.cancel(true);
                    }
                    reportT = new AsyncReportOnServerTask();
                    reportT.execute();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

}
