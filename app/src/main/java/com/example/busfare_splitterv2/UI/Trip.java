package com.example.busfare_splitterv2.UI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Trip implements Serializable {
    private int id; // server-side trip ID
    private String start;
    private String destination;
    private String date;
    private double totalCost;
    private List<PassengerShare> passengers = new ArrayList<>();

    // Constructors
    public Trip() {
        // Default constructor
    }

    public Trip(int id, String start, String destination, String date, double totalCost) {
        this.id = id;
        this.start = start;
        this.destination = destination;
        this.date = date;
        this.totalCost = totalCost;
    }

    public Trip(int id, String start, String destination, String date, double totalCost, List<PassengerShare> passengers) {
        this.id = id;
        this.start = start;
        this.destination = destination;
        this.date = date;
        this.totalCost = totalCost;
        this.passengers = passengers;
    }

    // Getters & setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public void setPassengers(List<PassengerShare> passengers) {
        this.passengers = passengers != null ? passengers : new ArrayList<>();
    }

    public int getNumberPassengers() {
        return passengers != null ? passengers.size() : 0;
    }


}
