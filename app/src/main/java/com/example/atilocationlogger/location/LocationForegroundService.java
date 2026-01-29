package com.example.atilocationlogger.location;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.atilocationlogger.data.AppDatabase;
import com.example.atilocationlogger.data.LocationLog;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class LocationForegroundService extends Service {

    private static final String CHANNEL_ID = "location_channel";
    private static final int NOTIF_ID = 1001;

    // Change this back to 5 * 60 * 1000L for real requirement (5 minutes)
    private static final long INTERVAL_MS = 5 * 60 * 1000L; // 5 seconds test

    private FusedLocationProviderClient fusedClient;

    private Handler mainHandler;

    // Background loop thread
    private HandlerThread loopThread;
    private Handler loopHandler;
    private boolean isLoopRunning = false;

    private final Runnable locationRunnable = new Runnable() {
        @Override
        public void run() {
            fetchAndSaveLocationOnce();

            // Schedule next run
            if (loopHandler != null) {
                loopHandler.postDelayed(this, INTERVAL_MS);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        Notification notification = buildNotification();
        startForeground(NOTIF_ID, notification);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        mainHandler = new Handler(Looper.getMainLooper());

        // Create background thread for the loop
        loopThread = new HandlerThread("LocationLoopThread");
        loopThread.start();
        loopHandler = new Handler(loopThread.getLooper());
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location service running")
                .setContentText("Logging location periodically")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Location",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!isLoopRunning) {
            isLoopRunning = true;
            // start immediately
            loopHandler.post(locationRunnable);
        }

        return START_STICKY;
    }

    private void fetchAndSaveLocationOnce() {
        try {
            // Permission check (service can't request permission; activity must do it)
            boolean fine = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
            boolean coarse = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;

            if (!fine && !coarse) {
                mainHandler.post(() ->
                        Toast.makeText(this, "Location permission missing", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            // More reliable than getLastLocation()
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(location -> handleLocation(location))
                    .addOnFailureListener(e -> mainHandler.post(() ->
                            Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    ));

        } catch (Exception e) {
            mainHandler.post(() ->
                    Toast.makeText(this, "Exception: " + e.getMessage(), Toast.LENGTH_SHORT).show()
            );
        }
    }

    private void handleLocation(Location location) {
        if (location == null) {
            mainHandler.post(() ->
                    Toast.makeText(this, "Location is null (enable GPS / wait)", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();

        // Toast
        mainHandler.post(() ->
                Toast.makeText(this, "Location: " + lat + ", " + lon, Toast.LENGTH_SHORT).show()
        );

        // Save to Room
        new Thread(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                db.locationLogDao().insert(new LocationLog(lat, lon, System.currentTimeMillis()));

                // Optional proof toast
                /*
                 mainHandler.post(() ->
                        Toast.makeText(this, "Saved to DB!", Toast.LENGTH_SHORT).show()
                );**/
            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "DB error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        // Stop loop
        isLoopRunning = false;
        if (loopHandler != null) loopHandler.removeCallbacks(locationRunnable);

        // Stop thread
        if (loopThread != null) {
            loopThread.quitSafely();
        }

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Called when app is removed from recent apps (best-effort restart)
        Intent restartIntent = new Intent(getApplicationContext(), RestartServiceReceiver.class);
        sendBroadcast(restartIntent);
        super.onTaskRemoved(rootIntent);
    }


}
