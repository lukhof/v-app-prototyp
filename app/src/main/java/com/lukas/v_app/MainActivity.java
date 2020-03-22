package com.lukas.v_app;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lukas.v_app.adapter.DividerItemDecorator;
import com.lukas.v_app.adapter.RecommendationActionAdapter;
import com.lukas.v_app.data.RecommendationAction;
import com.lukas.v_app.data.RiskOfInfectionStatus;
import com.lukas.v_app.helper.SQLhelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity {

    private final static int ACCESS_FINE_LOCATION_PERMISSION_REQUEST = 0;

    private final static int ACCESS_COARSE_LOCATION_PERMISSION_REQUEST = 1;

    private RecyclerView recyclerView;

    private RecommendationActionAdapter adapter = new RecommendationActionAdapter();

    private AppBarLayout appBarLayout;

    private CardView cardView;

    private NestedScrollView scrollContainer;

    private TextView riskOfInfectionStatusTextView;

    private CollapsingToolbarLayout collapsingToolbarLayout;

    private int maxScrollContainerTopPadding = 0;

    private Toolbar toolbar;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);
        appBarLayout = findViewById(R.id.appBarLayout);
        cardView = findViewById(R.id.cardView);
        scrollContainer = findViewById(R.id.scrollContainer);
        riskOfInfectionStatusTextView = findViewById(R.id.riskOfInfectionStatus);
        collapsingToolbarLayout = findViewById(R.id.collapsingToolbar);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupView();

        FirebaseMessaging.getInstance().subscribeToTopic("new_infection");

        createNotificationChannel();

        if (!checkIfAllPermissionGranted()) {

            // Permission is not granted
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    ACCESS_FINE_LOCATION_PERMISSION_REQUEST);
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_COARSE_LOCATION },
                    ACCESS_COARSE_LOCATION_PERMISSION_REQUEST);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        } else {
            // Permission has already been granted
            startFusedLocation();
        }

        SharedPreferences preferences = getSharedPreferences("data", Context.MODE_PRIVATE);
        if(preferences.getBoolean("infected", false)) {
            setRecommendationActions(RiskOfInfectionStatus.HIGH);
        } else {
            setRecommendationActions(RiskOfInfectionStatus.LOW);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        if (item.getItemId() == R.id.report_infection) {
           startActivity(new Intent(this, ReportInfectionActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ViewCompat.setNestedScrollingEnabled(recyclerView, false);
        recyclerView.setAdapter(adapter);
        DividerItemDecoration itemDecorator = new DividerItemDecoration(this,
                DividerItemDecoration.VERTICAL);
        recyclerView.addItemDecoration(new DividerItemDecorator(itemDecorator.getDrawable()));

        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                float percentage = 1 - ((float) Math.abs(verticalOffset)
                        / appBarLayout.getTotalScrollRange());
                cardView.setAlpha(percentage);
                if (maxScrollContainerTopPadding == 0) {
                    maxScrollContainerTopPadding = scrollContainer.getPaddingTop();
                }
                scrollContainer.setPaddingRelative(0,
                        (int) (maxScrollContainerTopPadding * percentage), 0, 0);

                if (percentage < 0.2) {
                    collapsingToolbarLayout.setTitle(
                            "Ihr Infektionsrisiko ist " + riskOfInfectionStatusTextView.getText()
                                    .toString()
                                    .toLowerCase());
                } else {
                    collapsingToolbarLayout.setTitle("Ihr Infektionsrisiko");
                }
            }
        });

    }

    private void setRecommendationActions(RiskOfInfectionStatus status) {
        String statusText = "";
        int statusColor = ContextCompat.getColor(this, R.color.inactive);
        List<RecommendationAction> items = new ArrayList<>();
        switch (status) {
            case HIGH:
                statusColor = ContextCompat.getColor(this, R.color.negative);
                statusText = getString(R.string.risk_of_infection_high);
                items = recommendationActionsForHighInfectionRisk();
                break;
            case LOW:
                statusColor = ContextCompat.getColor(this, R.color.positive);
                statusText = getString(R.string.risk_of_infection_low);
                items = recommendationActionsForLowInfectionRisk();
                break;
        }
        riskOfInfectionStatusTextView.setTextColor(statusColor);
        riskOfInfectionStatusTextView.setText(statusText);
        adapter.updateItems(items);
    }

    private List<RecommendationAction> recommendationActionsForLowInfectionRisk() {
        List<RecommendationAction> items = new ArrayList<>();
        items.add(new RecommendationAction(
                "Häufiges und gründliches Händewaschen (min. 20 Sekunden)"));
        items.add(new RecommendationAction("Vermeidung von Großveranstaltungen"));
        items.add(new RecommendationAction("Vermeiden Sie Berührungen im Gesicht"));
        items.add(new RecommendationAction(
                "Lüften Sie alle Aufenthaltsräume regelmäßig und vermeiden Sie Berührungen wie z. B. Händeschütteln oder Umarmungen."));
        items.add(new RecommendationAction(
                "Husten oder Niesen Sie in Papiertaschentücher und entsorgen Sie diese auch"));

        items.add(new RecommendationAction("Abstand halten (1-2 Meter)"));
        items.add(new RecommendationAction(
                "Bleiben Sie, so oft es geht, zu Hause. Schränken Sie insbesondere die persönlichen Begegnungen mit älteren, hochbetagten oder chronisch kranken Menschen zu deren Schutz ein. "));
        items.add(new RecommendationAction(
                "Wenn eine Person in Ihrem Haushalt erkrankt ist, sorgen Sie nach Möglichkeit für eine räumliche Trennung und genügend Abstand zu den übrigen Haushaltsmitgliedern."));
        items.add(new RecommendationAction(
                "Kaufen Sie nicht zu Stoßzeiten ein, sondern dann, wenn die Geschäfte weniger voll sind oder nutzen Sie Abhol- und Lieferservices."));
        items.add(new RecommendationAction(
                "Helfen Sie denen, die Hilfe benötigen! Versorgen Sie ältere, hochbetagte, chronisch kranke Angehörige oder Nachbarn und alleinstehende und hilfsbedürftige Menschen mit Lebensmitteln und Dingen des täglichen Bedarfs."));

        return items;
    }


    private List<RecommendationAction> recommendationActionsForHighInfectionRisk() {
        List<RecommendationAction> items = new ArrayList<>();
        items.add(new RecommendationAction(
                getString(R.string.high_infection_risk_recommendation_action, 6)));

        return items;
    }

    private boolean checkIfAllPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
            @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        if (checkIfAllPermissionGranted()) {
            startFusedLocation();
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    private void writeLocationToSharedPref(Date timestamp, double longitude, double latitude){

        if(latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) {

            JSONObject tempJson = new JSONObject();
            try {
                tempJson.put("latitude", latitude);
                tempJson.put("longitude", longitude);
                tempJson.put("time", timestamp.getTime());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            SharedPreferences prefs = this.getSharedPreferences("LOCATION", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            Set<String> oldMaps = prefs.getStringSet("locations", new HashSet<String>());

            oldMaps.add(tempJson.toString());

            editor.putStringSet("locations", oldMaps);
            editor.apply();
        }
    }

    private void sendLocationsToFirestore(){

        SharedPreferences prefs = this.getSharedPreferences("LOCATION", Context.MODE_PRIVATE);

        Set<String> oldMaps = prefs.getStringSet("locations", new HashSet<String>());

        List<String> listLocations = new ArrayList<>(oldMaps);

        ArrayList<HashMap> values = new ArrayList<>();

        for (String s : listLocations) {
            try {
                JSONObject jsonObject = new JSONObject(s);

                HashMap<String, Object> tempMap = new HashMap<>();
                tempMap.put("geo", new GeoPoint(jsonObject.getDouble("latitude"), jsonObject.getDouble("longitude")));
                tempMap.put("time", new Timestamp(new Date(jsonObject.getLong("time"))));

                values.add(tempMap);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("values", values);

        db.collection("test")
                .add(result)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("Firebase", "Uploaded");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.notification_channel_id), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void startFusedLocation() {
        FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(60 * 1000);
        locationRequest.setFastestInterval(30 * 1000);
        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        Date timestamp = Calendar.getInstance().getTime();
                        addGeopoint(getApplicationContext(), location.getLatitude(), location.getLongitude(), timestamp);
                        //writeLocationToSharedPref(timestamp, location.getLongitude(), location.getLatitude());
                    }
                }
            }
        };

        mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
    }

    public static void addGeopoint(Context context, double latitude, double longitude, Date date) {
        SQLhelper dbHelper = new SQLhelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("Latitude", latitude);
        values.put("Longitude", longitude);
        values.put("Time", date.getTime());

        db.insert("Geopoints", null, values);
    }

    public static ArrayList<HashMap<String, Object>> getAllPoints(Context context) {
        SQLhelper dbHelper = new SQLhelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        String[] projection = {
                "Latitude",
                "Longitude",
                "Time"
        };

        Cursor cursor = db.query(
                "Geopoints",
                projection,
                null,
                null,
                null,
                null,
                null
        );

        ArrayList result = new ArrayList<HashMap<String, Object>>();
        while(cursor.moveToNext()) {
            HashMap<String, Object> tempMap = new HashMap<String, Object>();

            tempMap.put("geo", new GeoPoint(cursor.getDouble(cursor.getColumnIndex("Latitude")), cursor.getDouble(cursor.getColumnIndex("Longitude"))));
            tempMap.put("time", new Timestamp(new Date(cursor.getLong(cursor.getColumnIndex("Time")))));
            result.add(tempMap);
        }
        cursor.close();

        return result;
    }
}
