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
import java.util.List;
import java.util.Locale;

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

        // UI references
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

        // City dropdown setup
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, cities);
        actvStart.setAdapter(cityAdapter);
        actvDestination.setAdapter(cityAdapter);
        actvStart.setOnClickListener(v -> actvStart.showDropDown());
        actvDestination.setOnClickListener(v -> actvDestination.showDropDown());

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(AddTripActivity.this, (view, year, month, dayOfMonth) -> {
                etDate.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, dayOfMonth));
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Passenger RecyclerView setup - FIXED: Updated to use 2 parameters
        passengerAdapter = new PassengerAdapter(
                passengerList,
                (pos, passenger) -> {  // FIX: Added second parameter
                    passengerList.remove(pos);
                    passengerAdapter.notifyItemRemoved(pos);
                },
                (pos, passenger) -> showEditPassengerDialog(pos, passenger)  // FIX: Added second parameter
        );

        rvPassengers.setLayoutManager(new LinearLayoutManager(this));
        rvPassengers.setAdapter(passengerAdapter);

        // Swipe to delete passengers
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                passengerList.remove(position);
                passengerAdapter.notifyItemRemoved(position);
                Toast.makeText(AddTripActivity.this, "Passenger removed", Toast.LENGTH_SHORT).show();
            }
        }).attachToRecyclerView(rvPassengers);

        // Button listeners
        btnAddPassenger.setOnClickListener(v -> showAddPassengerDialog());
        btnCalculate.setOnClickListener(v -> onCalculate());
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
                    double surcharge = 0;
                    try { surcharge = Double.parseDouble(etSurcharge.getText().toString().trim()); } catch (Exception ignored) {}
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Passenger name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    passengerList.add(new PassengerRequest(name, surcharge));
                    passengerAdapter.notifyDataSetChanged();
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
                    double surcharge = 0;
                    try { surcharge = Double.parseDouble(etSurcharge.getText().toString().trim()); } catch (Exception ignored) {}
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Passenger name required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    passengerList.set(position, new PassengerRequest(name, surcharge));
                    passengerAdapter.notifyDataSetChanged();
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
            Toast.makeText(this, "Please fill in all trip details", Toast.LENGTH_SHORT).show();
            return;
        }

        double total;
        try { total = Double.parseDouble(totalS); }
        catch (Exception ex) {
            Toast.makeText(this, "Invalid total cost", Toast.LENGTH_SHORT).show();
            return;
        }

        if (passengerList.isEmpty()) {
            Toast.makeText(this, "Add at least one passenger", Toast.LENGTH_SHORT).show();
            return;
        }

        TripRequest request = new TripRequest(start, dest, date, total, passengerList);

        apiService.addTrip(authToken, request).enqueue(new Callback<TripResponse>() {
            @Override
            public void onResponse(Call<TripResponse> call, Response<TripResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TripResponse tripResponse = response.body();
                    Log.i("AddTripActivity", "Trip created: " + tripResponse.getId());

                    // Save surcharges locally
                    List<Double> surcharges = new ArrayList<>();
                    for (PassengerRequest p : passengerList) surcharges.add(p.surcharge);
                    new Gson().toJson(surcharges);
                    prefs.edit().putString("trip_surcharges_" + tripResponse.getId(), new Gson().toJson(surcharges)).apply();

                    Intent i = new Intent(AddTripActivity.this, TripDetailsActivity.class);
                    i.putExtra("trip_id", tripResponse.getId());
                    startActivity(i);
                    finish();
                } else {
                    Log.e("AddTripActivity", "Failed: " + response.code() + " -> " + response.errorBody());
                    Toast.makeText(AddTripActivity.this, "Trip creation failed. Try again.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<TripResponse> call, Throwable t) {
                Log.e("AddTripActivity", "Network error", t);
                Toast.makeText(AddTripActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}