package com.example.skipq.Domain;
public class RestaurantDomain {
    private String name;
    private String imageUrl;
    private int image;

    public RestaurantDomain(String name, int image, String imageUrl) {
        this.name = name;
        this.image = image;
        this.imageUrl = imageUrl;
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
