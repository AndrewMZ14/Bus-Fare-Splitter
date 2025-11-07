package com.example.busfare_splitterv2.UI.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.UI.Trip;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.VH> {

    private List<Trip> trips;
    private final OnTripClick listener;

    public interface OnTripClick {
        void onTripClick(Trip trip);
    }

    public TripAdapter(List<Trip> trips, OnTripClick listener) {
        this.trips = trips != null ? trips : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Trip trip = trips.get(position);
        holder.tvRoute.setText(trip.getStart() + " → " + trip.getDestination());
        holder.tvDate.setText(trip.getDate());
        holder.tvTotal.setText(String.format(Locale.getDefault(), "K%.2f", trip.getTotalCost()));
        holder.tvPassengers.setText(trip.getNumberPassengers() + " Passengers");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onTripClick(trip);
        });
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public void updateTrips(List<Trip> newTrips) {
        this.trips = newTrips != null ? newTrips : new ArrayList<>();
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRoute, tvDate, tvTotal, tvPassengers;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvPassengers = itemView.findViewById(R.id.tvPassengers);
        }
    }
}
