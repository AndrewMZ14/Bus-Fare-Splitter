package com.example.busfare_splitterv2.UI.Dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.busfare_splitterv2.R;
import com.example.busfare_splitterv2.network.PassengerRequest;
import com.google.android.material.textfield.TextInputEditText;

public class PassengerEditDialog extends Dialog {

    public interface OnPassengerSaved {
        void onSave(PassengerRequest passenger, boolean isEdit, int position);
    }

    private TextInputEditText etName, etSurcharge;
    private Button btnSave, btnCancel;
    private OnPassengerSaved listener;
    private PassengerRequest existingPassenger;
    private boolean isEditMode = false;
    private int editPosition = -1;

    public PassengerEditDialog(Context context, OnPassengerSaved listener) {
        super(context);
        this.listener = listener;
    }

    public void setEditMode(PassengerRequest passenger, int position) {
        this.existingPassenger = passenger;
        this.isEditMode = true;
        this.editPosition = position;
    }

    // In PassengerEditDialog.java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_passenger_edit);

        etName = findViewById(R.id.etPassengerName);
        etSurcharge = findViewById(R.id.etPassengerSurcharge);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        TextView tvDialogTitle = findViewById(R.id.tvDialogTitle); // Add this ID to your layout

        if (isEditMode && existingPassenger != null) {
            tvDialogTitle.setText("Edit Passenger"); // Set title for edit mode
            etName.setText(existingPassenger.name);
            etSurcharge.setText(String.format("%.2f", existingPassenger.surcharge));
        } else {
            tvDialogTitle.setText("Add Passenger"); // Set title for add mode
        }

        btnSave.setOnClickListener(v -> savePassenger());
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void savePassenger() {
        String name = etName.getText().toString().trim();
        String surchargeStr = etSurcharge.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Please enter passenger name", Toast.LENGTH_SHORT).show();
            return;
        }

        double surcharge = 0.0;
        try {
            surcharge = Double.parseDouble(surchargeStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Invalid surcharge amount", Toast.LENGTH_SHORT).show();
            return;
        }

        PassengerRequest passenger = new PassengerRequest(name, surcharge);

        if (listener != null) {
            listener.onSave(passenger, isEditMode, editPosition);
        }

        dismiss();
    }
}