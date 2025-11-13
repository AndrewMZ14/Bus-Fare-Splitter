package com.example.busfare_splitterv2.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.UI.Adapters.TripAdapter;
import com.example.busfare_splitterv2.network.ApiClient;
import com.example.busfare_splitterv2.network.ApiService;
import com.example.busfare_splitterv2.network.PassengerRequest;
import com.example.busfare_splitterv2.network.TripResponse;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TripListActivity extends AppCompatActivity {

    private RecyclerView rvTrips;
    private TripAdapter adapter;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private ApiService apiService;
    private SharedPreferences prefs;
    private List<Trip> trips;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);

        rvTrips = findViewById(R.id.rvTrips);
        trips = new ArrayList<>();
        tvEmpty = findViewById(R.id.tvEmpty);
        progressBar = findViewById(R.id.progressBar);
        imageView = findViewById(R.id.ivProfile);
        FloatingActionButton fab = findViewById(R.id.fabAdd);

        apiService = ApiClient.getApiService();
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        rvTrips.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TripAdapter(trips, trip -> {
            Intent i = new Intent(TripListActivity.this, TripDetailsActivity.class);
            i.putExtra("trip_id", trip.getId());
            startActivity(i);
        });
        rvTrips.setAdapter(adapter);

        // Swipe-to-delete
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                deleteTrip(trips.get(position).getId(), position);
            }
        }).attachToRecyclerView(rvTrips);

        imageView.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        fab.setOnClickListener(v -> startActivity(new Intent(this, AddTripActivity.class)));

        loadTripsFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTripsFromServer();
    }

    private void deleteTrip(int tripId, int position) {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiService.deleteTrip("Bearer " + token, tripId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // Also clear the saved passenger list for this trip
                    clearSavedPassengerList(tripId);

                    trips.remove(position);
                    adapter.notifyItemRemoved(position);
                    tvEmpty.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);
                    Toast.makeText(TripListActivity.this, "Trip deleted", Toast.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(position);
                    Toast.makeText(TripListActivity.this, "Failed to delete trip", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                adapter.notifyItemChanged(position);
                Toast.makeText(TripListActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTripsFromServer() {
        String token = prefs.getString("jwt_token", null);
        if (token == null) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        rvTrips.setVisibility(View.GONE);

        apiService.getTrips("Bearer " + token).enqueue(new Callback<List<TripResponse>>() {
            @Override
            public void onResponse(Call<List<TripResponse>> call, Response<List<TripResponse>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<TripResponse> tripResponses = response.body();
                    List<Trip> tripsList = new ArrayList<>();

                    for (TripResponse tripResponse : tripResponses) {
                        Trip t = new Trip();
                        t.setId(tripResponse.getId());
                        t.setStart(tripResponse.getStart());
                        t.setDestination(tripResponse.getDestination());
                        t.setDate(tripResponse.getDate());
                        t.setTotalCost(tripResponse.getTotalCost());

                        // Store the original passengers
                        t.setPassengers(tripResponse.getPassengers() != null ? tripResponse.getPassengers() : new ArrayList<>());
                        tripsList.add(t);
                    }

                    trips = tripsList;
                    adapter.updateTrips(trips);
                    rvTrips.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(trips.isEmpty() ? View.VISIBLE : View.GONE);

                } else {
                    Toast.makeText(TripListActivity.this, "Failed to load trips", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<TripResponse>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(TripListActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearSavedPassengerList(int tripId) {
        // Clear both passenger list and surcharges when trip is deleted
        String userId = prefs.getString("user_id", "default_user");
        String passengerListKey = "trip_passengers_" + userId + "_" + tripId;
        String surchargeKey = "trip_surcharges_" + userId + "_" + tripId;

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(passengerListKey);
        editor.remove(surchargeKey);
        editor.apply();
    }
}