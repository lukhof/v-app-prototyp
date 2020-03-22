package com.lukas.v_app.tasks;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;
import com.lukas.v_app.MainActivity;
import com.lukas.v_app.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public class CheckDataset extends AsyncTask<Double, Double, Boolean> {

    private Context context;
    private FirebaseFunctions mFunctions;
    private String id;

    public CheckDataset(Context context, String id) {
        this.context = context;
        mFunctions = FirebaseFunctions.getInstance();
        this.id = id;
    }

    @Override
    protected Boolean doInBackground(Double[] objects) {
        Double latitude = objects[0];
        Double longitude = objects[1];
        Double range = objects[2];

        ArrayList<HashMap<String, Object>> geopoints = MainActivity.getAllPoints(context);

        ArrayList<Double> latitudes = new ArrayList<>();
        ArrayList<Double> longitudes = new ArrayList<>();

        ArrayList<HashMap> values = new ArrayList<>();

        for( HashMap<String, Object> o : geopoints) {
            try {
                HashMap<String, Object> tempMap = new HashMap<>();
                JSONObject geo = new JSONObject();
                geo.put("latitude", ((GeoPoint) o.get("geo")).getLatitude());
                geo.put("longitude", ((GeoPoint) o.get("geo")).getLongitude());

                tempMap.put("geo", geo);
                tempMap.put("time", o.get("time"));
                values.add(tempMap);

                latitudes.add(((GeoPoint) o.get("geo")).getLatitude());
                latitudes.add(((GeoPoint) o.get("geo")).getLongitude());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        double lat_m = (Collections.max(latitudes) + Collections.min(latitudes)) / 2;
        double lon_m = (Collections.max(longitudes) + Collections.min(longitudes)) / 2;
        double range_m = Math.max(Collections.max(latitudes) - lat_m, Collections.max(longitudes) - lon_m);

        double lo = longitude - lon_m;
        double la = latitude - lat_m;

        double temp = la*la + lo*lo;
        double distanceBetweenPoints = Math.sqrt(temp);

        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("values", values);

        if (distanceBetweenPoints < range + range_m) {
            mFunctions
                    .getHttpsCallable("isInfected")
                    .call(data)
                    .continueWith(new Continuation<HttpsCallableResult, Boolean>() {
                        @Override
                        public Boolean then(@NonNull Task<HttpsCallableResult> task) throws Exception {
                            JSONObject result = new JSONObject((String) Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getData()));
                            return result.getBoolean("result");
                        }
                    });
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if(result) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, context.getString(R.string.notification_channel_id))
                    .setSmallIcon(R.drawable.ic_logo)
                    .setContentTitle(context.getString(R.string.notification_infected_title))
                    .setContentText(context.getString(R.string.notifiction_infected_text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            notificationManager.notify(new Random().nextInt(1000), builder.build());

            SharedPreferences pref = context.getSharedPreferences("data", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("infected", true);
            editor.apply();
       }
    }
}
