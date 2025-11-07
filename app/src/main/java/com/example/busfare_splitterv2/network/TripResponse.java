package com.example.busfare_splitterv2.network;

import com.example.busfare_splitterv2.UI.PassengerShare;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TripResponse {

    private int id;
    private String start;
    private String destination;

    public void setId(int id) {
        this.id = id;
    }

    public void setPassengers(List<PassengerShare> passengers) {
        this.passengers = passengers;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setTotalCost(double totalCost) {
        this.totalCost = totalCost;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setStart(String start) {
        this.start = start;
    }

    private String date;

    @SerializedName("total_cost")
    private double totalCost;

    @SerializedName("user_id")
    private int userId;

    private List<PassengerShare> passengers;

    public int getId() {
        return id;
    }

    public String getStart() {
        return start;
    }

    public String getDestination() {
        return destination;
    }

    public String getDate() {
        return date;
    }

    public double getTotalCost() { return totalCost; }

    public int getUserId() {
        return userId;
    }

    public List<PassengerShare> getPassengers() {
        return passengers;
    }

    public String getRoute() {
        return start + " → " + destination;
    }

}
