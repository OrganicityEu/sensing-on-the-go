package eu.smartsantander.androidExperimentation.service;
/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.NotificationManager;
import android.support.v4.app.NotificationCompat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.util.Map;

import eu.organicity.set.app.R;
import eu.smartsantander.androidExperimentation.util.GcmMessageData;

public class MyGcmListenerService extends FirebaseMessagingService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param message the message sent from the server.
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(RemoteMessage message) {
        final String from = message.getFrom();
        final Map data = message.getData();
        onMessageReceived(from, data);
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    public void onMessageReceived(String from, Map data) {
        final String message = (String) data.get("message");

        if (from.startsWith("/topics/")) {
            // message received from some topic.

            String notificationTitle = null;
            String notificationMessage = null;
            String messageShort = null;
            try {
                GcmMessageData gcmMessageData = new ObjectMapper().readValue(message, GcmMessageData.class);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this);


                switch (gcmMessageData.getType()) {
                    case "encourage":
                        messageShort = String.valueOf(gcmMessageData.getCount()) +
                                " measurements collected!";
                        notificationMessage = "You collected " +
                                gcmMessageData.getCount() +
                                " measurements so far today. " +
                                "Keep up the good work.";
                        notificationTitle = "Hooray!";
                        mBuilder.setSmallIcon(R.drawable.award);
                        break;
                    case "text":
                        messageShort = gcmMessageData.getText();
                        notificationMessage = gcmMessageData.getText();
                        notificationTitle = "Experiment Message";
                        mBuilder.setSmallIcon(R.drawable.organicity_small_pink);
                        break;
                    default:
                        return;

                }

                mBuilder.setContentTitle(notificationTitle)
                        .setContentText(messageShort)
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(notificationMessage));

                int mNotificationId = 002;
                NotificationManager mNotifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                mNotifyMgr.notify(mNotificationId, mBuilder.build());

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // normal downstream message.
        }

        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//                PendingIntent.FLAG_ONE_SHOT);
//
//        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
//                .setSmallIcon(R.drawable.ic_stat_ic_notification)
//                .setContentTitle("GCM Message")
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(defaultSoundUri)
//                .setContentIntent(pendingIntent);
//
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}