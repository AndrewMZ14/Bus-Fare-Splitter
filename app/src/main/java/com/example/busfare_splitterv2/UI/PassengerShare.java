package com.example.busfare_splitterv2.UI;

import com.google.gson.annotations.SerializedName;

public class PassengerShare {

    public int surcharge;
    private int id;
    public String name;

    @SerializedName("share_amount")
    private double shareAmount;

    @SerializedName("trip_id")
    private int tripId;

    public PassengerShare() {}

    public int getSurcharge() {
        return surcharge;
    }

    public void setSurcharge(int surcharge) {
        this.surcharge = surcharge;
    }

    public void setTripId(int tripId) {
        this.tripId = tripId;
    }

    public void setId(int id) {
        this.id = id;
    }

    public PassengerShare(int id, String name, double shareAmount, int tripId) {
        this.id = id;
        this.name = name;
        this.shareAmount = shareAmount;
        this.tripId = tripId;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getShareAmount() {
        return shareAmount;
    }

    public int getTripId() {
        return tripId;
    }

    public void setShareAmount(double shareAmount) {
        this.shareAmount = shareAmount;
    }

    public void setName(String name) {
        this.name = name;
    }
}
