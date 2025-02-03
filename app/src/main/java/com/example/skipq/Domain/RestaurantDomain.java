package com.example.skipq.Domain;
public class RestaurantDomain {
    private String name;
    private String imageUrl; // Firebase image URL
    private int image; // Local image resource

    // Constructor for local image and URL
    public RestaurantDomain(String name, int image, String imageUrl) {
        this.name = name;
        this.image = image; // Local image resource ID
        this.imageUrl = imageUrl; // Firebase image URL
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getImage() {
        return image;
    }

    public void setImage(int image) {
        this.image = image;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
