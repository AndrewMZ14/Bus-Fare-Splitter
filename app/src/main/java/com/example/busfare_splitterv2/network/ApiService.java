package com.example.busfare_splitterv2.network;

import com.example.busfare_splitterv2.UI.Trip;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/register")
    Call<UserResponse> register(@Body UserRequest userRequest);

    @POST("auth/login")
    Call<UserResponse> login(@Body UserLoginRequest request);


    @GET("/trips/")
    Call<List<TripResponse>> getTrips(@Header("Authorization") String token);


    @POST("trips/")  // add the trailing slash
    Call<TripResponse> addTrip(@Header("Authorization") String authToken,
                               @Body TripRequest tripRequest);

    // Get a single trip by ID
    @GET("trips/{trip_id}")
    Call<TripResponse> getTrip(@Header("Authorization") String authToken,
                               @Path("trip_id") int tripId);

    @DELETE("trips/{trip_id}/passengers/{passenger_id}")
    Call<Void> deletePassenger(
            @Header("Authorization") String token,
            @Path("trip_id") int tripId,
            @Path("passenger_id") int passengerId
    );

    // NEW: Delete a trip by ID
    @DELETE("trips/{trip_id}")
    Call<Void> deleteTrip(
            @Header("Authorization") String token,
            @Path("trip_id") int tripId
    );
}

