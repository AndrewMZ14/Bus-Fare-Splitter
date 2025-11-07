package com.example.busfare_splitterv2.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.UI.Adapters.PassengerAdapter;
import com.example.busfare_splitterv2.UI.Dialogs.PassengerEditDialog;
import com.example.busfare_splitterv2.network.ApiClient;
import com.example.busfare_splitterv2.network.ApiService;
import com.example.busfare_splitterv2.network.PassengerRequest;
import com.example.busfare_splitterv2.network.TripResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripDetailsActivity extends AppCompatActivity {

    private static final String TAG = "TripDetailsActivity";

    private RecyclerView rvPassengers;
    private PassengerAdapter passengerAdapter;
    private TextView tvRouteDate, tvTotalCost, tvPassengerCount;
    private ImageView backBtn;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddPassenger;
    private ApiService apiService;
    private SharedPreferences prefs;

    private TripResponse currentTrip;
    private List<PassengerRequest> currentPassengers;
    private Map<String, Double> passengerSurcharges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);

        initializeViews();
        setupRecyclerView();
        loadTripDetails();

        findViewById(R.id.btnExport).setOnClickListener(v -> exportTripAsCSV());
    }

    private void initializeViews() {
        rvPassengers = findViewById(R.id.rvResults);
        tvRouteDate = findViewById(R.id.tvRouteDate);
        tvTotalCost = findViewById(R.id.tvTotalCost);
        tvPassengerCount = findViewById(R.id.tvPassengerCount);
        progressBar = findViewById(R.id.progressBar);
        backBtn = findViewById(R.id.btnBack);
        fabAddPassenger = findViewById(R.id.fabAddPassenger);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        apiService = ApiClient.getApiService();
        currentPassengers = new ArrayList<>();
        passengerSurcharges = new HashMap<>();

        fabAddPassenger.setOnClickListener(v -> showAddPassengerDialog());
    }

    private void setupRecyclerView() {
        rvPassengers.setLayoutManager(new LinearLayoutManager(this));
        passengerAdapter = new PassengerAdapter(
                currentPassengers,
                this::onRemovePassenger,
                this::onEditPassenger
        );
        rvPassengers.setAdapter(passengerAdapter);
    }

    private void showAddPassengerDialog() {
        PassengerEditDialog dialog = new PassengerEditDialog(this, this::onPassengerSaved);
        dialog.show();
    }

    private void showEditPassengerDialog(int position, PassengerRequest passenger) {
        PassengerEditDialog dialog = new PassengerEditDialog(this, this::onPassengerSaved);
        dialog.setEditMode(passenger, position);
        dialog.show();
    }

    private void onPassengerSaved(PassengerRequest passenger, boolean isEdit, int position) {
        if (isEdit) {
            updatePassenger(position, passenger);
        } else {
            addNewPassenger(passenger);
        }
        recalculateAndDisplayShares();
    }

    private void onRemovePassenger(int position, PassengerRequest passenger) {
        if (currentPassengers.size() <= 1) {
            Toast.makeText(this, "Cannot remove the last passenger", Toast.LENGTH_SHORT).show();
            return;
        }
        removePassenger(position);
    }

    private void onEditPassenger(int position, PassengerRequest passenger) {
        showEditPassengerDialog(position, passenger);
    }

    private void addNewPassenger(PassengerRequest passenger) {
        // Calculate the share amount for the new passenger
        double baseShare = currentTrip.getTotalCost() / (currentPassengers.size() + 1);
        double totalShare = baseShare + passenger.surcharge;

        passenger.setShareAmount(totalShare); // Set the share amount

        currentPassengers.add(passenger);
        passengerSurcharges.put(passenger.name, passenger.surcharge);
        recalculateAndDisplayShares();
        saveSurchargesToPrefs();
    }

    private void updatePassenger(int position, PassengerRequest updatedPassenger) {
        if (position >= 0 && position < currentPassengers.size()) {
            PassengerRequest oldPassenger = currentPassengers.get(position);

            // Remove old surcharge and add new one
            passengerSurcharges.remove(oldPassenger.name);
            passengerSurcharges.put(updatedPassenger.name, updatedPassenger.surcharge);

            // Update the passenger in the list - make sure to set shareAmount
            double baseShare = currentTrip.getTotalCost() / currentPassengers.size();
            double totalShare = baseShare + updatedPassenger.surcharge;
            updatedPassenger.setShareAmount(totalShare);

            currentPassengers.set(position, updatedPassenger);
            recalculateAndDisplayShares();
            saveSurchargesToPrefs();
        }
    }

    private void removePassenger(int position) {
        if (position >= 0 && position < currentPassengers.size()) {
            PassengerRequest removedPassenger = currentPassengers.get(position);

            // Remove from both list and surcharges map
            passengerSurcharges.remove(removedPassenger.name);
            currentPassengers.remove(position);

            recalculateAndDisplayShares();
            saveSurchargesToPrefs();
        }
    }

    private void recalculateAndDisplayShares() {
        if (currentTrip == null) return;

        int passengerCount = currentPassengers.size();
        if (passengerCount == 0) {
            // Clear the display if no passengers
            passengerAdapter.setPassengers(new ArrayList<>());
            updatePassengerCountDisplay();
            return;
        }

        double baseShare = currentTrip.getTotalCost() / passengerCount;

        // Update all passengers with recalculated shares
        List<PassengerRequest> updatedPassengers = new ArrayList<>();
        for (PassengerRequest passenger : currentPassengers) {
            double individualSurcharge = passengerSurcharges.getOrDefault(passenger.name, 0.0);
            double totalShare = baseShare + individualSurcharge;

            // Create updated passenger with the calculated shareAmount
            PassengerRequest updatedPassenger = new PassengerRequest(passenger.name, individualSurcharge);
            updatedPassenger.setShareAmount(totalShare); // Set the calculated total

            updatedPassengers.add(updatedPassenger);
        }

        currentPassengers.clear();
        currentPassengers.addAll(updatedPassengers);
        passengerAdapter.setPassengers(currentPassengers);
        updatePassengerCountDisplay();
    }

    private void updatePassengerCountDisplay() {
        int passengerCount = currentPassengers.size();
        tvPassengerCount.setText(String.format(Locale.getDefault(),
                "%d Passengers split the fare", passengerCount));
    }

    private void loadTripDetails() {
        int tripId = getIntent().getIntExtra("trip_id", -1);
        if (tripId == -1) {
            Toast.makeText(this, "Invalid trip ID.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(this, "Session expired.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        apiService.getTrip("Bearer " + token, tripId).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    currentTrip = response.body();
                    displayTripDetails(currentTrip);
                } else {
                    Log.e(TAG, "Failed to load trip: " + response.code());
                    Toast.makeText(TripDetailsActivity.this, "Failed to load trip details.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TripDetailsActivity.this, "Network error.", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error fetching trip details", t);
            }
        });
    }

    private void displayTripDetails(TripResponse trip) {
        String routeDate = String.format(Locale.getDefault(), "%s → %s | %s",
                trip.getStart(), trip.getDestination(), trip.getDate());
        tvRouteDate.setText(routeDate);

        tvTotalCost.setText(String.format(Locale.getDefault(), "Total Cost — K%.2f", trip.getTotalCost()));
        initializePassengersFromTrip(trip);
    }

    private void initializePassengersFromTrip(TripResponse trip) {
        currentPassengers.clear();
        passengerSurcharges.clear();

        if (trip.getPassengers() != null && !trip.getPassengers().isEmpty()) {
            // Load saved surcharges from SharedPreferences
            String json = prefs.getString("trip_surcharges_" + trip.getId(), "{}");
            Type type = new TypeToken<Map<String, Double>>(){}.getType();
            Map<String, Double> savedSurcharges = new Gson().fromJson(json, type);
            if (savedSurcharges == null) savedSurcharges = new HashMap<>();

            // Calculate base share (total cost divided by number of passengers)
            double baseShare = trip.getTotalCost() / trip.getPassengers().size();

            for (com.example.busfare_splitterv2.UI.PassengerShare ps : trip.getPassengers()) {
                double individualSurcharge = savedSurcharges.getOrDefault(ps.getName(), 0.0);
                double totalShare = baseShare + individualSurcharge;

                // Create passenger and set the shareAmount
                PassengerRequest passenger = new PassengerRequest(ps.getName(), individualSurcharge);
                passenger.setShareAmount(totalShare); // This is the key line!

                currentPassengers.add(passenger);
                passengerSurcharges.put(ps.getName(), individualSurcharge);
            }

            // Update the adapter with the calculated values
            passengerAdapter.setPassengers(currentPassengers);
            updatePassengerCountDisplay();
        }
    }

    private void saveSurchargesToPrefs() {
        if (currentTrip == null) return;

        SharedPreferences.Editor editor = prefs.edit();
        String json = new Gson().toJson(passengerSurcharges);
        editor.putString("trip_surcharges_" + currentTrip.getId(), json);
        editor.apply();
    }

    private void exportTripAsCSV() {
        if (currentTrip == null || currentPassengers.isEmpty()) {
            Toast.makeText(this, "No passenger data available.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String fileName = "Trip_" + currentTrip.getId() + "_shares.csv";
            File exportDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (exportDir != null && !exportDir.exists()) exportDir.mkdirs();

            File file = new File(exportDir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.append("Passenger Name,Base Share,Surcharge,Total Amount (K)\n");

            int passengerCount = currentPassengers.size();
            double baseShare = passengerCount > 0 ? currentTrip.getTotalCost() / passengerCount : 0.0;

            for (PassengerRequest passenger : currentPassengers) {
                double surcharge = passengerSurcharges.getOrDefault(passenger.name, 0.0);
                double total = baseShare + surcharge;

                writer.append(passenger.name)
                        .append(",")
                        .append(String.format(Locale.getDefault(), "%.2f", baseShare))
                        .append(",")
                        .append(String.format(Locale.getDefault(), "%.2f", surcharge))
                        .append(",")
                        .append(String.format(Locale.getDefault(), "%.2f", total))
                        .append("\n");
            }

            writer.flush();
            writer.close();
            shareCSVFile(file);

        } catch (IOException e) {
            Log.e(TAG, "Error exporting CSV", e);
            Toast.makeText(this, "Error exporting CSV.", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareCSVFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Bus Fare Split - Trip Details");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Passenger shares for trip " +
                currentTrip.getStart() + " → " + currentTrip.getDestination());
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share CSV via"));
    }

    public void backToHome(View v) {
        startActivity(new Intent(this, TripListActivity.class));
    }
}