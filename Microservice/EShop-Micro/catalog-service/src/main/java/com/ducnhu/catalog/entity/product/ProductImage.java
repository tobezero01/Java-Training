package com.ducnhu.catalog.entity.product;

import jakarta.persistence.*;

@Entity
@Table(name = "product_images")
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public ProductImage() {
    }

    public ProductImage(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public ProductImage(String name, Product product) {
        this.name = name;
        this.product = product;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

//    @Transient
//    public String getImagePath() {
//        // fallback nếu thiếu dữ liệu
//        if (product == null || product.getId() == null || name == null || name.isBlank()) {
//            return "/images/image-thumbnail.png";
//        }
//        // ví dụ: /product-images/1/extras/EOS M50 flash opened.png
//        return "/product-images/" + product.getId() + "/extras/" + name;
//    }

    @Transient
    public String getImagePath() {
        return "/product-images/" + product.getId() + "/extras/" + this.name;
    }
}
