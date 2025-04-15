package com.example.skipq;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "order_notifications";
    private static final String CHANNEL_NAME = "Order Updates";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d("FCM", "onMessageReceived called");
        if (remoteMessage.getNotification() != null) {
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            Log.d("FCM", "Message received: Title=" + title + ", Body=" + body);
            showNotification(title, body);
        } else {
            Log.d("FCM", "No notification payload in message");
        }
        if (remoteMessage.getData().size() > 0) {
            Log.d("FCM", "Data payload: " + remoteMessage.getData());
        }
    }

    private void showNotification(String title, String message) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for order updates");
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Fallback to android.R.drawable.ic_dialog_info if missing
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
            Log.d("FCM", "Notification displayed: " + title);
        } catch (SecurityException e) {
            Log.e("FCM", "Notification permission denied", e);
        }
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM", "New Token: " + token);

        String userId = getUserIdFromSession();
        if (userId != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users")
                    .document(userId)
                    .update("deviceToken", token)
                    .addOnSuccessListener(aVoid -> Log.d("FCM", "User token saved"))
                    .addOnFailureListener(e -> Log.e("FCM", "Error saving user token", e));

            db.collection("FoodPlaces")
                    .whereEqualTo("uid", userId)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            String restaurantId = querySnapshot.getDocuments().get(0).getId();
                            db.collection("FoodPlaces")
                                    .document(restaurantId)
                                    .update("deviceToken", token)
                                    .addOnSuccessListener(aVoid -> Log.d("FCM", "Restaurant token saved for ID: " + restaurantId))
                                    .addOnFailureListener(e -> Log.e("FCM", "Error saving restaurant token", e));
                        } else {
                            Log.d("FCM", "No restaurant found for UID: " + userId);
                        }
                    })
                    .addOnFailureListener(e -> Log.e("FCM", "Error checking restaurant", e));
        } else {
            Log.e("FCM", "User ID is null, cannot save token");
        }
    }

    private String getUserIdFromSession() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
    }
}