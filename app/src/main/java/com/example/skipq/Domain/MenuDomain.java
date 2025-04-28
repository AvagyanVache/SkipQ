package com.example.skipq.Domain;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class MenuDomain implements Parcelable {
    private String itemName;
    private String itemDescription;
    private String itemPrice;
    private String itemImg;
    private int itemCount = 0;
    private int prepTime;
    private RestaurantDomain restaurant;
    private String restaurantId;
    private boolean available;


    public MenuDomain() {}

    protected MenuDomain(Parcel in) {
        itemName = in.readString();
        itemDescription = in.readString();
        itemPrice = in.readString();
        itemImg = in.readString();
        itemCount = in.readInt();
        prepTime = in.readInt();
        restaurant = in.readParcelable(RestaurantDomain.class.getClassLoader());
    }
    public MenuDomain(String itemName, String itemDescription, String itemImg, String itemPrice, int prepTime, int itemCount) {
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.itemImg = itemImg;
        this.itemPrice = itemPrice;
        this.prepTime = prepTime;
        this.itemCount= itemCount;
    }


    public static final Creator<MenuDomain> CREATOR = new Creator<MenuDomain>() {
        @Override
        public MenuDomain createFromParcel(Parcel in) {
            return new MenuDomain(in);
        }

        @Override
        public MenuDomain[] newArray(int size) {
            return new MenuDomain[size];
        }
    };

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }
    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemDescription() {
        return itemDescription;
    }

    public void setItemDescription(String itemDescription) {
        this.itemDescription = itemDescription;
    }

    public String getItemPrice() {
        return itemPrice != null && !itemPrice.trim().isEmpty() ? itemPrice : "0.0";
    }


    public void setItemPrice(String itemPrice) {
        this.itemPrice = (itemPrice != null && !itemPrice.trim().isEmpty()) ? itemPrice : "0.0";
    }
    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
    public String getItemImg() {
        return itemImg;
    }

    public void setItemImg(String itemImg) {
        this.itemImg = itemImg;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getPrepTime() {
        return prepTime;
    }

    public void setPrepTime(int prepTime) {
        this.prepTime = prepTime;
    }

    public RestaurantDomain getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(RestaurantDomain restaurant) {
        if (restaurant == null) {
            this.restaurant = new RestaurantDomain("Unknown Restaurant", "");
        } else {
            this.restaurant = restaurant;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeString(itemName);
        parcel.writeString(itemDescription);
        parcel.writeString(itemPrice);
        parcel.writeString(itemImg);
        parcel.writeInt(itemCount);
        parcel.writeInt(prepTime);
        parcel.writeParcelable(restaurant, i);
    }
}
