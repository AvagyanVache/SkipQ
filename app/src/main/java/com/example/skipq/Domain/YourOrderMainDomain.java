package com.example.skipq.Domain;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;

import java.util.ArrayList;

public class YourOrderMainDomain implements Parcelable {
    private String orderId;
    private RestaurantDomain restaurant;
    private double totalPrice;
    private ArrayList<MenuDomain> items;
    private int totalPrepTime;
    private Timestamp startTime;

    public YourOrderMainDomain() {}

    public YourOrderMainDomain(String orderId, RestaurantDomain restaurant, double totalPrice, int totalPrepTime, ArrayList<MenuDomain> items, Timestamp startTime) {
        this.orderId = orderId;
        this.restaurant = restaurant;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
        this.items = items;
        this.startTime = startTime;
    }

    protected YourOrderMainDomain(Parcel in) {
        orderId = in.readString();
        restaurant = in.readParcelable(RestaurantDomain.class.getClassLoader());
        totalPrice = in.readDouble();
        totalPrepTime = in.readInt();
        items = in.createTypedArrayList(MenuDomain.CREATOR);
        startTime = in.readParcelable(Timestamp.class.getClassLoader());
    }

    public static final Creator<YourOrderMainDomain> CREATOR = new Creator<YourOrderMainDomain>() {
        @Override
        public YourOrderMainDomain createFromParcel(Parcel in) {
            return new YourOrderMainDomain(in);
        }

        @Override
        public YourOrderMainDomain[] newArray(int size) {
            return new YourOrderMainDomain[size];
        }
    };

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public RestaurantDomain getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantDomain restaurant) {
        this.restaurant = restaurant;
    }

    public int getTotalPrepTime() {
        return totalPrepTime;
    }

    public void setTotalPrepTime(int totalPrepTime) {
        this.totalPrepTime = totalPrepTime;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public ArrayList<MenuDomain> getItems() {
        return items;
    }

    public void setItems(ArrayList<MenuDomain> items) {
        this.items = items;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(orderId);
        parcel.writeParcelable(restaurant, i);
        parcel.writeDouble(totalPrice);
        parcel.writeInt(totalPrepTime);
        parcel.writeTypedList(items);
        parcel.writeParcelable(startTime, i);
    }
}
