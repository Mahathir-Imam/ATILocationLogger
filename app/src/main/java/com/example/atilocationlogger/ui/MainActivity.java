package com.example.atilocationlogger.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.atilocationlogger.R;
import com.example.atilocationlogger.data.AppDatabase;
import com.example.atilocationlogger.data.LocationLog;
import com.example.atilocationlogger.location.LocationForegroundService;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import android.widget.EditText;
import android.widget.TextView;

import com.example.atilocationlogger.network.ApiClient;
import com.example.atilocationlogger.network.Post;
import com.google.android.material.appbar.MaterialToolbar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private static final int REQ_LOCATION = 101;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        Button btnShowLogs = findViewById(R.id.btnShowLogs);
        TextView tvLogs = findViewById(R.id.tvLogs);

        btnShowLogs.setOnClickListener(v -> {
            new Thread(() -> {
                AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                List<LocationLog> logs = db.locationLogDao().getLatest10();

                StringBuilder sb = new StringBuilder();
                DateFormat df = DateFormat.getDateTimeInstance();

                for (LocationLog log : logs) {
                    sb.append("ID: ").append(log.id)
                            .append(" | ").append(log.latitude).append(", ").append(log.longitude)
                            .append(" | ").append(df.format(new Date(log.timestamp)))
                            .append("\n");
                }

                runOnUiThread(() -> tvLogs.setText(sb.length() == 0 ? "No logs yet" : sb.toString()));
            }).start();
        });


        Button btnStart = findViewById(R.id.btnStartService);
        Button btnStop = findViewById(R.id.btnStopService);

        btnStart.setOnClickListener(v -> {
            if (!ensureLocationPermission()) return;

            Intent i = new Intent(this, LocationForegroundService.class);
            ContextCompat.startForegroundService(this, i);
        });

        btnStop.setOnClickListener(v -> stopService(new Intent(this, LocationForegroundService.class)));

        // Keep your edge-to-edge padding behavior
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        EditText etApiInput = findViewById(R.id.etApiInput);
        Button btnFetchApi = findViewById(R.id.btnFetchApi);
        TextView tvApiResult = findViewById(R.id.tvApiResult);

        btnFetchApi.setOnClickListener(v -> {

            String raw = etApiInput.getText().toString().trim();

            // Input validation
            if (raw.isEmpty()) {
                tvApiResult.setText("Error: Please enter a Post ID (1-100).");
                return;
            }

            int id;
            try {
                id = Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                tvApiResult.setText("Error: Post ID must be a number.");
                return;
            }

            if (id < 1 || id > 100) {
                tvApiResult.setText("Error: Post ID must be between 1 and 100.");
                return;
            }

            tvApiResult.setText("Loading...");

            // API call
            ApiClient.getApiService().getPost(id).enqueue(new Callback<Post>() {
                @Override
                public void onResponse(Call<Post> call, Response<Post> response) {

                    // Error handling: HTTP error
                    if (!response.isSuccessful()) {
                        tvApiResult.setText("API error: HTTP " + response.code());
                        return;
                    }

                    Post post = response.body();

                    // Error handling: null/invalid body
                    if (post == null || post.title == null || post.title.trim().isEmpty()) {
                        tvApiResult.setText("API error: Empty/invalid response.");
                        return;
                    }

                    tvApiResult.setText(
                            "Title: " + post.title + "\n\n" +
                                    "Body:\n" + post.body
                    );
                }

                @Override
                public void onFailure(Call<Post> call, Throwable t) {
                    //  Error handling: network failure, no internet, timeout
                    tvApiResult.setText("Network error: " + (t.getMessage() == null ? "unknown" : t.getMessage()));
                }
            });
        });


    }

    private boolean ensureLocationPermission() {
        int fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQ_LOCATION
        );
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode != REQ_LOCATION) return;

        if (grantResults.length == 0) {
            Toast.makeText(this, "Permission request cancelled", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean grantedAny = false;
        for (int r : grantResults) {
            if (r == PackageManager.PERMISSION_GRANTED) {
                grantedAny = true;
                break;
            }
        }

        if (grantedAny) {
            Intent i = new Intent(this, LocationForegroundService.class);
            ContextCompat.startForegroundService(this, i);
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }
}
