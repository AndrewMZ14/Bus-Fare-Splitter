package com.example.busfare_splitterv2.network;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class TripRequest {
    public String start;
    public String destination;
    public String date;
    @SerializedName("totalCost")
    public double totalCost;
    //public List<PassengerRequest> passengers;

    public TripRequest(String start, String destination, String date, double totalCost, List<PassengerRequest> passengers) {
        this.start = start;
        this.destination = destination;
        this.date = date;
        this.totalCost = totalCost;
        this.passengers = passengers != null ? passengers : new ArrayList<>();
    }

    public List<PassengerRequest> passengers;

    public String getStart() {
        return start;
    }

    public String getDestination() {
        return destination;
    }

    public String getDate() {
        return date;
    }


    public List<PassengerRequest> getPassengers() {
        return passengers;
    }

    public double getTotalCost() {
        return totalCost;
    }
}
