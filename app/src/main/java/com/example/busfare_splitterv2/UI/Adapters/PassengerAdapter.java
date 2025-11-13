// PassengerAdapter.java
package com.example.busfare_splitterv2.UI.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.network.PassengerRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassengerAdapter extends RecyclerView.Adapter<PassengerAdapter.VH> {

    private List<PassengerRequest> passengers;
    private final OnRemove removeListener;
    private final OnEdit editListener;

    public interface OnRemove {
        void onRemove(int pos, PassengerRequest passenger);
    }

    public interface OnEdit {
        void onEdit(int pos, PassengerRequest passenger);
    }

    public PassengerAdapter(List<PassengerRequest> passengers, OnRemove removeListener, OnEdit editListener) {
        this.passengers = passengers != null ? new ArrayList<>(passengers) : new ArrayList<>();
        this.removeListener = removeListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        PassengerRequest p = passengers.get(position);
        holder.tvName.setText(p.name);

        // Display the correct amount (share or surcharge)
        double displayAmount = p.getDisplayAmount();
        holder.tvShare.setText(String.format(Locale.getDefault(), "K%.2f", displayAmount));

        holder.btnRemove.setOnClickListener(v -> {
            if (removeListener != null)
                removeListener.onRemove(holder.getAdapterPosition(), p);
        });

        holder.itemView.setOnClickListener(v -> {
            if (editListener != null)
                editListener.onEdit(holder.getAdapterPosition(), p);
        });
    }

    @Override
    public int getItemCount() {
        return passengers.size();
    }

    public void setPassengers(List<PassengerRequest> passengers) {
        this.passengers.clear();
        if (passengers != null) {
            this.passengers.addAll(passengers);
        }
        notifyDataSetChanged();
    }

    public void addPassenger(PassengerRequest passenger) {
        this.passengers.add(passenger);
        notifyItemInserted(this.passengers.size() - 1);
    }

    public void removePassenger(int position) {
        if (position >= 0 && position < passengers.size()) {
            this.passengers.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void updatePassenger(int position, PassengerRequest passenger) {
        if (position >= 0 && position < passengers.size()) {
            this.passengers.set(position, passenger);
            notifyItemChanged(position);
        }
    }

    public List<PassengerRequest> getPassengers() {
        return new ArrayList<>(passengers);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName, tvShare;
        ImageButton btnRemove;

        VH(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvShare = itemView.findViewById(R.id.tvShare);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
