package eu.smartsantander.androidExperimentation;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

/**
 * Android ActivityRecognitionService.
 */
public class ActivityRecognitionService extends IntentService {

    private static final String TAG = "ActivityRecognition";
    private Context context;
    private static int activityConfidence;
    private static int activityCode;
    private static String activityStatus;


    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ActivityRecognitionService() {
        super("ActivityRecognitionService");
    }

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ActivityRecognitionService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

            context = getApplicationContext();

            activityConfidence = result.getMostProbableActivity().getConfidence();
            activityCode = result.getMostProbableActivity().getType();
            activityStatus = getType(result.getMostProbableActivity().getType());
        }
    }

    private void evaluateActivityResult() {
    }

    // This method is only used in a log line to have readable status in logs
    private String getType(int type) {
        switch (type) {
            case DetectedActivity.UNKNOWN: {
                return "UNKNOWN";
            }
            case DetectedActivity.IN_VEHICLE: {
                return "IN_VEHICLE";
            }
            case DetectedActivity.ON_BICYCLE: {
                return "ON_BICYCLE";
            }
            case DetectedActivity.ON_FOOT: {
                return "ON_FOOT";
            }
            case DetectedActivity.STILL: {
                return "STILL";
            }
            case DetectedActivity.TILTING: {
                return "TILTING";
            }
            default: {
                return "";
            }
        }
    }

    public static int getActivityCode() {
        return activityCode;
    }

    public static int getActivityConfidence() {
        return activityConfidence;
    }

    public static String getActivityStatus() {
        return activityStatus;
    }
}
