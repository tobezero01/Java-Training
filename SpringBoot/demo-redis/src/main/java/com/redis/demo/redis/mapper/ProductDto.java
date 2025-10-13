package com.redis.demo.redis.mapper;

import java.util.List;
import java.util.Map;

public class ProductDto {
    public Integer id;
    public String name;
    public String alias;
    public String shortDescription;
    public String fullDescription;
    public float price;
    public float discountPercent;
    public float discountPrice;
    public boolean inStock;
    public Integer categoryId;
    public Integer brandId;
    public String mainImage;
    public List<String> images;
    public Map<String, String> details;

    // getters/setters (để Jackson serialize)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getShortDescription() { return shortDescription; }
    public void setShortDescription(String shortDescription) { this.shortDescription = shortDescription; }
    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }
    public float getPrice() { return price; }
    public void setPrice(float price) { this.price = price; }
    public float getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(float discountPercent) { this.discountPercent = discountPercent; }
    public float getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(float discountPrice) { this.discountPrice = discountPrice; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    public Integer getBrandId() { return brandId; }
    public void setBrandId(Integer brandId) { this.brandId = brandId; }
    public String getMainImage() { return mainImage; }
    public void setMainImage(String mainImage) { this.mainImage = mainImage; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Map<String, String> getDetails() { return details; }
    public void setDetails(Map<String, String> details) { this.details = details; }
}
