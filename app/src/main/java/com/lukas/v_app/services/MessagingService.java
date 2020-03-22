package com.lukas.v_app.services;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lukas.v_app.BuildConfig;
import com.lukas.v_app.tasks.CheckDataset;

import java.util.Map;
import java.util.Objects;

public class MessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        Map<String, String> data = remoteMessage.getData();

        try {
            if (Objects.equals(data.get("unit"), BuildConfig.BUILD_TYPE)) {
                final double latitude = Double.parseDouble(Objects.requireNonNull(data.get("lat")));
                final double longitude = Double.parseDouble(Objects.requireNonNull(data.get("lon")));
                final double range = Double.parseDouble(Objects.requireNonNull(data.get("range")));
                final String id = data.get("id");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new CheckDataset(getApplicationContext(), id).execute(latitude, longitude, range);
                    }
                }).start();
            }
        } catch (NullPointerException ignored) {}
    }
}
