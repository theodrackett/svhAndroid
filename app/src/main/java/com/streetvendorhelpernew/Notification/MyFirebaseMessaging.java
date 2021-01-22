package com.streetvendorhelpernew.Notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.streetvendorhelpernew.R;
import com.streetvendorhelpernew.ui.login.LoginActivity;

public class MyFirebaseMessaging extends FirebaseMessagingService {
    private NotificationManagerCompat notificationManager;
    private NotificationManager notifManager;

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        updateTokenToServer(s);
    }


    public void createNotification(String title, String description, Context context) {


        try {
            final int NOTIFY_ID = 0; // ID of notification
            String id = "id"; // default_channel_id
            String title1 = "title"; // Default Channel
            Intent intent;
            PendingIntent pendingIntent;
            NotificationCompat.Builder builder;
            if (notifManager == null) {
                notifManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = notifManager.getNotificationChannel(id);
                if (mChannel == null) {
                    mChannel = new NotificationChannel(id, title1, importance);
                    mChannel.enableVibration(true);
                    mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                    notifManager.createNotificationChannel(mChannel);
                }
                builder = new NotificationCompat.Builder(context, id);
                intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                builder.setContentTitle(title)                            // required
                        .setSmallIcon(R.drawable.vd)   // required
                        .setContentText(description) // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setTicker(title)
                        .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            } else {
                builder = new NotificationCompat.Builder(context, id);
                intent = new Intent(context, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
                builder.setContentTitle(title)                            // required
                        .setSmallIcon(R.drawable.ic_launcher_foreground)   // required
                        .setContentText(description) // required
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setTicker(title)
                        .setVibrate(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400})
                        .setPriority(Notification.PRIORITY_HIGH);
            }
            Notification notification = builder.build();
            notifManager.notify(NOTIFY_ID, notification);
        } catch (Exception e) {

        }

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Log.d("notification", "onMessageReceived: " + remoteMessage.getNotification().getTitle());

        createNotification(remoteMessage.getNotification().getTitle(), remoteMessage.getNotification().getBody(), this);
//        Intent intent = new Intent(this, DrawerActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//        String channelId = "HNO";
//        NotificationCompat.Builder builder = new  NotificationCompat.Builder(this, channelId)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(remoteMessage.getNotification().getTitle())
//                .setContentText(remoteMessage.getNotification().getBody()).setAutoCancel(true).setContentIntent(pendingIntent);
//        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(channelId, "HNO", NotificationManager.IMPORTANCE_HIGH);
//            manager.createNotificationChannel(channel);
//        }
//        manager.notify(1, builder.build());


        if (remoteMessage.getData() != null) {
//            if (remoteMessage.getData().get("key").equals("Transactions")) {
//
////                Intent intent = new Intent(this, RatingActivity.class);
////                intent.putExtra("driver_id", remoteMessage.getData().get("driver_id"));
////                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                startActivity(intent);
//
//            }
//            else {
////                Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
////                intent.putExtra("currentOrder", remoteMessage.getNotification().getBody());
////                intent.putExtra("adminToken", remoteMessage.getNotification().getTitle());
////                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
////                startActivity(intent);
//            }
        }
    }

    private void updateTokenToServer(String refreshedToken) {

//        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
//            FirebaseDatabase db = FirebaseDatabase.getInstance();
//            DatabaseReference tokens = db.getReference("Tokens");
//            Token token = new Token(refreshedToken, false); //false because this token send form client side
//            tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(token);
//        }
    }

}
