package com.example.skipq.Domain;

import android.os.Parcel;
import android.os.Parcelable;

public class RestaurantDomain implements Parcelable {
    private String name;
    private String imageUrl;

    public RestaurantDomain(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    protected RestaurantDomain(Parcel in) {
        name = in.readString();
        imageUrl = in.readString();
    }

    public static final Creator<RestaurantDomain> CREATOR = new Creator<RestaurantDomain>() {
        @Override
        public RestaurantDomain createFromParcel(Parcel in) {
            return new RestaurantDomain(in);
        }

        @Override
        public RestaurantDomain[] newArray(int size) {
            return new RestaurantDomain[size];
        }
    };

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(name);
        parcel.writeString(imageUrl);
    }
}
