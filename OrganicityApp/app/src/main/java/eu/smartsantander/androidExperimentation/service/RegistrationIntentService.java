package eu.smartsantander.androidExperimentation.service;//package eu.smartsantander.eu.smartsantander.androidExperimentation.service;
//
//import android.app.IntentService;
//import android.content.Intent;
//import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;
//
//import com.google.android.gms.gcm.GcmPubSub;
//import com.google.android.gms.gcm.GoogleCloudMessaging;
//import com.google.android.gms.iid.InstanceID;
//
//import org.ambientdynamix.core.DynamixService;
//import gr.cti.ru1.organicity.set.R;
//
//public class RegistrationIntentService extends IntentService {
//
//    private static final String TAG = "RegIntentService";
//    private static final String[] TOPICS = {"global"};
//
//    public static final String REGISTRATION_COMPLETE = "registrationComplete";
//    private RegistrationIntentService service;
//    private String token;
//
//    public RegistrationIntentService() {
//        super(TAG);
//    }
//
//    @Override
//    protected void onHandleIntent(Intent intent) {
//        try {
//            // [START register_for_gcm]
//            // Initially this call goes out to the network to retrieve the token, subsequent calls
//            // are local.
//            // [START get_token]
//            InstanceID instanceID = InstanceID.getInstance(this);
//            token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
//                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
//            // [END get_token]
//            service = this;
//
//            GcmPubSub pubSub = GcmPubSub.getInstance(this);
//
//            pubSub.subscribe(token, "/topics/experiment-" + DynamixService.getExperiment().getId(), null);
//            pubSub.subscribe(token, "/topics/device-" + DynamixService.getPhoneProfiler().getPhoneId(), null);
//
//            // You should store a boolean that indicates whether the generated token has been
//            // sent to your server. If the boolean is false, send the token to your server,
//            // otherwise your server should have already received the token.
//            // [END register_for_gcm]
//        } catch (Exception e) {
//            // If an exception happens while fetching the new token or updating our registration data
//            // on a third-party server, this ensures that we'll attempt the update at a later time.
//        }
//        // Notify UI that registration has completed, so the progress indicator can be hidden.
//        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
//        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
//    }
//
//
//}