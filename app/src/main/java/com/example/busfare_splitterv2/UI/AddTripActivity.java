package com.example.busfare_splitterv2.UI;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.UI.Adapters.PassengerAdapter;
import com.example.busfare_splitterv2.network.ApiClient;
import com.example.busfare_splitterv2.network.ApiService;
import com.example.busfare_splitterv2.network.PassengerRequest;
import com.example.busfare_splitterv2.network.TripRequest;
import com.example.busfare_splitterv2.network.TripResponse;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddTripActivity extends AppCompatActivity {

    private final String[] cities = {
            "Lusaka", "Ndola", "Kitwe", "Chingola", "Livingstone",
            "Kabwe", "Chipata", "Mufulira", "Mpika", "Solwezi", "Kasama"
    };

    private AutoCompleteTextView actvStart, actvDestination;
    private RecyclerView rvPassengers;
    private PassengerAdapter passengerAdapter;
    private List<PassengerRequest> passengerList = new ArrayList<>();
    private EditText etDate, etTotalCost;
    private TextView btnAddPassenger;
    private MaterialButton btnCalculate;
    private ApiService apiService;
    private String authToken;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_trip);

        actvStart = findViewById(R.id.actvStart);
        actvDestination = findViewById(R.id.actvDestination);
        etDate = findViewById(R.id.etDate);
        etTotalCost = findViewById(R.id.etTotalCost);
        rvPassengers = findViewById(R.id.rvPassengers);
        btnAddPassenger = findViewById(R.id.tvAddPassenger);
        btnCalculate = findViewById(R.id.btnCalculate);

        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        apiService = ApiClient.getApiService();

        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        authToken = "Bearer " + token;

        // Dropdown setup
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, cities);
        actvStart.setAdapter(cityAdapter);
        actvDestination.setAdapter(cityAdapter);
        actvStart.setOnClickListener(v -> actvStart.showDropDown());
        actvDestination.setOnClickListener(v -> actvDestination.showDropDown());

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(AddTripActivity.this, (view, year, month, dayOfMonth) ->
                    etDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth)),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // RecyclerView setup
        rvPassengers.setLayoutManager(new LinearLayoutManager(this));
        passengerAdapter = new PassengerAdapter(
                passengerList,
                (pos, passenger) -> removePassenger(pos),
                (pos, passenger) -> showEditPassengerDialog(pos, passenger)
        );
        rvPassengers.setAdapter(passengerAdapter);

        // Swipe to delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                removePassenger(viewHolder.getAdapterPosition());
            }
        }).attachToRecyclerView(rvPassengers);

        btnAddPassenger.setOnClickListener(v -> showAddPassengerDialog());
        btnCalculate.setOnClickListener(v -> onCalculate());
    }

    private void removePassenger(int position) {
        if (position >= 0 && position < passengerList.size()) {
            passengerList.remove(position);
            passengerAdapter.setPassengers(passengerList);
            Toast.makeText(this, "Passenger removed", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddPassengerDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View view = inflater.inflate(R.layout.dialog_add_passenger, null);
        EditText etName = view.findViewById(R.id.etPassengerName);
        EditText etSurcharge = view.findViewById(R.id.etPassengerSurcharge);

        new AlertDialog.Builder(this)
                .setTitle("Add Passenger")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String surchargeText = etSurcharge.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Passenger name required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    double surcharge = 0;
                    if (!surchargeText.isEmpty()) {
                        try {
                            surcharge = Double.parseDouble(surchargeText);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid surcharge amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    PassengerRequest newPassenger = new PassengerRequest(name, surcharge);
                    passengerList.add(newPassenger);
                    passengerAdapter.setPassengers(passengerList);

                    rvPassengers.post(() -> rvPassengers.smoothScrollToPosition(passengerList.size() - 1));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditPassengerDialog(int position, PassengerRequest passenger) {
        LayoutInflater inflater = LayoutInflater.from(this);
        android.view.View view = inflater.inflate(R.layout.dialog_add_passenger, null);
        EditText etName = view.findViewById(R.id.etPassengerName);
        EditText etSurcharge = view.findViewById(R.id.etPassengerSurcharge);

        etName.setText(passenger.name);
        etSurcharge.setText(String.valueOf(passenger.surcharge));

        new AlertDialog.Builder(this)
                .setTitle("Edit Passenger")
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = etName.getText().toString().trim();
                    String surchargeText = etSurcharge.getText().toString().trim();

                    double surcharge = 0;
                    if (!surchargeText.isEmpty()) {
                        try {
                            surcharge = Double.parseDouble(surchargeText);
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "Invalid surcharge amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    PassengerRequest updated = new PassengerRequest(name, surcharge);
                    passengerList.set(position, updated);
                    passengerAdapter.setPassengers(passengerList);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onCalculate() {
        String start = actvStart.getText().toString().trim();
        String dest = actvDestination.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String totalS = etTotalCost.getText().toString().trim();

        if (start.isEmpty() || dest.isEmpty() || date.isEmpty() || totalS.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double total = Double.parseDouble(totalS);
        if (passengerList.isEmpty()) {
            Toast.makeText(this, "Add at least one passenger", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate initial shares including surcharges
        int passengerCount = passengerList.size();
        double baseShare = total / passengerCount;

        List<PassengerRequest> passengersWithShares = new ArrayList<>();
        for (PassengerRequest passenger : passengerList) {
            double totalShare = baseShare + passenger.surcharge;
            PassengerRequest passengerWithShare = new PassengerRequest(passenger.name, passenger.surcharge);
            passengerWithShare.setShareAmount(totalShare);
            passengersWithShares.add(passengerWithShare);
        }

        btnCalculate.setText("CREATING TRIP...");
        btnCalculate.setEnabled(false);

        TripRequest request = new TripRequest(start, dest, date, total, passengersWithShares);

        apiService.addTrip(authToken, request).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                btnCalculate.setText("CALCULATE");
                btnCalculate.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    TripResponse tripResponse = response.body();

                    // Save initial surcharges to SharedPreferences
                    saveInitialSurcharges(tripResponse.getId(), passengerList);

                    Intent i = new Intent(AddTripActivity.this, TripDetailsActivity.class);
                    i.putExtra("trip_id", tripResponse.getId());
                    startActivity(i);
                    finish();
                } else {
                    Toast.makeText(AddTripActivity.this, "Trip creation failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                btnCalculate.setText("CALCULATE");
                btnCalculate.setEnabled(true);
                Toast.makeText(AddTripActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveInitialSurcharges(int tripId, List<PassengerRequest> passengers) {
        Map<String, Double> initialSurcharges = new HashMap<>();
        for (PassengerRequest passenger : passengers) {
            initialSurcharges.put(passenger.name, passenger.surcharge);
        }

        SharedPreferences.Editor editor = prefs.edit();
        String userId = prefs.getString("user_id", "default_user");
        String surchargeKey = "trip_surcharges_" + userId + "_" + tripId;
        String json = new Gson().toJson(initialSurcharges);
        editor.putString(surchargeKey, json);
        editor.apply();

        Log.d("AddTripActivity", "Saved initial surcharges for trip " + tripId + " with key: " + surchargeKey);
    }
}