package com.example.skipq.Domain;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;

public class YourOrderMainDomain implements Parcelable {
    private String orderId;
    private String restaurantId;
    private double totalPrice;
    private int totalPrepTime;
    private Timestamp startTime;

    private String userId;
    private String status;
    private String approvalStatus;
    private ArrayList<MenuDomain> items;

    @Exclude
    private RestaurantDomain restaurant;
    private Timestamp endTime;

    public YourOrderMainDomain() {}

    public YourOrderMainDomain(String orderId, String restaurantId, double totalPrice, int totalPrepTime,
                               Timestamp startTime, String userId, String status,String approvalStatus, ArrayList<MenuDomain> items) {
        this.orderId = orderId;
        this.restaurantId = restaurantId;
        this.totalPrice = totalPrice;
        this.totalPrepTime = totalPrepTime;
        this.startTime = startTime;
        this.userId = userId;
        this.status = status;
        this.approvalStatus = approvalStatus;
        this.items = items;
    }



    protected YourOrderMainDomain(Parcel in) {
        orderId = in.readString();
        restaurantId = in.readString();
        totalPrice = in.readDouble();
        totalPrepTime = in.readInt();
        startTime = in.readParcelable(Timestamp.class.getClassLoader());
        userId = in.readString();
        status = in.readString();
        approvalStatus = in.readString();
        items = in.createTypedArrayList(MenuDomain.CREATOR);
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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }


    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }
    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }



    public int getTotalPrepTime() {
        return totalPrepTime;
    }

    public void setTotalPrepTime(int totalPrepTime) {
        this.totalPrepTime = totalPrepTime;
    }

    public Timestamp getStartTime() {
        return startTime;
    }


    public void setStartTime(Object startTime) {
        if (startTime instanceof Timestamp) {
            this.startTime = (Timestamp) startTime;
        } else if (startTime instanceof Long) {
            this.startTime = new Timestamp((Long) startTime, 0);
        }
    }



    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public ArrayList<MenuDomain> getItems() {
        return items;
    }

    public void setItems(ArrayList<MenuDomain> items) {
        this.items = items;
    }

    public RestaurantDomain getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantDomain restaurant) {
        this.restaurant = restaurant;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(orderId);
        parcel.writeString(restaurantId);
        parcel.writeDouble(totalPrice);
        parcel.writeInt(totalPrepTime);
        parcel.writeParcelable(startTime, i);
        parcel.writeString(userId);
        parcel.writeString(status);
        parcel.writeTypedList(items);
    }
}
